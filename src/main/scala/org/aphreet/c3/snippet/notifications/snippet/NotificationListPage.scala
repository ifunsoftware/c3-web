package org.aphreet.c3.snippet.notifications.snippet

import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.model.{Notification, User}
import net.liftweb.util.BindHelpers._
import xml.Text
import org.aphreet.c3.service.notifications.NotificationService

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
class NotificationListPage {
  private val notificationService = inject[NotificationService].open_!

  def list = {
    val currentUser = User.currentUser
    val notifications: List[Notification] = currentUser.map(notificationService.getNotificationsForUser _).getOrElse(Nil)

    if (notifications.isEmpty)
      ".notification" #> Text("You have no notifications yet.")
    else
      ".notification *" #> notifications.reverse.map { notification =>
        ".title [class+]" #> (notification.isRead.is match {
          case false => "is_not_read"
          case true => ""
        }) &
        ".title *" #> notification.title.is &
        ".link [href]" #> notification.createLink
      }
  }
}