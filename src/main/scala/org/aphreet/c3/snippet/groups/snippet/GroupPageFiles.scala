package org.aphreet.c3.snippet.groups.snippet

import org.aphreet.c3.loc.{SuffixLoc, ItemRewriteLoc}
import org.aphreet.c3.model.{User, Group}
import net.liftweb.common._
import net.liftweb.sitemap.Loc.{Hidden, LinkText, Link}
import tags.TagForms
import xml.{NodeSeq, Text}
import net.liftweb.util.Helpers._
import net.liftweb.http.{RequestVar, SHtml, S}
import net.liftweb.common.Full
import org.aphreet.c3.snippet.groups.{AbstractGroupPageLoc, GroupPageFilesData}
import com.ifunsoftware.c3.access.fs.{C3FileSystemNode, C3File, C3Directory}
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._
import net.liftweb.util.{CssSel, PassThru}
import net.liftweb.sitemap.{Menu, SiteMap, Loc}
import annotation.tailrec
import net.liftweb.mapper.By
import net.liftweb.http.js.{JsCmds, JsCmd}
import org.aphreet.c3.lib.DependencyFactory
import com.ifunsoftware.c3.access.{MetadataRemove, MetadataUpdate, C3System}
import com.ifunsoftware.c3.access.C3System._
import net.liftweb.http.js.JsCmds.{Alert, OnLoad}
import net.liftweb.http.js.JE.JsRaw
import org.aphreet.c3.util.helpers.ByteCalculatorHelpers


/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 *         iFunSoftware
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

class GroupPageFiles(data: GroupPageFilesData) extends C3ResourceHelpers with GroupPageHelpers with FSHelpers with TagForms{

  import DependencyFactory._

  private val logger = Logger(classOf[GroupPageFiles])
  private val c3 = inject[C3System].open_!

  override lazy val activeLocId = "files"
  override lazy val group = data.group
  lazy val path = data.path

