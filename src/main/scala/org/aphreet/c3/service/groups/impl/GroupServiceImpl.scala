package org.aphreet.c3.service.groups.impl

import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.util.{C3Loggable, C3Exception}
import org.aphreet.c3.model.{UserGroup, Group, User}
import org.aphreet.c3.service.groups.GroupService
import org.aphreet.c3.service.notifications.{AddedToGroupMsg, NotificationManager}
import com.ifunsoftware.c3.access.C3System
import net.liftweb.common.{Box, Empty, Full, Failure}
import com.ifunsoftware.c3.access.fs.{C3File, C3Directory}
import net.liftweb.mapper.By
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.CreateNotification

class GroupServiceImpl extends GroupService with C3Loggable{

  lazy val c3 = inject[C3System].open_!

  override def createGroup(newGroup: Group, members: Iterable[User]): Box[Group] = {
    val group = newGroup.saveMe()
    try {
      createGroupMapping(group.id.is.toString)
    } catch {
      case e: C3Exception => {
        group.delete_! // rollback
        Failure("Couldn't create group C3 FS mapping", Full(e), Empty)
      }
    }
    group.owner.foreach(owner => UserGroup.join(owner, group))
    addUsersToGroup(group,members)
    Full(group)
  }

  override def removeGroup(group: Group): Boolean = {
    try {
      removeGroupMapping(group.id.is.toString)
    } catch {
      case e: C3Exception => {
        logger.error("Error while removing group mapping from C3: " + e.getMessage, e)
        false
      }
    }

    group.delete_!
  }

  override  def removeUserFromGroup(group:Group, user:User) = {
    try{
      UserGroup.findAll(By(UserGroup.user, user),By(UserGroup.group, group)).foreach(_.delete_!)
    } catch {
      case e: C3Exception => {
        logger.error("Error while removing user" + e.getMessage, e)
        false
      }
    }
    true
  }


  override def addUsersToGroup(group: Group, members: Iterable[User]): Iterable[Box[User]] = {
    for {
      member <- members
    } yield {
      if (UserGroup.findAll(By(UserGroup.user, member), By(UserGroup.group, group)).isEmpty){
        UserGroup.join(member, group)
        NotificationManager ! CreateNotification(AddedToGroupMsg(group = group, recipient = member))
        Full(member)
      } else Failure("User " + member.email + " is already a member of this group!")
    }
  }

  private def createGroupMapping(groupId: String){
    val root = c3.getFile("/").asDirectory

    root.createDirectory(groupId)

    root.getChild(groupId) match {
      case Some(node) => val dir = node.asDirectory
      dir.createDirectory("files")
      dir.createDirectory("messages")
      dir.createDirectory("wiki")
      case None => throw new C3Exception("Failed to create directory for group " + groupId)
    }
  }

  private def removeGroupMapping(name: String){
    val root = c3.getFile("/").asDirectory

    def removeDirectory(dir: C3Directory) {
      for(child <- dir.children()){
        child match {
          case d: C3Directory => {
            logger.debug("Removing directory " + d.fullname + " from group " + name)
            removeDirectory(d)
          }
          case f: C3File => {
            logger.debug("Removing file " + f.fullname + " from group " + name)
            c3.deleteFile(f.fullname)
          }
        }
      }
      c3.deleteFile(dir.fullname)
    }

    root.getChild(name).filter(_.isDirectory) match {
      case Some(groupDir) => {
        removeDirectory(groupDir.asDirectory)
      }
      case _ => throw new C3Exception("Group directory is not found!")
    }
  }
}

object GroupServiceImpl{
  def create: GroupService = new GroupServiceImpl
}

