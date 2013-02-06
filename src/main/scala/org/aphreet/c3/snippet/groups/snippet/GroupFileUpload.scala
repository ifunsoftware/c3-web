package org.aphreet.c3.snippet.groups.snippet

import scala.xml.NodeSeq
import net.liftweb.http.{S, FileParamHolder, SHtml}
import SHtml._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import org.aphreet.c3.lib.DependencyFactory._
import net.liftweb.http.js.JsCmds.Alert
import net.liftweb.common.Full
import com.ifunsoftware.c3.access.{DataStream, C3AccessException, C3System}
import javax.activation.MimetypesFileTypeMap
import org.apache.commons.httpclient.util.URIUtil
import org.aphreet.c3.model.Group

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
class GroupFileUpload(group: Group, currentPath: String) {
  val c3 = inject[C3System].open_!
  val logger = Logger(classOf[GroupFileUpload])

  val fullPath = group.baseFilePath + currentPath

  def fileUploadForm(form:NodeSeq) : NodeSeq = {
    var fileHolder: Box[FileParamHolder] = Empty

    def handleFile() = {
      fileHolder.map { holder =>
        uploadFile(holder, uploadFileToPath(URIUtil.decode(fullPath, "UTF-8"), holder.fileName))
        Alert("File is " + holder.fileName + " uploaded to path " + currentPath)
      } openOr {
        Alert("Well *that* upload failed...")
      }
    }

    val bindForm =
      "type=file" #> fileUpload((fph) => fileHolder = Full(fph)) &
      "type=submit" #> ajaxSubmit("Submit", handleFile _)

    ajaxForm(
      bindForm(form)
    )
  }

  private def uploadFile(fph: FileParamHolder, uploadMethod: (Array[Byte], Map[String,String]) => Unit ) = {
    logger.debug("Got a file "+fph.fileName)
    // this simple technique helps to predict uploaded file's type by it's name
    val mimeType: String = new MimetypesFileTypeMap().getContentType(fph.fileName)
    try {
      uploadMethod(fph.file, Map("content.type" -> mimeType))
    } catch {
      case e: C3AccessException => {
        S.error(e.toString)
      }
    }
  }

  def uploadFileToPath(path:String, name:String)(data:Array[Byte], metadata:Map[String, String]){
    c3.getFile("/" + path).asDirectory.createFile(name, metadata, DataStream(data))
  }

}
