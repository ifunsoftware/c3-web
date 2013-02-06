package org.aphreet.c3.snippet.notifications.snippet

import org.aphreet.c3.model.Notification
import org.aphreet.c3.snippet.notifications.NotificationPageData
import org.aphreet.c3.loc.ItemRewriteLoc
import xml.{NodeSeq, Text}
import net.liftweb.sitemap.Loc.{Link, LinkText}
import net.liftweb.common.{Full, Box}
import net.liftweb.util.BindHelpers._
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.service.notifications.NotificationManager
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.MarkAsRead
import org.aphreet.c3.util.helpers.DateTimeHelpers


/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
object NotificationPage extends ItemRewriteLoc[Notification, NotificationPageData] {
  override val name = "Notification"
  override def title = Text(currentValue.map(_.notification.title.is).openOr("Notification"))
  override def text = new LinkText[NotificationPageData](text = v => Text(v.notification.id.is.toString))

  override val pathPrefix = "notifications" :: Nil
  override lazy val pathList = pathPrefix ++ List("index")
  override def link = new Link[NotificationPageData](pathList){
    override def pathList(value: NotificationPageData): List[String] = pathPrefix ::: value.notification.id.is.toString :: Nil
  }
  override def getItem(id: String) = Notification.find(id)
  override def wrapItem(userBox: Box[Notification]) = userBox.map(NotificationPageData(_))
  override def canonicalUrl(data: NotificationPageData) = {
    Full((pathPrefix:::List(data.notification.id.is.toString)).mkString("/","/",""))
  }
}

class NotificationPage(data: NotificationPageData) extends C3Loggable{

  def view = {
    val notification = data.notification

    if (!notification.isRead.is){
      NotificationManager ! MarkAsRead(notification)
    }
    val time = DateTimeHelpers.todayTimeOrPastDate(data.notification.created)
    ".notification_title *" #> data.notification.title.is &
    ".notification_created *" #> time &
    ".notification_body *" #> data.notification.body.toXml
  }
}