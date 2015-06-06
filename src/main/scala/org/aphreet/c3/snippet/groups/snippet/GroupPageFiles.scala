package org.aphreet.c3.snippet.groups.snippet

import com.ifunsoftware.c3.access.C3System._
import com.ifunsoftware.c3.access.fs.{ C3Directory, C3File, C3FileSystemNode }
import com.ifunsoftware.c3.access.{ C3System, MetadataRemove, MetadataUpdate, StringMetadataValue }
import net.liftweb.common.{ Full, _ }
import net.liftweb.http.js.JE.{ JsRaw, JsVar }
import net.liftweb.http.js.JsCmds.{ Function, Script }
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.http.js.{ JsCmd, JsCmds }
import net.liftweb.http._
import net.liftweb.sitemap.Loc.{ Hidden, Link, LinkText }
import net.liftweb.sitemap.{ Loc, Menu, SiteMap }
import net.liftweb.util.Helpers._
import net.liftweb.util.{ Helpers, CssSel, PassThru }
import org.aphreet.c3.acl.groups.{ GroupsAccess, UserStatusGroup }
import org.aphreet.c3.comet.{ JournalServer, JournalServerEvent, MessageServerFactory }
import org.aphreet.c3.lib.DependencyFactory
import org.aphreet.c3.lib.metadata.Metadata
import org.aphreet.c3.lib.metadata.Metadata._
import org.aphreet.c3.loc.SuffixLoc
import org.aphreet.c3.model.{ Group, User }
import org.aphreet.c3.service.groups.GroupService
import org.aphreet.c3.service.journal.EventType
import org.aphreet.c3.snippet.LiftMessages
import org.aphreet.c3.snippet.groups.{ AbstractGroupPageLoc, GroupPageFilesData }
import org.aphreet.c3.snippet.groups.snippet.tags.TagForms
import org.aphreet.c3.util.C3Exception
import org.aphreet.c3.util.helpers._

import scala.xml.{ NodeSeq, Text }

/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 * @author Koyushev Sergey (mailto: serjk91@gmail.com)
 *         iFunSoftware
 */
object GroupPageFiles extends AbstractGroupPageLoc[GroupPageFilesData] with SuffixLoc[Group, GroupPageFilesData] {

  override val name = "Files"
  override val pathPrefix = "groups" :: Nil
  override val pathSuffix = "files" :: Nil

  override def getItem(id: String) = Group.findById(id)

  override def isAccessiblePage(page: GroupPageFilesData): Boolean = {
    if (!page.isDirectoryLoc) {
      true
    } else {
      super.isAccessiblePage(page)
    }
  }

  // we don't use it here
  override def wrapItem(groupBox: Box[Group]): Box[GroupPageFilesData] = Empty

  override def link = {
    new Link[GroupPageFilesData](pathPrefix ++ pathSuffix) {
      override def pathList(value: GroupPageFilesData): List[String] = pathPrefix ::: value.group.getId :: Nil ::: pathSuffix ::: value.path
    }
  }

  override def finishPath(itemBox: => Box[Group],
                          restPath: List[String],
                          suffix: String = ""): Box[GroupPageFilesData] = {

    val resultPath = if (suffix.isEmpty) restPath.diff(pathSuffix) else restPath.diff(pathSuffix).init ::: List(restPath.diff(pathSuffix).last + "." + suffix)
    if (restPath.startsWith(pathSuffix)) wrapItemWithPath(itemBox, resultPath) else Empty
  }

  def wrapItemWithPath(groupBox: Box[Group], path: List[String]) = groupBox.map(GroupPageFilesData(_, path))
}

