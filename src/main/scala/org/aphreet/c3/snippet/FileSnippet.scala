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

import org.aphreet.c3.model.Group
import net.liftweb.mapper.By
import net.liftweb.common.{Logger, Full}
import net.liftweb.util.BindHelpers._
import net.liftweb.http.S
import xml.NodeSeq
import org.apache.commons.httpclient.util.URIUtil
import org.aphreet.c3.apiaccess.C3
import com.ifunsoftware.c3.access.C3AccessException

class FileSnippet {

  val logger = Logger(classOf[FileSnippet])
  
  def fileDirectory(group:String):String = {
    "/" + group + "/files/"
  }

  def overview(html: NodeSeq) : NodeSeq = {
    S.param("groupname") match {
      case Full(groupName) => {

        Group.find(By(Group.name,groupName)) match {
          case Full(group) => {

            S.param("filepath") match {
              case Full(filepath) => {
                // TODO C3 get resource
                try{

                  C3().getFile(fileDirectory(groupName) + filepath).metadata

                  val fileName = filepath.split("/").last
                  val url = URIUtil.encodeQuery("/download/"+groupName+"/files/"+filepath,"UTF-8")

                  bind("resource", html,
                    "preview" -> <iframe src={url}></iframe>,
                    "group_name" -> groupName,
                    "file_name" -> fileName,
                    "url" -> ( (ns: NodeSeq) => <a href={url}>{ns}</a> )
                    //"md" -> resMD
                  )

                }
                catch {
                  case e: C3AccessException => {
                    S error e.toString
                    logger error e.toString
                    NodeSeq.Empty
                  }
                }


              }
              case _ => {
                S.error("Path is invalid.")
                NodeSeq.Empty
              }
            }

          }
          case _ => {
            S.error("Group "+groupName+" was not found.")
            NodeSeq.Empty
          }
        }

      }
      case _ => {
        S.error("Group name is invalid.")
        NodeSeq.Empty
      }
    }
  }

}