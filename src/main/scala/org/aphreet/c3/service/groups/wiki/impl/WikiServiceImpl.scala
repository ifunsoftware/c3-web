package org.aphreet.c3.service.groups.wiki.impl

import org.aphreet.c3.model.Wiki
import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.service.groups.wiki.WikiService
import com.ifunsoftware.c3.access.{DataStream, C3System}
import net.liftweb.common.{Box, Logger}
import org.apache.commons.httpclient.util.URIUtil
import net.liftweb.util.Helpers
import Helpers._
import com.ifunsoftware.c3.access.fs.C3Directory


class WikiServiceImpl extends WikiService{

  lazy val c3 = inject[C3System].open_!

  val logger = Logger(classOf[Wiki])

  private def encodeName(name:String):String = {
    URIUtil.encodeQuery(name, "UTF-8")
  }

  private def pageLocation(group:String, name:String) = {
    pageDirectory(group) + encodeName(name)
  }

  private def pageDirectory(group:String):String = {
    "/" + group + "/wiki/"
  }

  def getPage(group:String, name:String): Box[Wiki] =
    tryo {
      val file = c3.getFile(pageLocation(group, name))
      val content = file.versions.last.getData.readContentAsString
      new Wiki(name, content, file.metadata)
    }

  def createPage(group:String, page:Wiki) {
    try{
      c3.getFile(pageDirectory(group)).asInstanceOf[C3Directory]
        .createFile(page.name, Map("content.type" -> "application/x-c3-wiki"), DataStream(page.content))
    } catch {
      case e: Exception => logger.warn("Failed to save resource", e)
    }
  }

  def savePage(group:String, page:Wiki) {
    try{
      c3.getFile(pageLocation(group, page.name)).update(DataStream(page.content))
    } catch{
      case e: Exception => logger.warn("Failed to save resource", e)
    }
  }

  def getMetadata(group:String, name:String):Map[String, String] = {
    try {
      c3.getFile(pageLocation(group, name)).metadata
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Map()
      }
    }
  }
}

object WikiServiceImpl{

  def create():WikiService = new WikiServiceImpl

}
