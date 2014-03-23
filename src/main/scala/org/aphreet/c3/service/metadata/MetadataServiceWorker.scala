/**
 * Copyright (c) 2014, Dmitry Ivanov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
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

package org.aphreet.c3
package service.metadata

import akka.actor.{ ActorRef, Actor }
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.service.metadata.MetadataServiceProtocol._
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._
import net.liftweb.common.{ Box, Failure, Full }
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.CreateNotification
import org.aphreet.c3.model.{ Group, User }
import net.liftweb.mapper.By
import net.liftweb.util.Helpers._
import com.ifunsoftware.c3.access.{ MetadataRemove, C3System }
import com.ifunsoftware.c3.access.fs.C3File
import org.aphreet.c3.service.notifications.FileMetaProcessedMsg

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
class MetadataServiceWorker(c3system: C3System, notificationManager: ActorRef) extends Actor with C3Loggable {
  def receive = {
    case ProcessC3Resource(res) => {
      logger.debug("Received a C3 resource " + res.address + " for processing.")
      if (res.metadata.get(S4_PROCESSED_FLAG_META).isDefined) {
        res.metadata.get(TAGS_META) match {
          case Some(tags) => {
            logger.debug("Got tags for resource " + res.address + ": " + tags)
            val ownerIdBox = Box(res.metadata.get(OWNER_ID_META).flatMap(asLong(_)))
            val groupIdBox = Box(res.metadata.get(GROUP_ID_META).flatMap(asLong(_)))

            (for {
              ownerId ← ownerIdBox ?~ "No owner id in metadata provided"
              groupId ← groupIdBox ?~ "No group id in metadata provided"
              owner ← User.find(By(User.id, ownerId)) ?~ ("User with id " + ownerId + " is not found!")
              group ← Group.find(By(Group.id, groupId)) ?~ ("Group with id " + groupId + " is not found!")
            } yield (owner, group)) match {
              case Full((owner: User, group: Group)) => {
                // TODO we do 2 requests to C3... make it in 1
                fsPath(res.address).map(c3system.getFile _) match {
                  case Some(f: C3File) => {
                    notificationManager ! CreateNotification(FileMetaProcessedMsg(f, owner.id.is))
                    res.update(MetadataRemove(List(S4_PROCESSED_FLAG_META))) // updating metadata with s4-meta flag removed
                  }
                  case _ => logger.error("Resource " + res.address + " is not found in virtual FS... Skipping")
                }
              }
              case Failure(msg, _, _) => logger.debug(msg)
              case _                  => logger.error("Something unexpected happen.")
            }
          }
          case _ => logger.debug("Resource " + res.address + " has no tags assigned. Skipping...")
        }
      } else {
        logger.error("C3 resource has no " + S4_PROCESSED_FLAG_META + " flag in metadata. Skipping...")
      }
    }
  }

  private def fsPath(ra: String): Option[String] = {
    c3system.getResource(ra, List(FS_PATH_META)).systemMetadata.get(FS_PATH_META)
  }
}