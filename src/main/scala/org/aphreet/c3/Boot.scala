package org.aphreet.c3

import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http.provider._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import apiaccess.C3
import Helpers._
import org.aphreet.c3.model._
import net.liftweb.mapper._
import net.liftweb.http._
import js.jquery.JQuery14Artifacts
import net.liftweb.widgets.logchanger._
import net.liftweb.widgets.uploadprogress._
import net.liftweb.widgets.tablesorter.TableSorter
import net.liftweb.widgets.autocomplete.AutoComplete
import net.liftweb.widgets.menu.MenuWidget
import servlet.HTTPServletContext
import snippet.categories.CategoriesSection
import snippet.group.GroupSection
import snippet.logging.LogLevel
import snippet.search.SearchSection
import snippet.user.UserSection
import util.helpers.C3Streamer
import util.{DefaultAuthDataLoader, TextileRenderer}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Bootable{

  private val sections: List[Section] = List(BaseSection, UserSection, GroupSection, SearchSection, CategoriesSection)

  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr
            "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    LiftRules.resourceNames = "i18n/lift-core" :: LiftRules.resourceNames

    LiftRules.jsArtifacts = JQuery14Artifacts

    // where to search snippets
    sections.foreach(s => LiftRules.addToPackages(s.currentPackage))

    Schemifier.schemify(true, Schemifier.infoF _, User, Group, Category, Tag, UserGroup)

    lazy val loginUrl = "/user_mgt/login"

    // stateful redirect after login
    def loginAndComeBack = {
      val uri = S.uriAndQueryString
      RedirectWithState ( loginUrl, RedirectState( () => User.loginRedirect.set(uri) , "Not logged in" -> NoticeType.Notice ) )
    }

    val loggedIn = If(() => User.loggedIn_?, loginAndComeBack _ )

    val isSuperAdmin = If(() => {if(!User.currentUser.isEmpty) User.currentUser.open_!.superUser.is else false},
      () => RedirectWithState("/index", RedirectState( () => {} ,"Not a super user" -> NoticeType.Notice ) )
    )

    val isGroupAdmin = If(() => {
      (for {
        groupName <- S.param("groupname")
        group     <- Group.find(By(Group.name,groupName))
        user      <- User.currentUser
        if user.id.is == group.owner.is
      } yield true).openOr(false)
    }, () => RedirectWithState("/index", RedirectState( () => {} ,"Not a group admin" -> NoticeType.Notice )))

    // Build SiteMap
    def sitemap() = SiteMap(

      Menu("Home") / "index" >> LocGroup("mainmenu"),

      Menu("About") / "about" >> LocGroup("footerMenu"),

      Menu("Faq") / "faq" >> LocGroup("footerMenu"),

      Menu("Groups") / "groups" >> loggedIn >> LocGroup("mainmenu") submenus {
        GroupSection.menus:_*
      },

      Menu("Users") / "users" >> loggedIn >> LocGroup("mainmenu") submenus {
        UserSection.menus:_*
      },

      Menu("Categories") / "categories" >> loggedIn >> LocGroup("mainmenu") submenus {
        CategoriesSection.menus:_*
      },

      LogLevel.menu, // default log level menu is located at /loglevel/change

      Menu("UserEdit") / "users" / "edituser" >> loggedIn >> Hidden,

      Menu("Search") / "search" >> loggedIn >> Hidden
    )

    LiftRules.setSiteMapFunc(() => User.sitemapMutator(sitemap()))

    // Custom 404 page
    LiftRules.uriNotFound.prepend(NamedPF("404handler"){
      case (req,failure) =>
        NotFoundAsTemplate(ParsePath(List("404"),"html", false, false))
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
            case InMemoryResponse(data, headers, cookies, code)
              if ! req.uploadedFiles.isEmpty &&
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
    AutoComplete.init

    // Initilization for table sorter widget
    TableSorter.init

    MenuWidget.init()

    DefaultAuthDataLoader.init

    LiftRules.statelessDispatchTable.append(TextileRenderer)

    // for ajax file upload
    LiftRules.progressListener = {
      val opl = LiftRules.progressListener
      val ret: (Long, Long, Int) => Unit =
        (a, b, c) => {
          opl(a,b,c)
        }
      ret
    }


    //Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    LiftRules.liftRequest.append({
      case r if (r.path.partPath match {
        case "dav" :: _ => true
        case _ => false
      }) => false
    })

    S.addAround(DB.buildLoanWrapper)

//    if(!Props.productionMode){
//      Category.findAll().foreach(_.delete_!)
//        (1 to 10).foreach{ i: Int => {
//        val cat = Category.create.name("Category" + i).saveMe()
//        (1 to 5).map(i => Tag.create.name("Tag" + i).category(cat).saveMe())
//      }}
//    }

  }

  /**
   * Force the request to be UTF-8
   */
  private def makeUtf8(req: HTTPRequest) {
    req.setCharacterEncoding("UTF-8")
  }
}
