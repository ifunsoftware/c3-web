package org.aphreet.c3.lib

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

import net.liftweb.http.rest._
import net.liftweb.json._
import JsonDSL._
import net.liftweb.http.{FileParamHolder, S, InMemoryResponse, JsonResponse}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.common.{Full, Logger, Box}
import org.aphreet.c3.lib.DependencyFactory._
import com.ifunsoftware.c3.access.{DataStream, C3System}
import org.aphreet.c3.lib.metadata.Metadata._
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.model.User
import net.liftweb.http.BadResponse
import net.liftweb.util.Helpers
import Helpers._

// for ajax file upload
object FileUpload extends RestHelper with C3Loggable{
  val c3 = inject[C3System].open_!

  private val RegexGroupId = """.*groups/([^/]*)/.*""".r

  serve {
    case "upload" :: "file" :: currentPath Post req => {
      logger.info("Uploaded files: " + req.uploadedFiles)

      // we imply that currently "metadata" is a list of tags
      val currentUser = User.currentUser
      val groupIdOpt = extractGroupId(currentPath)

      tryo( (groupIdOpt.get, currentUser.map(_.id.is.toString).open_!) ) match {
        case Full((groupId, userId)) => {
          val fileMetadata: Map[String, String] = req param("metadata") map(s => Map((TAGS_META -> s),
            (OWNER_ID_META -> userId), (GROUP_ID_META -> groupId))) openOr Map()

          def removeTrailingIndex(path: List[String]) = path.reverse.dropWhile(_ == "index").reverse

          val filePath: List[String] = removeTrailingIndex(currentPath) match {
            case "groups" :: xs => xs
            case xs => xs
          }

          logger.info("Path to upload: " + filePath)

          val ojv: Box[JValue] =
            req.uploadedFiles.map(fph => {
              uploadToC3(fph, filePath, fileMetadata)
              ("name" -> fph.fileName) ~
                ("type" -> fph.mimeType) ~
                ("size" -> fph.file.length)
            }).headOption

          val ajv = ("name" -> "n/a") ~ ("type" -> "n/a") ~ ("size" -> 0L) ~ ("yak" -> "brrrr")

          val ret = ojv openOr ajv

          // This is a tad bit of a hack, but we need to return text/plain, not JSON
          val jr = JsonResponse(ret).toResponse.asInstanceOf[InMemoryResponse]
          InMemoryResponse(jr.data, ("Content-Length", jr.data.length.toString) ::
            ("Content-Type", "text/plain") :: S.getHeaders(Nil),
            S.responseCookies, 200)
        }
        case _ => {
          logger.error("Group id or current user id cannot be recognised. Path for upload: " + currentPath)
          BadResponse()
        }
      }
    }
  }

  private def uploadToC3(fph: FileParamHolder, filePath: List[String], metadata: Map[String, String]){
    logger info String.format("Uploading file %s to C3..", fph.name)
    c3.getFile(filePath.mkString("/", "/", "")).asDirectory.createFile(fph.fileName, metadata, DataStream(fph.file))
    logger info String.format("File %s is uploaded to C3!", fph.name)
  }

  private def extractGroupId(fullPath: List[String]): Box[String] = {
    logger.debug("Extracting group id from the full path " + fullPath)
    val id = tryo{
      val RegexGroupId(groupId) = fullPath.mkString("/")
      groupId
    }
    id
  }
}