class GroupPageFiles(data: GroupPageFilesData) extends C3ResourceHelpers
    with GroupPageHelper with FSHelper with TagForms with C3AccessHelpers {

  import org.aphreet.c3.lib.DependencyFactory._

  override lazy val activeLocId = "files"
  override lazy val group = data.group
  lazy val path = data.path
  val pathLocs = buildPathLocs
  val groupFilesLink = group.createLink
  val file = group.getFile(data.currentAddress)
  val currentResource = file.openOrThrowException("Directory or file is not exist.")
  val aclFormId = "acl"
  val defaultValueCheckbox = false
  private val logger = Logger(classOf[GroupPageFiles])
  private val c3 = inject[C3System].openOrThrowException("C3 is not available")
  private val journalServer: Box[JournalServer] = Full(MessageServerFactory(group))
  var currentResourceName = ""
  var currentAclValue = ""

  def parentNodeLink: String = pathLocs.reverse match {
    case Nil => groupFilesLink
    case xs  => xs.tail.headOption.fold(groupFilesLink)((l: Loc[_]) => l.createDefaultLink.get.text)
  }

  def render = {
    def renameCurrentNode(newName: String): JsCmd = {
      file.foreach {
        f =>
          val newPath = (f.fullname.split("/").toList.init ::: newName :: Nil).mkString("", "/", "")
          f.move(newPath)
          journalServer.foreach(_ ! JournalServerEvent(User.currentUserUnsafe, group, EventType.UpdateResources, newPath))
      }

      val redirectPath: String = file.map {
        f =>
          groupFilesLink + "/" + (path.init ::: newName :: Nil).mkString("", "/", if (f.isDirectory) "/" else "")
      }.openOr("")

      JsCmds.RedirectTo(redirectPath)
    }
    "#right-box-head *" #> group.name.is &
      ".base_files_path *" #> (
        ".link [href]" #> groupFilesLink &
        ".link *" #> group.name.is) &
        ".bcrumb *" #> pathLocs.map {
          (loc: Loc[_]) =>
            (if (isLocCurrent(pathLocs, loc) && (hasSuperAccessResource(currentResource) || hasWriteAccessResource(currentResource))) {
              ".link" #>
                <span class="hide name_submit_func">
                  {
                    Script(
                      Function("renameNodeCallback", List("name"),
                        SHtml.ajaxCall(
                          JsVar("name"),
                          (name: String) => renameCurrentNode(name))._2.cmd))
                  }
                </span>
                <a href="#" id="node_name" data-type="text" data-pk="2" data-placeholder="Name..." data-original-title="Rename" class="editable editable-click">
                  { loc.title }
                </a>
            } else {
              ".link [href]" #> loc.createDefaultLink &
                ".link *" #> loc.title
            }) &
              ".divider" #> (
                data.isDirectoryLoc match {
                  case false if loc == pathLocs.last => (_: NodeSeq) => NodeSeq.Empty // if it is a file then we want to skip last "/" divider
                  case _                             => PassThru
                })
        } &
        ".current_path *" #> Text(data.currentAddress) &
        ".edit_access *" #> acl() &
        ".submit_acl [onclick]" #> SHtml.ajaxInvoke(() => updateAclValue()) &
        (file match {
          case Empty => S.redirectTo("/404.html"); "* *" #> PassThru
          case Failure(msg, t, chain) => {
            logger.error("Error accessing file: " + msg, t)
            S.redirectTo("/404.html")
            "* *" #> PassThru
          }
          case Full(f) => {
            //Type matching is not gonna work since in local c3 accesslib implementation all nodes are both files and directories
            if (!f.isDirectory) {
              renderFileLoc(f.asFile)
            } else {
              if (!S.uri.endsWith("/"))
                S.redirectTo(S.uri + "/")
              else
                renderDirectoryLoc(f.asDirectory)
            }
          }
          case _ => "* *" #> PassThru
        })
  }

  def removeMeta(f: C3FileSystemNode, key: String, value: String): JsCmd = {
    try {
      f.update(MetadataRemove(List(key)))
      keys.set(keys.get - key)
      JsCmds.Replace(key + value, NodeSeq.Empty)
    } catch {
      case e: Exception => JsCmds.Alert("Failed to remove metadata")
    }
  }

  def editMeta(f: C3FileSystemNode, key: String, value: String): JsCmd = {
    try {

      if (Some(value) != f.metadata.get(key)) {
        val metadata = Map(key -> StringMetadataValue(value))
        f.update(MetadataUpdate(metadata))
      }

    } catch {
      case e: Exception => JsCmds.Alert("Failed to update metadata")
    }
  }

  //update value acl in storage
  def updateAclValue(): JsCmd = {
    val metadata = Map(ACL_META -> currentAclValue)
    group.getChildren(data.currentAddress).map(res => {
      if (res.fullname.hashCode.toString == currentResourceName) {
        res.update(MetadataUpdate(metadata))
        journalServer.foreach(_ ! JournalServerEvent(User.currentUserUnsafe, group, EventType.UpdateResources, res.fullname))
      }
    })
    JsRaw("$('#" + aclFormId + "').modal('hide')").cmd &
      JsCmds.SetHtml(currentResourceName, Text(currentAclValue))
  }

  //current edit resource
  def currentResource(nameResource: String, acl: String) = {
    currentResourceName = nameResource
    currentAclValue = acl
    JsCmds.Noop
  }

  def updateRightBox(file: C3FileSystemNode): JsCmd = {
    JsCmds.SetHtml("right-box-head", <span>
                                       { file.name }
                                     </span>) &
      JsCmds.SetHtml("description_box", <span>
                                          { ConvertHelper.ShortString(file.metadata.get(DESCRIPTION_META).getOrElse("")) }
                                        </span>) &
      JsCmds.SetHtml("edit_tags_form", <span>
                                         { file.metadata.get(TAGS_META).map(_.split(",").mkString(", ")).getOrElse("") }
                                       </span>) &
      (if (!file.isDirectory) {
        JsCmds.Replace("download_btn", <a type="button" href={ fileDownloadUrl(file.asFile) } id="download_btn" class="btn btn-inverse btn-block download_btn">
                                         <i class="icon-white glyphicon glyphicon-download-alt"></i>
                                         <span>Скачать</span>
                                       </a>) &
          JsCmds.Replace("view_btn", <a type="button" href={ fileViewUrl(file.asFile) } id="view_btn" class="btn btn-inverse btn-block view_btn">
                                       <i class="icon-white icon-eye-open"></i>
                                       <span>Просмотреть</span>
                                     </a>) &
          JsCmds.Replace("replace_file_btn", <div></div>)
      } else {
        JsCmds.Replace("download_btn", <a type="button" id="download_btn" visible="false"></a>) &
          JsCmds.Replace("view_btn", <a type="button" id="view_btn" visible="false"></a>) &
          JsCmds.Replace("replace_file_btn", <div></div>)
      })
  }

  def toCss(directory: C3Directory) = {

    val owner = nodeOwner(directory)
    val metaACL = acl(directory.metadata.get(ACL_META).getOrElse(""))
    def redirectToDirectory: CssSel = {
      ".link [href]" #> (directory.name + "/") &
        ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(directory.name + "/")) &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => updateRightBox(directory))
    }

    def transferDirectory: CssSel = {
      ".acl_cont [ondrag]" #> SHtml.ajaxInvoke(() => FileTransferHelper.saveDraggableResourceName(directory.name)) &
        ".acl_cont [ondrop]" #> SHtml.ajaxInvoke(() => FileTransferHelper.moveSelectedResource(group, data.currentAddress, directory.name, false))
    }

    def accessRestricted: CssSel = {
      ".link [href]" #> "#" &
        ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => LiftMessages.ajaxError(S.?("access.restricted")))
    }
    (if (hasSuperAccessResource(directory)) {
      ".rules *" #> metaACL &
        ".rules [id]" #> directory.fullname.hashCode &
        ".rules [ondblclick]" #> SHtml.ajaxInvoke(() => currentResource(directory.fullname.hashCode.toString, metaACL)) &
        transferDirectory &
        ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(directory.name + "/")) &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => updateRightBox(directory))
      ".link [href]" #> (directory.name + "/")
    } else {
      val haveReadRight = checkReadAccessResource(directory)
      val haveWriteRight = hasWriteAccessResource(directory)
      (if (haveReadRight) {

        val groupAccess = new GroupsAccess {}
        User.currentUser match {
          case Full(u) => groupAccess.checkAccess(u, group) match {
            case UserStatusGroup.Admin | UserStatusGroup.Owner | UserStatusGroup.Member | UserStatusGroup.Other =>
              redirectToDirectory
            case UserStatusGroup.Request =>
              if (haveReadRight)
                redirectToDirectory
              else
                accessRestricted
          }
          case Empty =>
            redirectToDirectory
        }
      } else {
        accessRestricted
      }) &
        (if (haveWriteRight) {
          transferDirectory
        } else {
          ".acl_cont [ondrag]" #> ""
          ".acl_cont [ondrop]" #> ""
        }) &
        ".acl_cont *" #> metaACL
    }) &
      ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> directory.name &
      ".icon [src]" #> {
        if (directory.name == group.trashCanName) "/images/trash.png" else "/images/folder_classic.png"
      } &
      ".description_box *" #> directory.metadata.get(DESCRIPTION_META).getOrElse("") &
      ".created_date *" #> internetDateFormatter.format(directory.date) &
      "#right-box-head *" #> directory.name
  }

  def toCss(file: C3File) = {

    def transferFile: CssSel = {
      ".acl_cont [ondrag]" #> SHtml.ajaxInvoke(() => FileTransferHelper.saveDraggableResourceName(file.name)) &
        ".acl_cont [ondrop]" #> ""
    }

    val owner = nodeOwner(file)
    val metaACL = acl(file.metadata.get(ACL_META).getOrElse(""))

    (if (hasSuperAccessResource(file)) {
      ".rules *" #> metaACL &
        ".rules [id]" #> file.fullname.hashCode &
        ".rules [ondblclick]" #> SHtml.ajaxInvoke(() => currentResource(file.fullname.hashCode.toString, metaACL)) &
        transferFile &
        ".link [href]" #> file.name &
        ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(file.name)) &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => updateRightBox(file))
    } else {
      (if (checkReadAccessResource(file)) {
        ".link [href]" #> (file.name + "/") &
          ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(file.name)) &
          ".child_td [onclick]" #> SHtml.ajaxInvoke(() => updateRightBox(file))
      } else {
        ".link [href]" #> "#" &
          ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => LiftMessages.ajaxError(S.?("access.restricted")))
      }) &
        (if (hasWriteAccessResource(file)) {
          transferFile
        } else {
          ".acl_cont [ondrag]" #> ""
          ".acl_cont [ondrop]" #> ""
        }) &
        ".acl_cont *" #> metaACL
    }) &
      ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> ConvertHelper.ShortString(file.name, 40) &
      ".description_box *" #> ConvertHelper.ShortString(file.metadata.get(DESCRIPTION_META).getOrElse(""), if (file.name.length > 40) 60 else (110 - file.name.length)) &
      ".icon [src]" #> (file.metadata.get(CONTENT_TYPE) match {
        case Some("application/vnd.ms-excel")     => "/images/excel_type.png"
        case Some("image/png")                    => "/images/png_type.png"
        case Some("image/gif")                    => "/images/gif_type.png"
        case Some("application/pdf")              => "/images/pdf_type.png"
        case Some("application/msword")           => "/images/word_type.png"
        case Some("application/zip")              => "/images/zip_type.png"
        case Some("application/x-rar-compressed") => "/images/rar_type.png"
        // case Some(s) => if s.contains("text/plain") "/images/document_letter.png"
        case _                                    => "/images/unkhown_type.png"
      }) &
      ".created_date *" #> internetDateFormatter.format(file.date) &
      "#right-box-head *" #> file.name
    //40 - max vizible symbols, when size file name is big
    //110 - count visible symbols in description columns
  }

  def acl(): CssSel = {
    def groupReadSave(b: Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res => {
        if (res.fullname.hashCode.toString == currentResourceName) {
          val aclVal = if (b) "r---" else "----"
          currentAclValue = aclVal
        }
      })
      JsCmds.Noop
    }

    def groupWriteSave(b: Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res => {
        if (res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b) {
            val newACL = metaACL.toCharArray
            newACL.update(1, 'w')
            newACL.update(0, 'r')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          } else {
            val newAcl = metaACL.replace('w', '-')
            currentAclValue = newAcl
          }
        }
      })
      JsCmds.Noop
    }

    def otherUsersReadSave(b: Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res => {
        if (res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b) {
            val newACL = metaACL.toCharArray
            newACL.update(2, 'r')
            newACL.update(0, 'r')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          } else {
            val newACL = metaACL.toCharArray
            newACL.update(3, '-')
            newACL.update(2, '-')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
        }
      })
      JsCmds.Noop
    }

    def otherUsersWriteSave(b: Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res => {
        if (res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b)
            currentAclValue = "rwrw"
          else {
            val newACL = metaACL.toCharArray
            newACL.update(3, '-')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
        }
      })
      JsCmds.Noop
    }

    ".group_read" #> SHtml.ajaxCheckbox(defaultValueCheckbox, groupReadSave) &
      ".group_write" #> SHtml.ajaxCheckbox(defaultValueCheckbox, groupWriteSave) &
      ".all_read" #> SHtml.ajaxCheckbox(defaultValueCheckbox, otherUsersReadSave) &
      ".all_write" #> SHtml.ajaxCheckbox(defaultValueCheckbox, otherUsersWriteSave)
  }

  def buildPathLocs: List[Loc[_]] = {
    val locs: List[Loc[_]] = transformToPathLists(data.path).map {
      thisPath =>

        new Loc[List[String]] {
          private val __sitemap = SiteMap.build(Array(Menu(this)))

          override def name: String = thisPath.lastOption.getOrElse("N/A")

          override def link: Loc.Link[List[String]] = new Link[List[String]](List("groups", data.group.getId, "files") ::: thisPath)

          override def text: Loc.LinkText[List[String]] = new LinkText[List[String]](v => Text(v.lastOption.getOrElse("N/A")))

          override def defaultValue: Box[List[String]] = Full(thisPath)

          override def params = Hidden :: Nil

          override def siteMap: SiteMap = __sitemap

          override def defaultRequestValue: Box[List[String]] = Full(thisPath)
        }
    }

    locs
  }

  def isLocCurrent(allLocs: List[Loc[_]], loc: Loc[_]) = allLocs.lastOption.map(_ == loc).getOrElse(false)

  protected def updateDescription(node: C3FileSystemNode, descr: String): JsCmd = {
    node.update(MetadataUpdate(Map(DESCRIPTION_META -> descr)))
    JsCmds.Noop // bootstrap-editable will update text value on page by itself
  }

  protected def updateTags(node: C3FileSystemNode, tags: String): JsCmd = {
    val metadata = Map(TAGS_META -> tags.split(",").map(_.trim()).mkString(","))
    node.update(MetadataUpdate(metadata))
    JsCmds.Noop // bootstrap-editable will update text value on page by itself
  }

  protected def commonForms(node: C3FileSystemNode): CssSel = {
    val meta = node.metadata
    val metadataUser = Metadata.filterSystemKey(meta)

    "#edit_tags_form *" #> meta.get(TAGS_META).map(_.split(",").mkString(", ")).getOrElse("") &
      ".description_box *" #> meta.get(DESCRIPTION_META).getOrElse("") &
      (if (meta.get(HASH).getOrElse("") == "") {
        "#txtHash [value]" #> "" &
          "#sharing [class+]" #> "disp_none"
      } else
        "#txtHash [value]" #> FileSharingHelper.fileShareFullUrl(node.asFile)) &
      "#txtHash [value]" #> (if (meta.get(HASH).getOrElse("") == "") "" else FileSharingHelper.fileShareFullUrl(node.asFile)) &
      (if (hasWriteAccessResource(node) || hasSuperAccessResource(node)) {
        ".edit_tags_form_func *" #> {
          Script(
            Function("updateTagsCallback", List("tags"),
              SHtml.ajaxCall(
                JsVar("tags"),
                (d: String) => updateTags(node, d))._2.cmd))
        } &
          "#meta" #> metadataEdit(node, metadataUser) &
          ".description_submit_func *" #> {
            Script(
              Function("updateDescriptionCallback", List("description"),
                SHtml.ajaxCall(
                  JsVar("description"),
                  (d: String) => updateDescription(node, d))._2.cmd))
          } &
          ".delete_file_btn [onclick]" #> SHtml.ajaxInvoke(() => {
            {
              moveFileToTrashCan(node.fullname, true);
              JsCmds.RedirectTo(parentNodeLink)
            }
          }) &
          ".share_btn [onclick]" #> SHtml.ajaxInvoke(() => FileSharingHelper.shareFile(node)) &
          ".remove_public_link [onclick]" #> SHtml.ajaxInvoke(() => FileSharingHelper.disableSharing(node))
      } else {
        "#edit_tags_form [data-disabled]" #> "true" &
          ".remove_resource" #> NodeSeq.Empty &
          ".description_box [data-disabled]" #> "true" &
          "#meta" #> metadataView(node, metadataUser) &
          ".share_btn [disabled]" #> "true" &
          ".remove_public_link [disabled]" #> "true" &
          "#sharing *" #> NodeSeq.Empty &
          ".replace_file_btn" #> NodeSeq.Empty

      }) &
      ".file_tags" #> meta.get(TAGS_META).map(_.split(TAGS_SEPARATOR).toList).getOrElse(Nil).map((tag: String) => {
        ".label *" #> tag
      })
  }

  protected def renderDirectoryLoc(d: C3Directory): CssSel = {
    object selectedResourcePaths extends RequestVar[Set[String]](Set())
    val currentAddress = data.currentAddress
    val currentPathLink = data.group.createLink + currentAddress
    def writeTools(): CssSel = {
      "#file_upload_form [action]" #> ("/upload/file/groups/" + group.getId + "/files" + data.currentAddress) &
        "#file_upload_close_btn [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.Reload) &
        "#new-directory" #> newDirectoryForm(d, currentPath = currentPathLink)
    }
    def superAccessTools(): CssSel = {
      ".delete_selected_btn [onclick]" #> SHtml.ajaxInvoke(() => {
        selectedResourcePaths.foreach(moveFileToTrashCan(_, false))
        JsCmds.RedirectTo(currentPathLink)
      })
    }
    val parentFolderPath = currentPathLink.substring(0, currentPathLink.dropRight(1).lastIndexOf("/"))

    val parentResourcePath =
      if (currentAddress != "/")
        currentAddress.substring(0, currentAddress.dropRight(1).lastIndexOf("/")) + "/"
      else
        ""
    val parentResource = group.getFile(parentResourcePath).openOr(null)
    (if (hasSuperAccess) {
      if (hasWriteAccess(group)) {
        superAccessTools()
        writeTools()
      } else {
        superAccessTools()
      }
    } else if (hasWriteAccess(group)) {
      ".delete_selected_form_button" #> NodeSeq.Empty &
        ".delete_selected_form" #> NodeSeq.Empty &
        writeTools()
    } else {
      ".delete_selected_form_button" #> NodeSeq.Empty &
        ".delete_selected_form" #> NodeSeq.Empty &
        "#new-directory" #> NodeSeq.Empty &
        ".new_directory_btn" #> NodeSeq.Empty &
        ".upload_files_btn" #> NodeSeq.Empty &
        "#file_upload_form" #> NodeSeq.Empty
    }) &
      tagsForm(d) &
      ".parent_link [href]" #> (parentFolderPath + "/") &
      ".parentfolder_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(parentFolderPath + "/")) &
      (if ((parentResource != null && hasWriteAccessResource(parentResource)) || (parentResourcePath == "/" && hasWriteAccess(group))) {
        ".parentfolder [ondrop]" #> SHtml.ajaxInvoke(() => FileTransferHelper.moveSelectedResource(group, data.currentAddress, parentFolderPath + "/", true))
      } else {
        ".parentfolder [ondrop]" #> ""
      }) &
      (if (parentResourcePath.isEmpty) ".parentfolder *" #> NodeSeq.Empty
      else "invalid-empty-tag" #> NodeSeq.Empty) &
      ".child *" #> group.getChildren(data.currentAddress).sortBy(!_.isDirectory).map {
        resource =>
          {
            (resource.isDirectory match {
              case true => toCss(resource.asDirectory)
              case _    => toCss(resource.asFile)
            }) &
              ".select_resource" #> SHtml.ajaxCheckbox(value = false, (value: Boolean) => {
                if (value)
                  selectedResourcePaths.set(selectedResourcePaths.get + resource.fullname)
                else
                  selectedResourcePaths.set(selectedResourcePaths.get - resource.fullname)
                JsCmds.Noop
              })
          }
      } &
      ".file-view" #> NodeSeq.Empty &
      commonForms(d)
  }

  def moveFileToTrashCan(name: String, IsDeletedFromFilePage: Boolean): JsCmd = {
    if (!name.contains(group.trashCanDirectory) && !name.endsWith("/" + group.trashCanName)) {
      if (group.getFile(group.trashCanDirectory).openOr(null) == null) createTrashCan();
      val resourceName = (name.substring(name.dropRight(1).lastIndexOf("/"), name.length));
      FileTransferHelper.moveToTrashCan(resourceName, group, data.currentAddress, IsDeletedFromFilePage)
    } else {
      JsCmds.Alert("Can't delete trashcan or file in it.")
    }
  }

  def createTrashCan(): Unit = {
    val root = c3.getFile("/").asDirectory
    root.getChild(group.getId) match {
      case Some(node) =>
        val dir = node.asDirectory
        dir.getChild("files") match {
          case Some(node) =>
            val files = node.asDirectory
            val metadata = Map(OWNER_ID_META -> dir.metadata.get(OWNER_ID_META).getOrElse(User.id.is.toString),
              GROUP_ID_META -> data.group.getId,
              TAGS_META -> "Trash",
              DESCRIPTION_META -> "",
              ACL_META -> dir.metadata.get(ACL_META).getOrElse(""))
            files.createDirectory(group.trashCanName, metadata)
          case None => throw new C3Exception("Failed to create trash can for group " + group.getId)
        }
      case None => throw new C3Exception("Can't find group with id " + group.getId)
    }
  }

  protected def renderFileLoc(f: C3File): CssSel = {
    val owner = nodeOwner(f)
    def doRenderFileLoc(hasAccess: Boolean): CssSel = {
      if (!hasAccess) S.redirectTo("/401.html")
      ".file-table" #> NodeSeq.Empty &
        ".fs_toolbar" #> NodeSeq.Empty &
        "#upload_form" #> NodeSeq.Empty &
        "#directory_tags" #> NodeSeq.Empty &
        ".name_file *" #> f.name &
        (
          ".view_btn [href]" #> fileViewUrl(f) &
          ".download_btn [href]" #> fileDownloadUrl(f)) &
          ".data_file *" #> internetDateFormatter.format(f.date) &
          ".owner_file *" #> owner.map(_.shortName).getOrElse("Unknown") &
          ".size_file *" #> ByteCalculatorHelper.convert(f.versions.lastOption.fold("None")(_.length.toString)) &
          commonForms(f) &
          "#file_replace_form [action]" #> ("/replace/file/groups/" + group.getId + "/files" + data.currentAddress) &
          "#file_replace_close_btn [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.Reload)

    }
    if (hasSuperAccess || checkReadAccessResource(f)) doRenderFileLoc(true)
    else ".child_td [ondblclick]" #> SHtml.ajaxInvoke(() => (LiftMessages.ajaxError(S.?("access.restricted")))) &
      doRenderFileLoc(false)
  }

  protected def metadataEdit(f: C3FileSystemNode, metadataUsr: scala.collection.Map[String, String]) = {
    ".metadata_form" #> combineUserMetadata(metadataUsr).map {
      case (k, v) => {
        keys.set(keys.get + k)
        ".metadata_form [id]" #> (k + v) &
          ".metadata_key [value]" #> S.?(k) &
          ".metadata_value" #> SHtml.ajaxText(v, editMeta(f, k, _)) &
          ".remove_metadata [onClick]" #> SHtml.ajaxInvoke(() => removeMeta(f, k, v))
      }
    } &
      ".add_metadata" #> addMetadata(f)
  }

  protected def metadataView(f: C3FileSystemNode, metadataUsr: scala.collection.Map[String, String]) = {
    ".metadata_form *" #> combineUserMetadata(metadataUsr).map {
      case (k, v) => {
        ".metadata_key [value]" #> k &
          ".metadata_value [value]" #> v &
          ".remove_metadata" #> NodeSeq.Empty &
          ".metadata_value [readonly+]" #> "readonly"
      }
    } &
      ".add_metadata" #> NodeSeq.Empty &
      ".btn_save_metadata" #> NodeSeq.Empty &
      "#edit_metadata_form" #> NodeSeq.Empty
  }

  protected def addMetadata(f: C3FileSystemNode): CssSel = {
    var key = ""
    var value = ""

    def addMetadataInst(key: String, value: String): JsCmd = {
      if (!keys.contains(key)) {
        keys.set(keys.get + key)
        val metadata = Map(key -> value)
        f.update(MetadataUpdate(metadata))
        val idMetadataContainer = "metadata_container"
        JqJsCmds.AppendHtml(idMetadataContainer,
          <tr class="metadata_form" id={ key + value }>
            <td>
              <input class="metadata_key" value={ key } readonly="readonly"/>
            </td>
            <td>
              <input type="text" class="metadata_value" value={ value }/>
            </td>
            <td>
              <button class="close remove_metadata" onclick={ SHtml.ajaxInvoke(() => removeMeta(f, key, value))._2.toJsCmd }>
                &times;
              </button>
            </td>
          </tr>)
      } else
        JsCmds.Noop
    }

    ".add_metadata" #> {
      (xml: NodeSeq) =>
        SHtml.ajaxForm(
          ("name=key" #> SHtml.onSubmit(key = _) &
            "name=value" #> SHtml.onSubmit(value = _) &
            "type=submit" #> ((xml: NodeSeq) =>
              xml ++ SHtml.hidden {
                () =>
                  if (key == "" || value == "")
                    JsCmds.Noop
                  else
                    addMetadataInst(key, value)
              })).apply(xml))
    }
  }

  protected def newDirectoryForm(currentDirectory: C3Directory, currentPath: String): CssSel = {
    var name = ""
    var tags = ""
    var description = ""

    def createDirectory() {
      if (name.trim.isEmpty) {
        S.error("Directory name cannot be empty")
      } else {
        val metadata = Map(OWNER_ID_META -> User.currentUserUnsafe.id.is.toString,
          GROUP_ID_META -> data.group.getId,
          TAGS_META -> tags.trim,
          DESCRIPTION_META -> description.trim,
          ACL_META -> currentDirectory.metadata.get(ACL_META).getOrElse(""),
          ORIGINAL_NAME_META -> name)
        val normalizedName = normilizeFileName(name)
        currentDirectory.createDirectory(normalizedName.trim, metadata)
        journalServer.foreach(_ ! JournalServerEvent(User.currentUserUnsafe, group, EventType.CreateResources, currentDirectory.fullname + normalizedName.trim))
        S.redirectTo(currentPath) // redirect on the same page
      }
    }

    "name=name" #> SHtml.onSubmit(name = _) &
      "name=description" #> SHtml.onSubmit(description = _) &
      "name=tags" #> SHtml.onSubmit(tags = _) &
      "type=submit" #> SHtml.onSubmitUnit(createDirectory)
  }

  private def normilizeFileName(name: String): String = {
    name.replace(':', '-').replace('/', '-').replace('\\', '-')
  }

  private def combineUserMetadata(metadata: scala.collection.Map[String, String]): Map[String, String] = {

    val combinedMeta: Map[String, Iterable[MetadataElement]] = metadata.map(e => MetadataElement(e._1.replaceFirst("^doc:", ""), e._2, e._1.startsWith("doc:"))).groupBy(_.key)

    val filteredMeta = combinedMeta.values.map(list => if (list.forall(_.value == list.head.value)) List(list.toList.sortBy(_.extracted).head) else list)
      .flatten.map(e => (if (e.extracted) "doc:" + e.key else e.key, e.value)).toMap

    filteredMeta
  }

  case class MetadataElement(key: String, value: String, extracted: Boolean)

  //list keys for metadata
  object keys extends SessionVar[Set[String]](Set())

}
