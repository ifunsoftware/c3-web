package org.aphreet.c3.model

import net.liftweb.util.TimeHelpers
import net.liftweb.common.Box
import java.util

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

case class Message(groupId: String, authorId: String, creationDate: util.Date, content: String, uuid: String, tags: List[String] = List()) {
  lazy val author: Box[User] = User.find(authorId)
  lazy val group: Box[Group] = Group.find(groupId)

  override def toString = {
    val builder = new StringBuilder("Message{")
    builder.append("authorId=").append(authorId).
    append(", groupId=").append(groupId).
    append(", creationDate=").append(creationDate.getTime).
    append(", content=").append(content).
    append("}")

    builder.toString()
  }
}
object Message {
  def apply(groupId: String, authorId: String, content: String, uuid: String, tags: List[String]) =
    new Message(groupId, authorId, TimeHelpers.now, content, uuid, tags)
}