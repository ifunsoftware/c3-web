package org.aphreet.c3.snippet.groups.snippet

import org.aphreet.c3.loc.SuffixLoc
import org.aphreet.c3.model.{User, Group}
import net.liftweb.common._
import net.liftweb.sitemap.Loc.{Hidden, LinkText, Link}
import tags.TagForms
import xml.{NodeSeq, Text}
import net.liftweb.util.Helpers._
import net.liftweb.http.{RequestVar, SHtml, S}
import org.aphreet.c3.snippet.groups.AbstractGroupPageLoc
import com.ifunsoftware.c3.access.fs.{C3FileSystemNode, C3File, C3Directory}
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._
import net.liftweb.util.{CssSel, PassThru}
import net.liftweb.sitemap.{Menu, SiteMap, Loc}
import net.liftweb.http.js.{JsCmds, JsCmd}
import org.aphreet.c3.lib.DependencyFactory
import com.ifunsoftware.c3.access.C3System
import com.ifunsoftware.c3.access.C3System._
import net.liftweb.http.js.JsCmds.{Function, Script}
import org.aphreet.c3.util.helpers.{ConvertHelpers, ByteCalculatorHelpers}
import com.ifunsoftware.c3.access.MetadataUpdate
import net.liftweb.common.Full
import com.ifunsoftware.c3.access.MetadataRemove
import org.aphreet.c3.snippet.groups.GroupPageFilesData
import net.liftweb.http.js.JE.{JsVar, JsRaw}
import org.aphreet.c3.snippet.LiftMessages

/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 * @author Koyushev Sergey (mailto: serjk91@gmail.com)
 *  iFunSoftware
 */
object GroupPageFiles extends AbstractGroupPageLoc[GroupPageFilesData] with SuffixLoc[Group, GroupPageFilesData]{

  override val name = "Files"
  override val pathPrefix = "groups" :: Nil
  override val pathSuffix = "files" ::  Nil
  override def getItem(id: String) = Group.find(id)

  override def isAccessiblePage(page: GroupPageFilesData): Boolean = {
    if (!page.isDirectoryLoc) {
      true
    } else {
      super.isAccessiblePage(page)
    }
  }

  // we don't use it here
  override def wrapItem(groupBox: Box[Group]): Box[GroupPageFilesData] = Empty

  def wrapItemWithPath(groupBox: Box[Group], path: List[String]) = groupBox.map(GroupPageFilesData(_, path))

  override def link = {
    new Link[GroupPageFilesData](pathPrefix ++ pathSuffix){
      override def pathList(value: GroupPageFilesData): List[String] = pathPrefix ::: value.group.id.is.toString :: Nil ::: pathSuffix ::: value.path
    }
  }

  override def finishPath(itemBox: => Box[Group],
                          restPath: List[String],
                          suffix: String = ""): Box[GroupPageFilesData] = {

    val resultPath = if(suffix.isEmpty) restPath.diff(pathSuffix) else restPath.diff(pathSuffix).init ::: List(restPath.diff(pathSuffix).last + "." + suffix)
    if (restPath.startsWith(pathSuffix)) wrapItemWithPath(itemBox, resultPath) else Empty
  }
}

