package org.aphreet.c3.service.notifications

import org.aphreet.c3.model.{Group, Notification, NotificationType, User}
import org.aphreet.c3.model.{Group, User, NotificationType}
import xml.NodeSeq
import com.ifunsoftware.c3.access.fs.C3File
import net.liftweb.http.S
import net.liftweb.util.Helpers
import Helpers._

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
trait NotificationService {
  def sendNotification(notifyMsg: NotifyMsg)

  def getNotificationsForUser(recipient: User): List[Notification]

  def markAsRead(notification: Notification): Notification
}

abstract sealed class NotifyMsg {
  val recipient: User
  val title: String
  val message: NodeSeq
  val notifyType: NotificationType.Value
}

case class AddedToGroupMsg(group: Group, recipient: User) extends NotifyMsg {
  val title: String = TemplateManager.addedGroupTitle(group.name.is)
  val message: NodeSeq = TemplateManager.addedGroupTemplate(group.name.is, group.createLink)
  val notifyType: NotificationType.Value = NotificationType.AddedToGroup
}

case class FileMetaProcessedMsg(file: C3File, recipient: User) extends NotifyMsg {
  val title: String = TemplateManager.fileMetaProcessedTitle(file.name)
  val message: NodeSeq = TemplateManager.fileMetaProcessedTemplate(file.name, file.fullname)
  val notifyType: NotificationType.Value = NotificationType.FileMetaUpdated
}

object TemplateManager{
  private val addedGroupTemplatePath: List[String] = List("templates", "_added_to_group_message_template")
  private val fileMetaProcessedTemplatePath: List[String] = List("templates", "_file_meta_processed_msg_template")

  private val addedGroupMessageTemplate: NodeSeq = S.runTemplate(addedGroupTemplatePath).getOrElse(<div>Template is not found</div>)
  private val fileMetaProcessedMessageTemplate: NodeSeq = S.runTemplate(fileMetaProcessedTemplatePath).getOrElse(<div>Template is not found</div>)

  private val addedGroupMessageTitle: String = S.?("added.to.group.msg.title")
  private val fileMetaProcessedMessageTitle: String = S.?("file.meta.processed.msg.title")

  def addedGroupTitle(groupName: String) = addedGroupMessageTitle.replace("{group.name}", groupName)

  def fileMetaProcessedTitle(fileName: String) = fileMetaProcessedMessageTitle.replace("{file.name}", fileName)

  def addedGroupTemplate(groupName: String, groupLink: String): NodeSeq = {
    (".group_name *" #> groupName & ".group_link [href]" #> groupLink).apply(addedGroupMessageTemplate)
  }

  def fileMetaProcessedTemplate(fileName: String, fileLink: String): NodeSeq = {
    (".file_name *" #> fileName & ".file_link [href]" #> fileLink).apply(fileMetaProcessedMessageTemplate)
  }
}
