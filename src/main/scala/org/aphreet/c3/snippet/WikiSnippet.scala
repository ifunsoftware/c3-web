/**
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright 
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above 
 * copyright notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors 
 * may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.snippet

import net.liftweb.util.BindHelpers._
import org.aphreet.c3.model.Wiki
import be.devijver.wikipedia.SmartLink.SmartLinkType
import org.aphreet.c3.lib.wiki.C3HtmlVisitor
import be.devijver.wikipedia.{SmartLinkResolver, Parser, SmartLink}
import java.io.StringWriter
import org.aphreet.c3.view.GroupNavigationUtil
import net.liftweb.http._
import net.liftweb.common.{Box, Empty, Logger, Full}
import xml.{Text, XML, NodeSeq}
import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.service.groups.wiki.WikiService

class WikiSnippet{

  lazy val wikiService = inject[WikiService].open_!

  val logger = Logger(classOf[WikiSnippet])

  def view(html: NodeSeq) : NodeSeq = {

    val pageName = S.param("pagename") match {
      case Full(value) => value
      case _ => "Main"
    }

    val groupName = S.param("groupname") match {
      case Full(value) => value
      case _ => ""
    }

    def bindWikiPage(page:String, content:NodeSeq,  metadata:Map[String, String]):NodeSeq = {
      bind("wiki", html,
        "groupname" -> groupName,
        "groupnav" -> GroupNavigationUtil.createNavigation(groupName),

        "name" -> page,
        "content" -> content,
        "actions" -> <a href={"/group/" + groupName + "/wiki/" + pageName + "/edit"}>Edit</a>,
        "metadata" -> {(ns:NodeSeq) =>
          metadata.flatMap(el => bind("md", ns, "key" -> el._1, "value" -> el._2)).toSeq:NodeSeq}
      )
    }

    wikiService.getPage(groupName, pageName) match {
      case Full(page) => bindWikiPage(page.name, XML.loadString(formatContent(page.content, groupName)), page.metadata)
      case _ => bindWikiPage(pageName, Text("Page not found"), Map())
    }
  }

  def formatContent(content:String, group:String):String = {

    val writer = new StringWriter

    new Parser().withVisitor(content.replaceAll("([^\r])\n", "$1\r\n"), new C3HtmlVisitor(writer, new SmartLinkResolver(){

      override def  resolve(key:String):SmartLink = {
        resolveSmartLink(key, group)
      }

    }))

    writer.toString
  }

  def resolveSmartLink(key:String, group:String):SmartLink = {
    new SmartLink("/group/" + group + "/wiki/" + key, key, SmartLinkType.A_LINK);
  }


  def edit(html: NodeSeq) : NodeSeq = {

    val pageName = S.param("pagename") match {
      case Full(value) => value
      case _ => "Main"
    }

    val groupName = S.param("groupname") match {
      case Full(value) => value
      case _ => ""
    }

    bind("wiki", html,
      "groupname" -> groupName,
      "groupnav" -> GroupNavigationUtil.createNavigation(groupName),
      "name" -> pageName,
      "actions" -> <a href={"/group/" + groupName + "/wiki/" + pageName}>Cancel</a>,
      "metadata" -> {(ns:NodeSeq) =>
        wikiService.getMetadata(groupName, pageName).flatMap(i => bind("md", ns, "key" -> i._1, "value" -> i._2)).toSeq:NodeSeq}
    )

  }

  private object pagePreview extends RequestVar[Box[String]](Empty)
  private object pageContent extends RequestVar[Box[String]](Empty)

  def form(html: NodeSeq) : NodeSeq = {

    var submittedContent:String = null
    val pageName = S.param("pagename") match {
      case Full(value) => value
      case _ => "Main"
    }

    val groupName = S.param("groupname") match {
      case Full(value) => value
      case _ => ""
    }

    def processWikiEdit() = {
      wikiService.getPage(groupName, pageName) match {
        case Full(page) => {
          page.content = submittedContent
          wikiService.savePage(groupName, page)
          S.notice("Page saved")
          S.redirectTo("/group/" + groupName + "/wiki/" + pageName)
        }

        case _ => {

          val page = new Wiki(pageName, submittedContent)

          wikiService.createPage(groupName, page)
          S.notice("Page created")
          S.redirectTo("/group/" + groupName + "/wiki/" + pageName)
        }
      }
    }

    def processPreview() = {
      try{
        val formattedContent = formatContent(submittedContent, groupName)
        pagePreview.set(Full(formattedContent))
        pageContent.set(Full(submittedContent))
      } catch {
        case e: Exception => S.error("Failed to parse page: " + e.getMessage)
      }
    }

    val pageString = wikiService.getPage(groupName, pageName) match {
      case Full(page) => page.content
      case _ => ""
    }

    val previewXml = pagePreview.get match{
      case Full(value) => XML.loadString(value)
      case _ => <span/>
    }

    val editablePageContent = pageContent.get match{
      case Full(value) => value
      case _ => pageString
    }

    import net.liftweb.http.SHtml._

    bind("wiki-form", html,
      "content" -> textarea(editablePageContent, submittedContent = _),
      "preview" -> previewXml,
      "submit" -> submit("Save", processWikiEdit),
      "submit-preview" -> submit("Preview", processPreview)
    )

  }

}