package org.aphreet.c3.service.groups.messages.impl

import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.model.{Group, Message}
import org.aphreet.c3.lib.DependencyFactory._
import MessageStorageServiceImpl._
import org.aphreet.c3.lib.metadata.Metadata._
import net.liftweb.util.Helpers
import Helpers._
import org.aphreet.c3.service.groups.messages.{MessageStorageException, MessageStorageService}
import com.ifunsoftware.c3.access.{DataStream, C3System}
import net.liftweb.common.Box
import com.ifunsoftware.c3.access.fs.C3Directory

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
class MessageStorageServiceImpl extends MessageStorageService with C3Loggable{

  private val metaTags = Set(MSG_CREATOR_META)
  private val MSG_FILE_PREFIX = "msg-"

  lazy val c3 = inject[C3System].open_!

  @throws(classOf[MessageStorageException])
  override def findAll(group: Group): Traversable[Message] = {
    val messagesRoot = getGroupMessagesRoot(group)

    if(messagesRoot.isEmpty){
      warn("Message root of group " + group.name.is + " is not found!")
      List()
    }
    else {
      val messages = for {
        root <- messagesRoot.toList
        file <- root.children(embedChildrenData = true, embedChildMetaData = metaTags).filter(!_.isDirectory).map(_.asFile)
      } yield {
        val md = file. metadata
        Message(group.id.is.toString,
          Box(md.get(MSG_CREATOR_META)).openOr("N/A"),
          file.versions.last.date,
          file.versions.last.getData.readContentAsString,
          messageUUID(file.name))
      }

      messages.sortWith((cd1, cd2) => cd1.creationDate.after(cd2.creationDate))
    }
  }

  @throws(classOf[MessageStorageException])
  override def save(msg: Message): Box[Message] = {
    for{
        group <- msg.group ?~ "Group message belongs to is not defined!"
        root <- getGroupMessagesRoot(group)
    } yield {
      val tagsMap = buildTagsMap(CreatorTag(msg.author.map(_.id.is.toString).openOr("N/A")))
      root.createFile(messageFileName(msg), tagsMap, DataStream(msg.content))
      msg
    }
  }

  @throws(classOf[MessageStorageException])
  override def delete(msg: Message): Boolean = {
    val result = for{
      group <- msg.group
      root <- getGroupMessagesRoot(group)
      msgFileName = messageFileName(msg)
      file <- root.getChild(msgFileName)
    } yield {
      c3.deleteFile(file.fullname)
      msg
    }

    !result.isEmpty
  }

  protected def getGroupMessagesRoot(group: Group): Box[C3Directory] = {
    tryo{
      val path = "/" + group.id.is.toString + "/" + GROUP_MESSAGES_ROOT + "/"
      c3.getFile(path)
    }.filter(_.isDirectory).map(_.asDirectory)
  }

  protected def buildTagsMap(tags: MsgMDTag*): Map[String, String] = {
    tags.map(t => t.name -> t.value).toMap
  }

  protected def messageFileName(msg: Message) = MSG_FILE_PREFIX + msg.uuid

  protected def messageUUID(fileName: String) = fileName.replace(MSG_FILE_PREFIX, "")
}

object MessageStorageServiceImpl{
  val GROUP_MESSAGES_ROOT = "messages"
  def apply: MessageStorageService = new MessageStorageServiceImpl
}

sealed abstract class MsgMDTag(val name: String, val value: String)

final case class CreatorTag(creator: String) extends MsgMDTag(name = MSG_CREATOR_META, value = creator)
