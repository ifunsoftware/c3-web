package org.aphreet.c3.snippet.groups.snippet

import org.aphreet.c3.snippet.groups.{GroupPageFilesData, GroupPageData}
import org.aphreet.c3.loc.{ItemRewriteLoc, SuffixLoc}
import org.aphreet.c3.model.{File, Catalog, C3Resource, Group}
import net.liftweb.common._
import net.liftweb.sitemap.Loc.Link
import xml.{NodeSeq, Text}
import net.liftweb.util.BindHelpers._
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.common.Full
import org.aphreet.c3.snippet.groups.GroupPageFilesData
import org.aphreet.c3.snippet.{FileUploadDialog, CreateDirectoryDialog}
import org.apache.commons.httpclient.util.URIUtil
import net.liftweb.sitemap.Loc


/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 *         iFunSoftware
 */
object GroupPageFiles extends ItemRewriteLoc[Group, GroupPageFilesData] with SuffixLoc {

  override val name = "Files"
  override val pathPrefix = "groups" :: Nil
  override val pathSuffix = "files" ::  Nil
  override def getItem(id: String) = Group.find(id)

  // we don't use it here
  override def wrapItem(groupBox: Box[Group]) = Empty
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

class GroupPageFiles(data: GroupPageFilesData) extends C3ResourceHelpers with GroupPageHelpers {

  private val logger = Logger(classOf[GroupPageFiles])

  override lazy val activeLocId = "files"
  override lazy val group = data.group
  lazy val path = data.path

  def render = {
    ".current_path *" #> Text(data.currentAddress) &
    ".create_dir" #> ((ns: NodeSeq) => new CreateDirectoryDialog().button(ns, Full("/" + data.group.id.is + "/files" + data.currentAddress))) &
    (if(data.isDirectoryLoc){
      if(!S.uri.endsWith("/"))
        S.redirectTo(S.uri + "/")
      else
        renderDirectoryLoc
    } else
      renderFileLoc)
  }

  protected def renderDirectoryLoc = {
    ".child *" #> group.getChildren(data.currentAddress).map {
      resource => {
        resource match {
          case c: Catalog => toCss(c)
          case f: File => toCss(f)
        }
      }
    } &
    ".file-view" #> NodeSeq.Empty
  }

  protected def renderFileLoc = {
    val file = group.getFile(data.currentAddress)
    file match {
      case Full(f) => {
        ".label_file" #> f.tags.map(tag =>{
          ".label *" #> tag
        })&
        ".file-table" #> NodeSeq.Empty &
        ".fs_toolbar" #> NodeSeq.Empty &
        "#upload_form" #>NodeSeq.Empty &
        ".name_file *" #> f.name &
        ".download_btn [href]" #> fileDownloadUrl(f)&
        ".data_file *" #> internetDateFormatter.format(f.created)
      }
      case Failure(msg, t, chain) => {
        S.warning("File not found")
        "* *" #> NodeSeq.Empty
      }
      case _ => {
        S.error("Oops, something bad is happened")
        "* *" #> NodeSeq.Empty
      }
    }
  }
}

trait C3ResourceHelpers {
  import net.liftweb.util.TimeHelpers._

  def toCss(directory: Catalog) = {
    ".name *" #> directory.name &
    ".link [href]" #> (directory.name + "/") &
    ".icon [class+]" #> "icon-folder-close" &
    ".created_date *" #> internetDateFormatter.format(directory.created)
  }

  def toCss(file: File) = {
    ".name *" #> file.name &
    ".link [href]" #> file.name &
    ".icon [class+]" #> "icon-file" &
    ".created_date *" #> internetDateFormatter.format(file.created)
  }

  def fileDownloadUrl(file: File): String = "/download/" + file.group.id.is + "/files" + file.path
}