package org.aphreet.c3.service.metadata

import akka.actor.Actor
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.service.metadata.MetadataServiceProtocol._
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._
import net.liftweb.common.{Box, Failure, Full}
import org.aphreet.c3.service.notifications.{FileMetaProcessedMsg, NotificationManager}
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.CreateNotification
import org.aphreet.c3.model.{Group, User}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers._

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
class MetadataServiceWorker extends Actor with C3Loggable{
  def receive = {
    case ProcessC3Resource(res) => {
      logger.debug("Received a C3 resource " + res.address + " for processing.")
      if (res.metadata.get(S4_PROCESSED_FLAG_META).isDefined){
        res.metadata.get(TAGS_META) match {
          case Some(tags) => {
            logger.debug("Got tags for resource " + res.address + ": " + tags)
            val ownerIdBox = Box(res.metadata.get(OWNER_ID_META).flatMap(asLong(_)))
            val groupIdBox = Box(res.metadata.get(GROUP_ID_META).flatMap(asLong(_)))

            (for {
              ownerId <- ownerIdBox ?~ "No owner id in metadata provided"
              groupId <- groupIdBox ?~ "No group id in metadata provided"
              owner <- User.find(By(User.id, ownerId)) ?~ ("User with id " + ownerId + " is not found!")
              group <- Group.find(By(Group.id, groupId)) ?~ ("Group with id " + groupId + " is not found!")
            } yield (owner,group)) match {
              case Full((owner: User, group: Group)) => {
                val tagsList = tags.split(TAGS_SEPARATOR).map(_.trim)

                // TODO extract file data and send notification
                //NotificationManager ! CreateNotification(FileMetaProcessedMsg(null, owner))
                res.update(res.metadata - S4_PROCESSED_FLAG_META) // updating metadata with s4-meta flag removed
              }
              case Failure(msg, _, _) => logger.error(msg)
              case _ => logger.error("Something unexpected happen.")
            }
          }
          case _ => logger.debug("Resource " + res.address + " has no tags assigned. Skipping...")
        }
      } else {
        logger.error("C3 resource has no " + S4_PROCESSED_FLAG_META + " flag in metadata. Skipping...")
      }
    }
  }
}