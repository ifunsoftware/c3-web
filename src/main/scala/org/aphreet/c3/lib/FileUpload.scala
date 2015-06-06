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
import net.liftweb.http._
import net.liftweb.common.Box
import org.aphreet.c3.lib.DependencyFactory._
import com.ifunsoftware.c3.access.{C3AccessException, DataStream, C3System}
import com.ifunsoftware.c3.access.C3System._
import org.aphreet.c3.lib.metadata.Metadata._
import org.aphreet.c3.util.{C3Exception, C3Loggable}
import org.aphreet.c3.model.{Group, User}
import net.liftweb.util.Helpers
import Helpers._
import net.liftweb.http.InMemoryResponse
import net.liftweb.common.Full
import net.liftweb.http.BadResponse
import org.aphreet.c3.comet.{MessageServerFactory, JournalServer, JournalServerEvent}
import org.aphreet.c3.service.journal.EventType
import org.aphreet.c3.util.helpers.FileTransferHelper

// for ajax file upload
object FileUpload extends RestHelper with C3Loggable {
  val c3 = inject[C3System].openOrThrowException("Cannot wire a C3 System")

  private val RegexGroupId = """.*groups/([^/]*)/.*""".r
  private val isCallbackEnabled = false

  serve {
    case "upload" :: "file" :: currentPath Post req => {
      withCurrentUserAndGroupCtx(req, currentPath) {

        val uploads = req.uploadedFiles
        logger.info("Uploaded files: " + uploads)

        val filePath = c3FilePath(currentPath)

        logger.info("Path to upload: " + filePath)
        userGroupIds: UserGroupIds => {

          try {
            val ojv: List[JObject] = uploads.map { fph =>
              val url = removeTrailingIndex(currentPath).mkString("/", "/", "/") + fph.fileName
              val description = req.param(s"description_${fph.fileName}") match {
                case Full(temp) => temp;
                case _ => ""
              }

              val tags = req.param(s"tags_${fph.fileName}") match {
                case Full(temp) => temp;
                case _ => ""
              }

              if (description == "") { throw new IllegalArgumentException("Description is empty") }
              if (tags == "") { throw new Exception("Tags are empty") }

              val fileMetadata: Map[String, String] =
                  Map((OWNER_ID_META -> userGroupIds.userId), (GROUP_ID_META -> userGroupIds.groupId), (DESCRIPTION_META -> description), (TAGS_META -> tags))
                //req param("metadata") map(s => Map((TAGS_META -> s))) openOr Map()
                uploadToC3(fph, filePath, fileMetadata)
                ("name" -> fph.fileName) ~
                  ("url" -> url) ~
                  ("sizef" -> fph.length) ~
                  ("delete_url" -> ("/delete/file" + url)) ~
                  ("delete_type" -> "DELETE")
              }

              val jr = JsonResponse(ojv).toResponse.asInstanceOf[InMemoryResponse]
              InMemoryResponse(jr.data, ("Content-Length", jr.data.length.toString) ::
                ("Content-Type", "text/plain") :: S.getHeaders(Nil),
                S.responseCookies, 200)

          } catch {
            case e: C3AccessException => {
              if (e.message.endsWith("already exists")) // that's ugly, need a proper cause from access api
                errorResponse(uploads, 409, "File already exists")
              else
                BadResponse()
            }
            case e: IllegalArgumentException => {
              if (e.getMessage().endsWith("empty"))
                errorResponse(uploads, 404, e.getMessage())
              else
                BadResponse()
            }
          }
        }
      }
    }
    case "replace" :: "file" :: currentPath Post req => {
      withCurrentUserAndGroupCtx(req, currentPath) {
        userGroupIds: UserGroupIds => {

          val uploads = req.uploadedFiles
          logger.info("Uploaded files: " + uploads)

          val filePath = c3FilePath(currentPath)
          var oldFilePath = filePath.init ::: reunite(filePath.last, req.path.suffix) :: Nil;

          val oldFile = c3.getFile(oldFilePath.mkString("/", "/", ""))
          logger.info("Path to upload: " + filePath)
          try {
            val ojv: List[JObject] = uploads.map { fph =>
              val url = removeTrailingIndex(currentPath).mkString("/", "/", "/") + fph.fileName
              val fileMetadata: Map[String, String] =
                Map((OWNER_ID_META -> userGroupIds.userId), (GROUP_ID_META -> userGroupIds.groupId), (DESCRIPTION_META -> oldFile.metadata.get(DESCRIPTION_META).getOrElse("")),
                  (TAGS_META -> oldFile.metadata.get(TAGS_META).getOrElse("")))
              val group = Group.findById(filePath.head).open_!
              val relativeFilePathString = filePath.drop(2) mkString "/"
              if (group.getFile(group.trashCanDirectory).openOr(null) == null) createTrashCan(group);
              FileTransferHelper.moveToTrashCan(oldFile.name, group, "/" + relativeFilePathString, true)
              uploadToC3(fph, filePath.dropRight(1), fileMetadata)
              ("name" -> fph.fileName) ~
                ("url" -> url) ~
                ("sizef" -> fph.length) ~
                ("delete_url" -> ("/delete/file" + url)) ~
                ("delete_type" -> "DELETE")
            }

            val jr = JsonResponse(ojv).toResponse.asInstanceOf[InMemoryResponse]
            InMemoryResponse(jr.data, ("Content-Length", jr.data.length.toString) ::
              ("Content-Type", "text/plain") :: S.getHeaders(Nil),
              S.responseCookies, 200)
          } catch {
            case e: C3AccessException => {
              if (e.message.endsWith("already exists")) // that's ugly, need a proper cause from access api
                errorResponse(uploads, 409, "File already exists")
              else
                BadResponse()
            }
          }
        }
      }
    }
    case "delete" :: "file" :: currentPath Delete req => {
      withCurrentUserAndGroupCtx(req, currentPath) {
        userGroupIds: UserGroupIds => {
          val filePath = c3FilePath(currentPath)
          logger.info("File to be deleted: " + removeTrailingIndex(currentPath).mkString("/", "/", ""))
          deleteFromC3(filePath.init ::: reunite(filePath.last, req.path.suffix) :: Nil)
          OkResponse()
        }
      }

    }
  }