  def render = {
    val file = group.getFile(data.currentAddress)
    val pathLocs = buildPathLocs
    val groupFilesLink = group.createLink + "/files/"

    ".base_files_path *" #> (
      ".link [href]" #> groupFilesLink &
        ".link *" #> group.name.is
      ) &
      ".bcrumb *" #> (pathLocs.map{ (loc: Loc[_]) =>
        (if(isLocCurrent(pathLocs, loc)){
          ".link" #> <strong>{loc.title}</strong>
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
      ".back_btn [href]" #> (pathLocs.reverse match {
        case Nil => Text(groupFilesLink)
        case xs => xs.tail.headOption.map((l: Loc[_]) => l.createDefaultLink.get)
          .getOrElse(Text(groupFilesLink))
      }) &
      (file match {
        case Empty => S.redirectTo("/404.html"); "* *" #> PassThru
        case Failure(msg, t, chain) => {
          logger.error("Error accessing file: " + msg, t)
          S.redirectTo("/404.html")
          "* *" #> PassThru
        }

        case Full(f) => {
          //Type matching is not gonna work since in local c3 accesslib implementation all nodes are both files and directories
          if(!f.isDirectory){
            renderFileLoc(f.asFile)
          }else{
            if(!S.uri.endsWith("/"))
              S.redirectTo(S.uri + "/")
            else
              renderDirectoryLoc(f.asDirectory)
          }
        }
        case _ => "* *" #> PassThru
      })
  }

  protected def renderDirectoryLoc(d: C3Directory): CssSel = {

    object selectedResourcePaths extends RequestVar[Set[String]](Set())

    val currentPathLink = data.group.createLink + "/files" + data.currentAddress

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
      "#new-directory" #> newDirectoryForm(d, currentPath = currentPathLink)
  }

  protected def renderFileLoc(f: C3File): CssSel = {
    val meta = f.metadata
    val metadataUser = Metadata.filterSystemKey(meta)
    val owner = nodeOwner(f)
    f.versions.lastOption.map(_.length).getOrElse("None")
    ".file_tags" #> meta.get(TAGS_META).map(_.split(TAGS_SEPARATOR).toList).getOrElse(Nil).map((tag: String) => {
      ".label *" #> tag
    }) &
      "#node-tags-form" #> NodeSeq.Empty &
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
      "#tags" #> editTags(f) &
      "#meta_edit" #> saveMetadata(f) &
      ".metadata_form" #> metadataUser.map{ case (k, v) => {
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
      }}
  }

  protected def saveMetadata(f: C3File): CssSel ={
    var key = ""
    var value = ""
    def save (){
      val  keyMetadata= key.split("%")
      val  valueMetadata= value.split("%")
      //      var metaKey =  (f.metadata.filterNot{case (k, v) => keySet.contains(k)})
      //      metaKey = metaKey.filterNot{case(k,v)=>keyMetadata.contains(k)}
      //      f.update(MetadataRemove())
      val metadata = keyMetadata.zip(valueMetadata).toMap
      f.update(MetadataUpdate(metadata))
    }
    "name=metadata_key" #> SHtml.onSubmit(key = _) &
      "name=metadata_value" #> SHtml.onSubmit(value = _) &
      "type=submit" #> SHtml.onSubmitUnit(save)
  }

  protected def editTags(f: C3File): CssSel = {
    var tags = ""
    def saveTags() {
      val metadata = Map((TAGS_META -> tags.split(",").map(_.trim).mkString(",")))
      f.update(MetadataUpdate(metadata))
    }
    ".current_tags [value]" #> f.metadata.get(TAGS_META).getOrElse("")&
      "name=tags_edit" #> SHtml.onSubmit(tags = _) &
      "type=submit" #> SHtml.onSubmitUnit(saveTags)
  }

  protected def newDirectoryForm(currentDirectory: C3Directory, currentPath: String): CssSel = {
    var name = ""
    var tags = ""

    def createDirectory(){
      if (name.trim.isEmpty){
        S.error("Directory name cannot be empty")
      } else {
        val metadata = Map((OWNER_ID_META -> User.currentUser.map(_.id.is.toString).open_!),
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

trait C3ResourceHelpers {
  import net.liftweb.util.TimeHelpers._

  val group: Group

  def toCss(directory: C3Directory) = {

    val owner = nodeOwner(directory)

    ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> directory.name &
      ".link [href]" #> (directory.name + "/") &
      ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(directory.name + "/"))&
      ".icon [src]" #> "/images/folder_classic.png" &
      ".created_date *" #> internetDateFormatter.format(directory.date)
  }

  def toCss(file: C3File) = {
    val owner = nodeOwner(file)

    ".owner *" #> owner.map(_.shortName).getOrElse("Unknown") &
      ".owner [href]" #> owner.map(_.createLink) &
      ".name *" #> file.name &
      ".link [href]" #> file.name &
      ".child_td [onclick]" #> SHtml.ajaxInvoke(() => JsCmds.RedirectTo(file.name))&
      ".icon [src]" #> "/images/document_letter.png" &
      ".created_date *" #> internetDateFormatter.format(file.date)
  }

  def fileDownloadUrl(file: C3File): String = "/download" + file.fullname + "?dl=true"

  def fileViewUrl(file: C3File): String = "/download" + file.fullname

  def nodeOwner(node: C3FileSystemNode): Box[User] = node.metadata.get(OWNER_ID_META) match {
    case Some(id) if !id.isEmpty => User.find(By(User.id, id.toLong))
    case _ => Empty
  }
}

trait FSHelpers {

  case class FSLoc(path: List[String]){
    def title: String = path.lastOption.getOrElse("N/A")
  }

  def transformToPathLists(fullPath: List[String]): List[List[String]] = {
    @tailrec
    def transform(acc: List[List[String]], fullPath: List[String]): List[List[String]] = {
      fullPath match {
        case x :: Nil => acc
        case _ :: tail => transform(tail.reverse :: acc, tail)
        case Nil => acc
      }
    }

    if (fullPath.isEmpty) Nil
    else transform(List(fullPath), fullPath.reverse)
  }


}