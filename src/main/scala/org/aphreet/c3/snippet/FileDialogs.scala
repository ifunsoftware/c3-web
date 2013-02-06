/**
 * Copyright (c) 2011, Dmitry Ivanov
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

import _root_.net.liftweb._
import http._
import js._
import JsCmds._
import js.jquery.JqJsCmds
import JqJsCmds._
import common._
import util._
import Helpers._
import xml.NodeSeq
import org.aphreet.c3.apiaccess.C3
import com.ifunsoftware.c3.access.C3AccessException
import org.aphreet.c3.util.C3Loggable
import net.liftweb.http.S

trait C3ResourceMetadataForms {

  protected object tags extends RequestVar[scala.collection.mutable.Set[String]] (scala.collection.mutable.Set()) {

    val divTagsName: String = S.attr("id_tags_name") openOr "tags_id"

    def toXML(xml: NodeSeq): NodeSeq = {
      tags.get.toList.sortWith(_ > _).flatMap(
        (tag: String) =>
          bind("tag", xml,
            "text" -> ( SHtml.text(tag, (str) => { tags.get += str }, "placeholder" -> "e.g. IT","size"-> "8" ) )
          )
      ): NodeSeq
    }

    def toForm(xml: NodeSeq): NodeSeq = {

      def addTag(): JsCmd = {
        tags += ""
        //full reload
        SetHtml(divTagsName, toForm(xml = xml))
      }
      SHtml.ajaxForm(
        bind("list",xml,
          "tags_list" -> ( (ns: NodeSeq) => tags.toXML(ns) ),
          "add_tag" -> ( (ns: NodeSeq) => SHtml.hidden(addTag _) ++
            SHtml.submit(ns.text, () => {
            })
            )
        )
      )
    }
  }


  protected object metadata extends RequestVar[scala.collection.mutable.Map[String,String]] (scala.collection.mutable.Map()) {

    val divMDName: String = S.attr("id_md_name") openOr "md_id"

    def toXML(xml: NodeSeq): NodeSeq = {

      metadata.get.toList.sortWith(_._1 > _._1).flatMap(
        mdNode => {

          var tmpMDName = mdNode._1
          var tmpMDValue = mdNode._2

          bind("md_node",xml,
            "name" -> SHtml.text(mdNode._1, tmpMDName = _ , "placeholder" -> "e.g. author","size"-> "8" ),
            "value" -> SHtml.text(mdNode._2,tmpMDValue = _ , "placeholder" -> "e.g. Jack Jones","size"-> "8" )
          ) ++ SHtml.hidden( () => {
            if(tmpMDName != mdNode._1) {
              metadata -= mdNode._1
            }
            metadata+=(tmpMDName -> tmpMDValue)
          })
        }
      ) : NodeSeq

    }


    def toForm(xml: NodeSeq): NodeSeq = {

      def addMDNode(): JsCmd = {

        metadata += ( ("", "") )
        //full reload
        SetHtml(divMDName, toForm(xml = xml))
      }

      SHtml.ajaxForm(
        bind("list",xml,
          "md_list" ->( (ns: NodeSeq) => metadata.toXML(ns) ),
          "add_md_node" -> ((ns: NodeSeq) => SHtml.hidden(addMDNode _) ++
            SHtml.submit(ns.text, () => {

            }) )
        ))
    }

  }

}

trait AbstractFormDialog {

  val templateName: String

  protected  object theCurrentPath extends SessionVar[Box[String]](Empty)

  def unblockForm(xhtml: NodeSeq) =
    bind("form",xhtml,
      "close" -> ((b: NodeSeq) => <button onclick={Unblock.toJsCmd}>{b}</button>)
    )

  def button(in: NodeSeq, path: Box[String]) = {
    theCurrentPath.set(path);
    SHtml.ajaxButton(in,
      () => {
        theCurrentPath.set(path)
        S.runTemplate(List(templateName)).
          map(ns => ModalDialog(ns)) openOr
          Alert("Couldn't find "+templateName+" template")
      })
  }
  def form(xhtml: NodeSeq): NodeSeq

}

class FileUploadDialog extends AbstractFormDialog with C3ResourceMetadataForms {

  val templateName = "_jsdialog_fileupload"

  private object theFileName extends RequestVar[Box[String]](Empty)
  private object theFileUpload extends RequestVar[Box[FileParamHolder]](Empty)

  // temporary we wil use another button function that will display file upload box (without ajax because it doesn't work correctly here)

  def button(in: NodeSeq) =
    SHtml.ajaxButton(in,
      () => {
        JsCmds.Run("showDiv('fileupload')")
      }
    )

  private def uploadFile(): JsCmd = {
    theFileName.is match {
      case Full(some) => Alert("Stub for file upload!")
      case _ => Alert("File is empty :(") & Unblock
    }

  }

  override def form(xhtml: NodeSeq) = {
    SHtml.ajaxForm(
      bind("file", xhtml,
        "name" -> SHtml. text("", (name: String) => if(name != "") theFileName.set(Full(name)) ),
        "file_upload" -> SHtml.fileUpload(ul => theFileUpload.set(Full(ul))),
        "metadata" -> ( (ns: NodeSeq) =>
          metadata.toForm(ns)
          ),
        "tags" -> ( (ns: NodeSeq) =>
          tags.toForm(ns)
          ),
        "submit" ->((ns: NodeSeq) => SHtml.hidden(uploadFile _) ++ SHtml.submit(ns.text, () => {} ))
      )
    )
  }


}

class CreateDirectoryDialog extends AbstractFormDialog with C3ResourceMetadataForms with C3Loggable{

  val templateName = "_jsdialog_createdirectory"

  private object theDirectoryName extends RequestVar[Box[String]](Empty)

  private def createDirectory(): JsCmd = {
    if(theCurrentPath.isDefined)
      theDirectoryName.is match {
        case Full(name) => {
          try {
            logger.debug("Trying to create a directory " + name + " in directory " + theCurrentPath.get.open_!)
            C3().getFile(theCurrentPath.get.open_!).asDirectory.createDirectory(name)
            S.notice("Directory " + name + " created")
            Unblock & RedirectTo("")
          } catch {
            case e: C3AccessException => {
              logger.error(e.getMessage, e)
              S.error("Directory " + name + " wasn't created!")
              Unblock & RedirectTo("")
            }
          }
        }
        case _ => {
          S.error("Directory name is empty")
        }
      }
    else {
      S.error("Internal error: unknown current path.")
      Unblock & RedirectTo("")
    }
  }

  override def form(xhtml: NodeSeq): NodeSeq = {

    SHtml.ajaxForm(
      bind("dir", xhtml,
        "name" -> SHtml.text("", (name: String) => if(name != "") theDirectoryName.set(Full(name)) ),
        "metadata" -> ( (ns: NodeSeq) =>
          metadata.toForm(ns)
          ),
        "tags" -> ( (ns: NodeSeq) =>
          tags.toForm(ns)
          ),
        "submit" ->((ns: NodeSeq) => SHtml.hidden(createDirectory _) ++ SHtml.submit(ns.text, () => {} ))
      )
    )
  }
}

