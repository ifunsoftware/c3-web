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

import net.liftweb.common.{ Box, Failure, Full }
import net.liftweb.mapper.By

import akka.actor.{ ActorRef, Actor }

import com.ifunsoftware.c3.access.C3System
import com.ifunsoftware.c3.access.fs.C3File

import org.aphreet.c3.service.notifications.FileMetaProcessedMsg
import org.aphreet.c3.lib.metadata.Metadata._
import org.aphreet.c3.util.{ WebMetadata, C3Loggable }
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.CreateNotification
import org.aphreet.c3.model.{ Group, User }
import MetadataServiceProtocol._


class MetadataServiceWorker(c3system: C3System, notificationManager: ActorRef) extends Actor with C3Loggable {

  def receive = {
    case ProcessC3Resource(res) =>
      val resMeta = WebMetadata(res)

      if (resMeta.isProcessedS4 && hasTags(resMeta)) {
        logger.debug(s"Received a C3 resource ${res.address} processed by S4. Tags: ${resMeta.tags}")
        processUpdatedResource(resMeta)
      } else {
        logger.warn("C3 resource is not processed by S4. Skipped.")
      }
  }

  private[this] def processUpdatedResource[T](meta: WebMetadata): Unit = {
    fetchUserAndGroup(meta) match {
      case Full((owner: User, group: Group)) =>
        // TODO we do 2 requests to C3... make it in 1
        fsPath(meta).map(c3system.getFile) match {
          case Some(f: C3File) =>
            notificationManager ! CreateNotification(FileMetaProcessedMsg(f, owner.id.is))
            meta.removeProcessedS4Flag() // updating metadata with s4-meta flag removed

          case _ => logger.error(s"Resource ${meta.res.address} is not found in virtual FS. Skipped.")
        }

      case Failure(msg, _, _) => logger.debug(msg)

      case _                  => logger.error("Something unexpected happened")
    }
  }

  private[this] def fetchUserAndGroup[T](meta: WebMetadata): Box[(User, Group)] =
    for {
      ownerId <- meta.ownerId ?~ "No owner id in metadata provided"
      groupId <- meta.groupId ?~ "No group id in metadata provided"
      owner <- User.find(By(User.id, ownerId))   ?~ s"User with id <$ownerId> is not found!"
      group <- Group.find(By(Group.id, groupId)) ?~ s"Group with id <$groupId> is not found!"
    } yield (owner, group)

  private[this] def fsPath(meta: WebMetadata): Option[String] =
    c3system.getResource(meta.res.address, List(FS_PATH_META)).systemMetadata.get(FS_PATH_META)

  private[this] def hasTags(meta: WebMetadata): Boolean = !meta.tags.isEmpty
}