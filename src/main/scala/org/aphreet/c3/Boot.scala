package org.aphreet.c3

import net.liftweb.util._
import net.liftweb.http.provider._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper._
import net.liftweb.http._
import net.liftweb.http.js.jquery.JQueryArtifacts
import net.liftmodules.widgets.logchanger._
import net.liftmodules.widgets.uploadprogress._
import net.liftmodules.widgets.tablesorter.TableSorter
import net.liftmodules.widgets.autocomplete.AutoComplete
import net.liftmodules.widgets.menu.MenuWidget
import net.liftweb.util.Props
import net.liftweb.http.Html5Properties
import net.liftweb.http.InMemoryResponse
import net.liftweb.common.Full
import net.liftweb.http.ParsePath
import net.liftweb.http.NotFoundAsTemplate
import net.liftweb.sitemap.Loc.LocGroup
import net.liftweb.http.ServiceUnavailableResponse
import net.liftweb.sitemap.Loc.If

import javax.mail.{ Authenticator, PasswordAuthentication }

import util.helpers.C3Streamer
import util.{ DefaultAuthDataLoader, TextileRenderer }
import model._

import snippet.approve.ApproveSection
import snippet.categories.CategoriesSection
import snippet.groups.GroupsSection
import snippet.logging.LogLevel
import snippet.notifications.NotificationsSection
import snippet.users.UsersSection

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Bootable {
  private val sections: List[Section] =
    List(BaseSection, UsersSection, GroupsSection, CategoriesSection, NotificationsSection, ApproveSection)

  import Boot._

  def boot() {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = new StandardDBVendor(dbDriver, dbUrl, dbUserOpt, dbPassOpt)
      LiftRules.unloadHooks.append(vendor.closeAllConnections_!)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    LiftRules.resourceNames = "i18n/lift-core" :: LiftRules.resourceNames

    LiftRules.jsArtifacts = JQueryArtifacts

    // where to search snippets
    sections.foreach(s => LiftRules.addToPackages(s.currentPackage))

    DBSetup.setup()

    lazy val loginUrl = "/user_mgt/login"

    // stateful redirect after login
    def loginAndComeBack: RedirectWithState = {
      val uri = S.uriAndQueryString
      RedirectWithState(
        loginUrl, RedirectState(() => User.loginRedirect.set(uri), "Not logged in" -> NoticeType.Notice))
    }

    val loggedIn = If(() => User.loggedIn_?, loginAndComeBack)

    /**
     * LocParam check that current user is a super admin
     * Because of If(..) cannot be easily composed there should be loggedIn check performed before
     */
    val isSuperAdmin = If(
      () => User.currentUser.map(_.superUser.is) openOr false,
      () => RedirectWithState("/index", RedirectState(() => {}, "Not a super user" -> NoticeType.Notice)))

    // Build SiteMap
    def sitemap() = SiteMap(

      Menu("index") / "index",

      Menu("About") / "about" >> LocGroup("footerMenu"),

      Menu("Faq") / "faq" >> LocGroup("footerMenu"),

      Menu("Groups") / "groups" >> LocGroup("mainmenu") submenus {
        GroupsSection.menus: _*
      },
      Menu("users", "Users") / "users" >> loggedIn >> LocGroup("mainmenu") submenus {
        UsersSection.menus: _*
      },
      Menu("admin", "Admin") / "admin" >> LocGroup("admin_menus") >> isSuperAdmin submenus {
        List(Menu("categories", "Categories") / "admin" / "categories" submenus {
          CategoriesSection.menus: _*
        },
          Menu("group_admin", "Approve group") / "admin" / "group_admin" submenus {
            ApproveSection.menus: _*
          })
      },
      Menu("notifications", "Notifications") / "notifications" >> loggedIn submenus {
        NotificationsSection.menus: _*
      },
      Menu("Experiments") / "experiments" >> LocGroup("mainmenu"),

      Menu(Loc("virtualization", ExtLink(plabAddress), "Virtualization", LocGroup("mainmenu"))),

      Menu("R service") / "r_suite" >> LocGroup("mainmenu"),

      LogLevel.menu, // default log level menu is located at /loglevel/change

      Menu("UserEdit") / "users" / "edituser" >> loggedIn >> Hidden)

    LiftRules.setSiteMapFunc(() => User.sitemapMutator(sitemap()))

    // Custom 404 page
    LiftRules.uriNotFound.prepend(NamedPF("404handler") {
      case (req, failure) =>
        NotFoundAsTemplate(ParsePath(List("404"), "html", absolute = false, endSlash = false))
    })

    LiftRules.dispatch.append {
      case Req("download" :: groupname :: filePath, extension, GetRequest) =>
        C3Streamer(groupname, filePath, extension)
    }
    /************************ FILE UPLOAD *******************************/
    // In cases where we have an AJAX request for IE with an uploaded file, we
    // assume we served through an iframe (a fairly safe assumption) and serve
    // up the response with a content type of text/plain so that IE does not
    // attempt to save the response as a downloaded file.
    LiftRules.responseTransformers.append {
      resp =>
        (for (req <- S.request) yield {
          resp match {
            case InMemoryResponse(data, headers, cookies, code) if !req.uploadedFiles.isEmpty &&
              req.isIE &&
              req.path.wholePath.head == LiftRules.ajaxPath =>
              val contentlessHeaders = headers.filterNot(_._1.toLowerCase == "content-type")
              InMemoryResponse(data, ("Content-Type", "text/plain; charset=utf-8") :: contentlessHeaders, cookies, code)
            case _ => resp
          }
        }) openOr resp
    }
    /********************************************************************/

    // for ajax file upload
    import org.aphreet.c3.lib.FileUpload
    LiftRules.dispatch.append(FileUpload)

    /*
     * Make the spinny image go away when it ends
     */
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // 1 hour timeout for long requests
    LiftRules.ajaxPostTimeout = 3600000

    LiftRules.early.append(makeUtf8)

    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Log Changer widget inittialization is required for setting setup
    // default location for log changer is {webapproot}/loglevel/change
    LogLevelChanger.init()

    // Initialization for upload progress widget
    UploadProgress.init

    //Make sure we don't put stuff in memory for uploads
    LiftRules.handleMimeFile = OnDiskFileParamHolder.apply

    // Initialization for auto complete widget
    AutoComplete.init()

    // Initilization for table sorter widget
    TableSorter.init()

    // TODO do we use it?
    MenuWidget.init()

    // Check and create default users if necessary
    DefaultAuthDataLoader.init()

    // Table sorter widget init
    TableSorter.init()

    //Init auto complete input
    AutoComplete.init()

    LiftRules.statelessDispatch.append(TextileRenderer)

    // for ajax file upload
    LiftRules.progressListener = {
      val opl = LiftRules.progressListener
      val ret: (Long, Long, Int) => Unit =
        (a, b, c) => {
          opl(a, b, c)
        }
      ret
    }

    //Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Ignore requests to /dav/*
    LiftRules.liftRequest.append({
      case r if r.path.partPath.headOption.exists(_ == "dav") => false
    })

    configMailer(mailHost, mailUser, mailPass)

    FileUpload.init()

    LiftRules.statelessDispatch.prepend {
      case _ if DB.currentConnection.isEmpty => () => Full(ServiceUnavailableResponse(10))
    }

    S.addAround(DB.buildLoanWrapper())
  }

  /**
   * Force the request to be UTF-8
   */
  private def makeUtf8(req: HTTPRequest) {
    req.setCharacterEncoding("UTF-8")
  }

  private def configMailer(host: String, user: String, password: String) {
    // Enable TLS support
    System.setProperty("mail.smtp.starttls.enable", "true")
    // Set the host name
    System.setProperty("mail.smtp.host", host)
    // Enable authentication
    System.setProperty("mail.smtp.auth", "true")

    // Provide a means for authentication. Pass it a Box, which can either be Full or Empty
    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(user, password)
    })
  }
}

object Boot {
  private val defaultMailHost = "smtp.gmail.com"
  private val defaultMailUser = "c3-project@ifunsoftware.com"
  private val defaultMailPassword = "myverysecretpassword"
  private val defaultPlabUrl = "https://194.85.162.171/"

  val plabAddress = Props.get("plab.address", defaultPlabUrl)

  val mailHost = Props.get("mail.host", defaultMailHost)
  val mailUser = Props.get("mail.user").openOr(defaultMailUser)
  val mailPass = Props.get("mail.password").openOr(defaultMailPassword)

  val dbDriver = Props.get("db.driver", "org.h2.Driver")
  val dbUrl = Props.get("db.url", "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE")
  val dbUserOpt = Props.get("db.user")
  val dbPassOpt = Props.get("db.password")
}