  case class UserGroupIds(userId: String, groupId: String)

  private def createTrashCan(group: Group): Unit = {
    val root = c3.getFile("/").asDirectory
    root.getChild(group.getId) match {
      case Some(node) =>
        val dir = node.asDirectory
        dir.getChild("files") match {
          case Some(node) =>
            val files = node.asDirectory
            val metadata = Map(OWNER_ID_META -> dir.metadata.get(OWNER_ID_META).getOrElse(User.id.is.toString),
              GROUP_ID_META -> group.getId,
              TAGS_META -> "Trash",
              DESCRIPTION_META -> "",
              ACL_META -> dir.metadata.get(ACL_META).getOrElse(""))
            files.createDirectory(group.trashCanName, metadata)
          case None => throw new C3Exception("Failed to create trash can for group " + group.getId)
        }
      case None => throw new C3Exception("Can't find group with id " + group.getId)
    }
  }



  private def withCurrentUserAndGroupCtx(req: Req, currentPath: List[String])(block: UserGroupIds => LiftResponse): LiftResponse = {
    val currentUser = User.currentUser
    val groupIdOpt = extractGroupId(currentPath)

    tryo( (groupIdOpt.get, currentUser.map(_.id.is.toString).open_!) ) match {
      case Full((groupId, userId)) => {
        block(UserGroupIds(userId, groupId))
      }
      case _ => {
        logger.error("Group id or current user id cannot be recognised. Path for upload: " + currentPath)
        BadResponse()
      }
    }
  }

  private def c3FilePath(currentPath: List[String]) = removeTrailingIndex(currentPath) match {
    case "groups" :: xs => xs
    case xs => xs
  }

  private def removeTrailingIndex(path: List[String]) = path.reverse.dropWhile(_ == "index").reverse

  private def reunite(name: String, suffix: String) = if (suffix.isEmpty) name else name + "." + suffix

  private def uploadToC3(fph: FileParamHolder, filePath: List[String], metadata: Map[String, String]){
    logger info String.format("Uploading file %s to C3..", fph.name)
    val parentDirectory = c3.getFile(filePath.mkString("/", "/", "")).asDirectory
    // add to file ACL_META from parent directory
    val newMD:Map[String,String] =  metadata + (ACL_META -> parentDirectory.metadata.get(ACL_META).getOrElse(""))
    parentDirectory.createFile(fph.fileName, newMD, DataStream(fph.file))

    val group: Box[Group] = Group.findById(filePath.head)
    val journalServer: Box[JournalServer] = group.map(MessageServerFactory(_))
    journalServer.foreach(_ ! JournalServerEvent(User.currentUserUnsafe, group.open_!, EventType.CreateResources, parentDirectory.fullname +"/"+fph.fileName))
    logger info String.format("File %s is uploaded to C3!", fph.name)
  }

  private def deleteFromC3(filePath: List[String]){
    val fileShortName = filePath.lastOption.getOrElse("<Unknown>")
    logger info s"Removing file $fileShortName from C3.."
    c3.deleteFile(filePath.mkString("/", "/", ""))
    logger info s"File $fileShortName is removed from C3!"
  }

  private def extractGroupId(fullPath: List[String]): Box[String] = {
    logger.debug("Extracting group id from the full path " + fullPath)
    val id = tryo{
      val RegexGroupId(groupId) = fullPath.mkString("/")
      groupId
    }
    id
  }

  private def errorResponse(fphs: List[FileParamHolder], errorCode: Int, errorMsg: String): LiftResponse = {
    val ojv: List[JObject] = fphs.map { fph =>
      ("name" -> fph.fileName) ~
        ("sizef" -> fph.length) ~
        ("error" -> errorMsg)
    }

    val jr = JsonResponse(ojv).toResponse.asInstanceOf[InMemoryResponse]
    InMemoryResponse(jr.data,  ("Content-Length", jr.data.length.toString) ::
      ("Content-Type", "text/plain") :: S.getHeaders(Nil),
      S.responseCookies, errorCode)
  }

  def init(){
    logger.info("Ajax file upload is initializing")
    if (isCallbackEnabled){
      logger.info("Callbacks are enabled for ajax file upload")
      // rewrite so the rest-callback will be a param instead to be fired with LiftSession.runParams
      LiftRules.statelessRewrite.append {
        case RewriteRequest(ParsePath("upload" :: "file" :: callback :: Nil, "", true, _), _, _) =>
          RewriteResponse("upload" :: "file" :: Nil, Map("callback" -> "_"))
      }

      LiftRules.dispatch.append(this)
    }
  }
}