class GroupPageFiles(data: GroupPageFilesData) extends C3ResourceHelpers
with GroupPageHelpers with FSHelpers with TagForms with C3FileAccessHelpers{

  import DependencyFactory._

  private val logger = Logger(classOf[GroupPageFiles])
  private val c3 = inject[C3System].openOrThrowException("C3 is not available")

  override lazy val activeLocId = "files"
  override lazy val group = data.group
  lazy val path = data.path

  val pathLocs = buildPathLocs
  val groupFilesLink = group.createLink + "/files/"
  val file = group.getFile(data.currentAddress)
  val currentResource = file.openOrThrowException("Directory or file is not exist.")

  def parentNodeLink: String = {
    (pathLocs.reverse match {
      case Nil => groupFilesLink
      case xs => xs.tail.headOption.map((l: Loc[_]) => l.createDefaultLink.get.text).getOrElse(groupFilesLink)
    })
  }

  def render = {
    def renameCurrentNode(newName: String): JsCmd = {
      file.foreach{ f =>
        val newPath = (f.fullname.split("/").toList.init ::: newName :: Nil).mkString("", "/", "")
        f.move(newPath)
      }

      val redirectPath: String = file.map { f =>
        groupFilesLink + (path.init ::: newName :: Nil).mkString("", "/", if (f.isDirectory) "/" else "")
      }.openOr("")

      JsCmds.RedirectTo(redirectPath)
    }

    ".base_files_path *" #> (
      ".link [href]" #> groupFilesLink &
      ".link *" #> group.name.is
    ) &
    ".bcrumb *" #> (pathLocs.map{ (loc: Loc[_]) =>
      ( if (isLocCurrent(pathLocs, loc) && (hasSuperAccess(currentResource)||hasWriteAccess(currentResource))){
        ".link" #>
          <span class="hide name_submit_func">
            {
              Script(
                Function("renameNodeCallback", List("name"),
                  SHtml.ajaxCall(
                    JsVar("name"),
                    (name: String) => renameCurrentNode(name)
                  )._2.cmd
                )
              )
            }
          </span>
          <a href="#" id="node_name" data-type="text" data-pk="2"
                 data-placeholder="Name..." data-original-title="Rename"
                 class="editable editable-click">
                {loc.title}
          </a>
        } else {
          ".link [href]" #> loc.createDefaultLink &
            ".link *" #> loc.title
      }) &
        ".divider" #> (
            data.isDirectoryLoc match {
              case false if loc == pathLocs.last => (_:NodeSeq) => NodeSeq.Empty // if it is a file then we want to skip last "/" divider
              case _ => PassThru
            }
        )
    }) &
    ".current_path *" #> Text(data.currentAddress) &
    ".edit_access *" #> acl()&
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
          if (!f.isDirectory){
            renderFileLoc(f.asFile)
          } else {
            if(!S.uri.endsWith("/"))
              S.redirectTo(S.uri + "/")
            else
              renderDirectoryLoc(f.asDirectory)
          }
        }
        case _ => "* *" #> PassThru
      })
  }

  protected def updateDescription(node: C3FileSystemNode, descr: String): JsCmd = {
    node.update(MetadataUpdate(Map(DESCRIPTION_META -> descr)))
    JsCmds.Noop // bootstrap-editable will update text value on page by itself
  }

  protected def updateTags(node: C3FileSystemNode, tags: String): JsCmd = {
    val metadata = Map((TAGS_META -> tags.split(",").map(_.trim()).mkString(",")))
    node.update(MetadataUpdate(metadata))
    JsCmds.Noop // bootstrap-editable will update text value on page by itself
  }

  protected def commonForms(node: C3FileSystemNode): CssSel = {
    val meta = node.metadata
    val metadataUser = Metadata.filterSystemKey(meta)

    "#edit_tags_form *" #> meta.get(TAGS_META).map(_.split(",").mkString(", ")).getOrElse("") &
    ".description_box *" #> meta.get(DESCRIPTION_META).getOrElse("") &
    (if(hasWriteAccess(node) || hasSuperAccess(node)){
      ".edit_tags_form_func *" #> {
        Script(
          Function("updateTagsCallback", List("tags"),
            SHtml.ajaxCall(
              JsVar("tags"),
              (d: String) => updateTags(node, d)
            )._2.cmd
          )
        )
      } &
      "#meta" #> metadataEdit(node, metadataUser) &
      ".description_submit_func *" #> {
          Script(
            Function("updateDescriptionCallback", List("description"),
              SHtml.ajaxCall(
                JsVar("description"),
                (d: String) => updateDescription(node, d)
              )._2.cmd
            )
          )
      } &
      ".delete_file_btn [onclick]" #> SHtml.ajaxInvoke(() => { c3.deleteFile(node.fullname); JsCmds.RedirectTo(parentNodeLink) })
    } else {
      "#edit_tags_form [data-disabled]" #> "true" &
      ".remove_resource" #> NodeSeq.Empty &
      ".description_box [data-disabled]" #> "true" &
      "#meta" #> metadataView(node, metadataUser)
    }) &
    ".file_tags" #> meta.get(TAGS_META).map(_.split(TAGS_SEPARATOR).toList).getOrElse(Nil).map((tag: String) => {
      ".label *" #> tag
    })
  }

  protected def renderDirectoryLoc(d: C3Directory): CssSel = {
    object selectedResourcePaths extends RequestVar[Set[String]](Set())

    val currentPathLink = data.group.createLink + "/files" + data.currentAddress
    (if(hasSuperAccess(d)){
      ".delete_selected_btn [onclick]" #> SHtml.ajaxInvoke(() =>
      { selectedResourcePaths.foreach(c3.deleteFile _); JsCmds.RedirectTo(currentPathLink) })
    } else {
      ".delete_selected_form_button" #> NodeSeq.Empty &
      ".delete_selected_form" #> NodeSeq.Empty
    }) &
    tagsForm(d) &
    ".child *" #> group.getChildren(data.currentAddress).map {
      resource => {
        (resource.isDirectory match {
          case true => toCss(resource.asDirectory)
          case _ => toCss(resource.asFile)
        }) &
        ".select_resource" #> SHtml.ajaxCheckbox(false, (value: Boolean) => {
          if(value)
            selectedResourcePaths.set(selectedResourcePaths.get + resource.fullname)
          else
            selectedResourcePaths.set(selectedResourcePaths.get - resource.fullname)
            JsCmds.Noop
          })
      }
    } &
    ".delete_selected_btn [onclick]" #> SHtml.ajaxInvoke(() =>
      { selectedResourcePaths.foreach(c3.deleteFile _); JsCmds.RedirectTo(currentPathLink) }) &
    ".file-view" #> NodeSeq.Empty &
    "#file_upload_form [action]" #> ("/upload/file/groups/" + group.id.is + "/files" + data.currentAddress) &
    "#file_upload_close_btn [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.Reload) &
    "#new-directory" #> newDirectoryForm(d, currentPath = currentPathLink) &
    commonForms(d)
  }

  protected def renderFileLoc(f: C3File): CssSel = {
      val owner = nodeOwner(f)

      ".file-table" #> NodeSeq.Empty &
      ".fs_toolbar" #> NodeSeq.Empty &
      "#upload_form" #> NodeSeq.Empty &
      "#directory_tags" #> NodeSeq.Empty &
      ".name_file *" #> f.name &
      ".download_btn [href]" #> fileDownloadUrl(f) &
      ".view_btn [href]" #> fileViewUrl(f) &
      ".data_file *" #> internetDateFormatter.format(f.date)&
      ".owner_file *" #>  owner.map(_.shortName).getOrElse("Unknown") &
      ".size_file *" #> ByteCalculatorHelpers.convert(f.versions.lastOption.map(_.length.toString).getOrElse("None")) &
      commonForms(f)
  }

  protected def metadataEdit(f: C3FileSystemNode, metadataUsr:scala.collection.Map[String,String]) = {
    ".metadata_form" #> metadataUsr.map{ case (k, v) => {
      def removeMeta():JsCmd = {
        try{
          f.update(MetadataRemove(List(k)))
          JsCmds.Replace((k+v),NodeSeq.Empty)
        }catch{
          case e:Exception => JsCmds.Alert("User is not removed! Please check logs for details")
        }
      }
      ".metadata_form [id]" #> (k + v)&
        ".metadata_form *" #>
          ((n: NodeSeq) => SHtml.ajaxForm(
            (".metadata_key [value]" #> k &
              ".metadata_value [value]" #> v &
              ".remove_metadata *" #> SHtml.memoize(t => t ++ SHtml.hidden(removeMeta _))).apply(n)
          ))
    }}&
    "#meta_edit" #> saveMetadata(f)
  }

  protected def metadataView(f: C3FileSystemNode, metadataUsr:scala.collection.Map[String,String]) = {
    ".metadata_form *" #> metadataUsr.map{ case (k, v) => {
      ".metadata_key [value]" #> k &
        ".metadata_value [value]" #> v &
        ".remove_metadata"#> NodeSeq.Empty &
        ".metadata_value [readonly+]" #> "readonly"
    }}&
      ".add_metadata" #> NodeSeq.Empty &
      ".btn_save_metadata" #> NodeSeq.Empty &
      "#edit_metadata_form" #> NodeSeq.Empty
  }

  protected def saveMetadata(f: C3FileSystemNode): CssSel ={
    var key = ""
    var value = ""
    def save (){
      val  keyMetadata= key.split("%")
      val  valueMetadata= value.split("%")
      val metadata = keyMetadata.zip(valueMetadata).toMap
      f.update(MetadataUpdate(metadata))
    }
    "name=metadata_key" #> SHtml.onSubmit(key = _) &
      "name=metadata_value" #> SHtml.onSubmit(value = _) &
      "type=submit" #> SHtml.onSubmitUnit(save)
  }

  protected def newDirectoryForm(currentDirectory: C3Directory, currentPath: String): CssSel = {
    var name = ""
    var tags = ""

    def createDirectory(){
      if (name.trim.isEmpty){
        S.error("Directory name cannot be empty")
      } else {
        val metadata = Map((OWNER_ID_META -> User.currentUserUnsafe.id.is.toString),
          (GROUP_ID_META -> data.group.id.is.toString),
          (TAGS_META -> tags.trim))
        currentDirectory.createDirectory(name.trim, metadata)
        S.redirectTo(currentPath) // redirect on the same page
      }
    }

    "name=name" #> SHtml.onSubmit(name = _) &
      "name=tags" #> SHtml.onSubmit(tags = _) &
      "type=submit" #> SHtml.onSubmitUnit(createDirectory)
  }

  var currentResourceName = ""
  var currentAclValue = ""
  val aclFormId = "acl"

  //update value acl in storage
  def updateAclValue():JsCmd = {
    val metadata = Map(ACL_META -> currentAclValue)
    group.getChildren(data.currentAddress).map(res=>{
      if(res.fullname.hashCode.toString == currentResourceName)
        res.update(MetadataUpdate(metadata))
    })
    JsRaw("$('#"+aclFormId+"').modal('hide')").cmd &
      JsCmds.SetHtml(currentResourceName,Text(currentAclValue))
  }
  //current edit resource
  def currentResource(nameResource:String,acl:String) = {
    currentResourceName = nameResource
    currentAclValue = acl
    JsCmds.Noop
  }

  def toCss(directory: C3Directory) = {

    val owner = nodeOwner(directory)
    val metaACL = acl(directory.metadata.get(ACL_META).getOrElse(""))
    (if(hasSuperAccess(directory)){
      ".rules *" #> metaACL  &
        ".rules [id]" #> directory.fullname.hashCode &
        ".rules [onclick]" #> SHtml.ajaxInvoke(()=>currentResource(directory.fullname.hashCode.toString,metaACL))&
        ".link [href]" #> (directory.name + "/") &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(directory.name + "/"))
    }else{
      (if(checkReadAccess(directory)){
        ".link [href]" #> (directory.name + "/") &
          ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(directory.name + "/"))
      }else {
        ".link [href]" #> "#"&
          ".child_td [onclick]" #> SHtml.ajaxInvoke(() => (LiftMessages.ajaxError(S.?("access.restricted"))))
      })&
        ".acl_cont *" #> metaACL
    })&
      ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> directory.name &
      ".icon [src]" #> "/images/folder_classic.png" &
      ".description_box *" #> directory.metadata.get(DESCRIPTION_META).getOrElse("")&
      ".created_date *" #> internetDateFormatter.format(directory.date)

  }

  def toCss(file: C3File) = {
    val owner = nodeOwner(file)
    val metaACL = acl(file.metadata.get(ACL_META).getOrElse(""))

    (if(hasSuperAccess(file)){
      ".rules *" #> metaACL  &
        ".rules [id]" #> file.fullname.hashCode &
        ".rules [onclick]" #> SHtml.ajaxInvoke(()=>currentResource(file.fullname.hashCode.toString,metaACL))&
        ".link [href]" #> file.name &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(file.name))
    } else {
      (if(checkReadAccess(file)){
        ".link [href]" #> (file.name + "/") &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(file.name))
      } else {
        ".link [href]" #> "#" &
        ".child_td [onclick]" #> SHtml.ajaxInvoke(() => (LiftMessages.ajaxError(S.?("access.restricted"))))
      }) &
        ".acl_cont *" #> metaACL
    }) &
      ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> ConvertHelpers.ShortString(file.name,40) &
      ".description_box *" #> ConvertHelpers.ShortString(file.metadata.get(DESCRIPTION_META).getOrElse(""),if(file.name.length > 40) 60 else (110-file.name.length))&
      ".icon [src]" #> "/images/document_letter.png" &
      ".created_date *" #> internetDateFormatter.format(file.date)
       //40 - max vizible symbols, when size file name is big
       //110 - count visible symbols in description columns
  }

  val defaultValueCheckbox = false

  def acl(): CssSel = {
    def groupReadSave(b:Boolean):JsCmd = {
      group.getChildren(data.currentAddress).map(res=>{
        if(res.fullname.hashCode.toString == currentResourceName) {
          val aclVal =  if(b)"r---" else "----"
          currentAclValue = aclVal
        }

      })

      JsCmds.Noop
    }

    def groupWriteSave(b:Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res=>{
        if(res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b) {
            val newACL = metaACL.toCharArray
            newACL.update(1,'w')
            newACL.update(0,'r')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
          else{
            val newAcl = metaACL.replace('w','-')
            currentAclValue = newAcl
          }
        }
      })
      JsCmds.Noop
    }

    def otherUsersReadSave(b:Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res=>{
        if(res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b) {
            val newACL = metaACL.toCharArray
            newACL.update(2,'r')
            newACL.update(0,'r')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
          else{
            val newACL = metaACL.toCharArray
            newACL.update(3,'-')
            newACL.update(2,'-')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
        }
      })
      JsCmds.Noop
    }

    def otherUsersWriteSave(b:Boolean): JsCmd = {
      group.getChildren(data.currentAddress).map(res=>{
        if(res.fullname.hashCode.toString == currentResourceName) {
          val metaACL = acl(res.metadata.get(ACL_META).getOrElse(""))
          if (b)
            currentAclValue = "rwrw"
          else{
            val newACL = metaACL.toCharArray
            newACL.update(3,'-')
            var aclVal = ""
            newACL.map(aclVal += _)
            currentAclValue = aclVal
          }
        }
      })
      JsCmds.Noop
    }

    ".group_read" #> SHtml.ajaxCheckbox(defaultValueCheckbox,groupReadSave(_))&
    ".group_write" #> SHtml.ajaxCheckbox(defaultValueCheckbox,groupWriteSave(_))&
    ".all_read" #> SHtml.ajaxCheckbox(defaultValueCheckbox,otherUsersReadSave(_))&
    ".all_write" #> SHtml.ajaxCheckbox(defaultValueCheckbox,otherUsersWriteSave(_))
  }

  def buildPathLocs: List[Loc[_]] = {
    val locs: List[Loc[_]] = (transformToPathLists(data.path)).map { thisPath =>

      new Loc[List[String]] {
        private val __sitemap = SiteMap.build(Array(Menu(this)))

        override def name: String = thisPath.lastOption.getOrElse("N/A")

        override def link: Loc.Link[List[String]] = new Link[List[String]](List("groups", data.group.id.is.toString, "files") ::: thisPath)

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

}