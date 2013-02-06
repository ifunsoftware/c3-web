/**
 * Copyright (c) 2012, Mikhail Malygin
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
package org.aphreet.c3.model

case class C3Path(path:String){

  var groupName:String = ""

  var resourceName:String = ""

  var resourceType:ResourceType = UnknownType

  var resourceUri = ""

  {
    path.split("/").toList.filter(!_.isEmpty) match {
      case fullpath @ (group :: "files" :: filePath) => {
        groupName = group
        resourceName = filePath.last
        var resourceLink = filePath.filter(_!=resourceName).mkString("/")
        if (!resourceLink.isEmpty){
          resourceLink+='/'
        }
        resourceType = FileType
        resourceUri = "/groups/" + group + "/files/" + resourceLink
      }

      case fullpath @ (group :: "wiki" :: filePath) => {
        groupName = group
        resourceName = filePath.head
        resourceType = WikiType
        resourceUri = "/groups/" + group + "/wiki/" + resourceName
      }
      case fullpath @ (group :: "messages" :: filePath) => {
        groupName = group
        resourceName = filePath.head
        resourceType = MessagesType
        resourceUri = "/groups/" + group + "/messages/" + resourceName
      }

      case _ =>
    }
  }

}

object C3Path {

  def apply(groupId:String, path:List[String], extension:String):String = {
    "/" + groupId + "/" + path.init.mkString("/") + "/" +
      path.last + {
      extension match {
        case "" => ""
        case ext => "." + ext
      }
    }
  }
}

sealed trait ResourceType;
object FileType extends ResourceType
object WikiType extends ResourceType
object UnknownType extends ResourceType
object MessagesType extends ResourceType
