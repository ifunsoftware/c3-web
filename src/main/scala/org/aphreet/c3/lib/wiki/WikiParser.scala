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

package org.aphreet.c3.lib.wiki

import be.devijver.wikipedia.html.{HtmlEncoder, HtmlVisitor}
import be.devijver.wikipedia.{SmartLinkResolver, SmartLink}
import java.io.Writer
import org.apache.commons.lang.StringEscapeUtils
import annotation.switch

class WikiParser{

}

class C3HtmlVisitor(val writer:Writer, val resolver:SmartLinkResolver)
  extends HtmlVisitor(writer, resolver, new C3HtmlEncoder, true){

  var inHeader = false;
  var inImage = false;



  override def handleString(s:String) = {

    if(inImage){
      val params = s.split("\\|");
      if (params(0).matches("^[\\d]+px$")) {
        output.append(" width=\"" + params(0) + "\"");
      }
      if (params.length > 1) {
        output.append(" alt=\"" + characterEncoder.encode(params(1))
          + "\"");
      }
      output.append("/>");
      inImage = false;
    }else{

      if(inHeader){
        output.append(characterEncoder.encode(s));
        output.append("\">");
        inHeader = false;
      }

      output.append(characterEncoder.encode(s));
    }
  }

  private def resolveSmartLink(s:String):SmartLink = {

    val resolvedLink = smartLinkResolver.resolve(s)

    if(resolvedLink == null){
      throw new Exception("SmartLinkResolver ["
        + smartLinkResolver + "] could not resolved smart link ["
        + s + "]!")
    }

    resolvedLink
  }

  override def handleSmartLinkWithoutCaption(s:String) = {
    val resolvedLink = resolveSmartLink(s)

    if(resolvedLink.isImage){
      output.append("<a href=\""
        + characterEncoder.encode(resolvedLink.getResourceLink())
        + "\"><img src=\""
        + characterEncoder.encode(resolvedLink.getUrl())
        + "\"/></a>");
    }else{
      output.append("<a href=\""
        + characterEncoder.encode(resolvedLink.getUrl()) + "\">"
        + resolvedLink.getName() + "</a>");
    }
  }

  override def startSmartLinkWithCaption(s:String) = {
    val resolvedLink = resolveSmartLink(s)

    if(resolvedLink.isImage()){
      inImage = true
      output.append("<a href=\""
        + characterEncoder.encode(resolvedLink.getResourceLink())
        + "\"><img src=\""
        + characterEncoder.encode(resolvedLink.getUrl()) + "\"");
    }else{
      output.append("<a href=\""
        + characterEncoder.encode(resolvedLink.getUrl()) + "\">");
    }
  }

  override def startDocument = {
    output.append("<div class=\"wiki-content\">")
  }

  override def endDocument = {
    output.append("</div>")
    output.flush
    output.finished
  }

  override def startHeading1 = {
    output.append("<h1 id\"")
    inHeader = true
  }

  override def startHeading2 = {
    output.append("<h2 id=\"")
    inHeader = true
  }

  override def startHeading3 = {
    output.append("<h3 id=\"")
    inHeader = true
  }

  override def startHeading4 = {
    output.append("<h4 id=\"")
    inHeader = true
  }

  override def startHeading5 = {
    output.append("<h5 id=\"")
    inHeader = true
  }

  override def startHeading6 = {
    output.append("<h6 id=\"")
    inHeader = true
  }
}

class C3HtmlEncoder extends HtmlEncoder{

  private def escape(source:String):String = {
    if(source == null){
      null
    }else{
      val builder = new StringBuilder

      for(i <- 0 to source.length - 1){
        val c = source.charAt(i)
        (c : @switch) match {
          case 34 =>
            builder.append("&quot;")
          case 38 =>
            builder.append("&amp;")
          case 60 =>
            builder.append("&lt;")
          case 62 =>
            builder.append("&gt;")
          case _ =>
            builder.append(c)
        }
      }

      builder.toString()
    }
  }

  override def encode(s:String):String = {
    val result = StringEscapeUtils.unescapeHtml(s)
    escape(result)
  }
}

