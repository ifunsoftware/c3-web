package org.aphreet.c3
package service.groups.impl

import net.liftweb.common.{ Box, Full, Empty, Failure }
import net.liftweb.mapper.By

import com.ifunsoftware.c3.access.fs.{ C3File, C3Directory }
import com.ifunsoftware.c3.access.C3System
import com.ifunsoftware.c3.access.C3System._

import org.aphreet.c3.lib.metadata.Metadata
import org.aphreet.c3.lib.metadata.Metadata._
import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.util.{ C3Loggable, C3Exception }
import org.aphreet.c3.model.{ UserGroup, Group, User }
import org.aphreet.c3.service.groups.GroupService
import org.aphreet.c3.service.notifications.AddedToGroupMsg
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.CreateNotification
import org.aphreet.c3.lib.NotificationManagerRef

class GroupServiceImpl extends GroupService with C3Loggable {

  lazy val c3 = inject[C3System].open_!

  lazy val notificationManager = inject[NotificationManagerRef].open_!.actorRef

  override def createGroup(newGroup: Group, members: Iterable[User], tags: String, description: String): Box[Group] = {
    val metadata: Map[String, String] = Map(
      TAGS_META -> tags.split(",").map(_.trim).mkString(","),
      DESCRIPTION_META -> description)

    val group = newGroup.saveMe()

    try {
      createGroupMapping(group.id.is.toString, metadata, group.owner)
      group.owner.foreach(owner => UserGroup.join(owner, group))
      addUsersToGroup(group, members)
      Full(group)
    } catch {
      case e: Exception =>
        logger.error("Couldn't create group C3 FS mapping: " + newGroup.name.is)
        group.delete_! // rollback
        Failure("Couldn't create group C3 FS mapping", Full(e), Empty)
    }
  }

  override def createGroup(newGroup: Group, tags: String, description: String): Box[Group] = {
    createGroup(newGroup, List(), tags, description)
  }

  override def removeGroup(group: Group): Boolean = {
    try {
      removeGroupMapping(group.id.is.toString)
    } catch {
      case e: Exception =>
        logger.error("Error while removing group mapping from C3: " + e.getMessage, e)
        false
    }
    group.delete_!
  }

  override def removeUserFromGroup(group: Group, user: User) = {
    try {
      UserGroup.findAll(By(UserGroup.user, user), By(UserGroup.group, group)).foreach(_.delete_!)
    } catch {
      case e: Exception =>
        logger.error("Error while removing user" + e.getMessage, e)
        false
    }
    true
  }

  override def addUsersToGroup(group: Group, members: Iterable[User]): Iterable[Box[User]] = {
    members.map { member: User =>
      if (UserGroup.findAll(By(UserGroup.user, member), By(UserGroup.group, group)).isEmpty) {
        UserGroup.join(member, group)
        UserGroup.isApproved(true)
        notificationManager ! CreateNotification(AddedToGroupMsg(group = group, recipientId = member.id.is))
        Full(member)
      } else Failure("User " + member.email + " is already a member of this group!")
    }
  }

  override def addUsersToApproveListGroup(group: Group, members: Iterable[User]): Iterable[Box[User]] = {
    members.map { member: User =>
      if (UserGroup.findAll(By(UserGroup.user, member), By(UserGroup.group, group)).isEmpty) {
        UserGroup.join(member, group)
        Full(member)
      } else Failure("User " + member.email + " is already a member of this group!")
    }
  }

  override def approveUsersToGroup(group: Group, members: Iterable[User]): Iterable[Box[User]] = {
    members.map { member: User =>
      val userGroups = UserGroup.findAll(By(UserGroup.user, member), By(UserGroup.group, group))
      if (!userGroups.isEmpty) {
        userGroups.map { userGroup: UserGroup =>
          userGroup.isApproved(true).save()
          notificationManager ! CreateNotification(AddedToGroupMsg(group = group, recipientId = member.id.is))
        }
        Full(member)
      } else Failure("User " + member.email + " is not a member of this group!")
    }
  }

  private def createGroupMapping(groupId: String, metadata: Map[String, String], owner: Box[User]) {
    val root = c3.getFile("/").asDirectory

    val defaultMeta: Map[String, String] = Map(Metadata.GROUP_ID_META -> groupId) ++
      owner.map(u => Map[String, String](Metadata.OWNER_ID_META -> u.id.is.toString)).getOrElse(Map())

    val metadataGroup: Map[String, String] = defaultMeta ++ metadata

    root.createDirectory(groupId, metadataGroup)

    root.getChild(groupId) match {
      case Some(node) =>
        val dir = node.asDirectory
        dir.createDirectory("files", metadataGroup)
        dir.createDirectory("messages", metadataGroup)
      case None => throw new C3Exception("Failed to create directory for group " + groupId)
    }
  }

  private def removeGroupMapping(name: String) {
    val root = c3.getFile("/").asDirectory

    def removeDirectory(dir: C3Directory) {
      for (child <- dir.children()) {
        child match {
          case d: C3Directory =>
            logger.debug("Removing directory " + d.fullname + " from group " + name)
            removeDirectory(d)

          case f: C3File =>
            logger.debug("Removing file " + f.fullname + " from group " + name)
            c3.deleteFile(f.fullname)

        }
      }
      c3.deleteFile(dir.fullname)
    }

    root.getChild(name).filter(_.isDirectory) match {
      case Some(groupDir) =>
        removeDirectory(groupDir.asDirectory)

      case _ => throw new C3Exception("Group directory is not found!")
    }
  }
}

object GroupServiceImpl {

  lazy val service = new GroupServiceImpl

  def apply: GroupService = service
}

