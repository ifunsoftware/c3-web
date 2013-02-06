package org.aphreet.c3.snippet.notifications.snippet

import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.model.{Notification, User}
import net.liftweb.util.BindHelpers._
import xml.{NodeSeq, Text}
import org.aphreet.c3.service.notifications.NotificationService
import org.aphreet.c3.util.helpers.DateTimeHelpers
import net.liftweb.util.PassThru

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
class NotificationListPage {
  private val notificationService = inject[NotificationService].open_!

  def list = {
    val currentUser = User.currentUser
    val notifications: List[Notification] = currentUser.map(notificationService.getNotificationsForUser _).getOrElse(Nil)

    def asNotViewed(xml: NodeSeq): NodeSeq = {
      <strong>{xml}</strong>
    }

    if (notifications.isEmpty)
      ".notification" #> Text("You have no notifications yet.")
    else
      ".notification *" #> notifications.reverse.map { notification =>
        ".created *" #> DateTimeHelpers.todayTimeOrPastDate(notification.created.is) &
        ".link [href]" #> notification.createLink &
        ".title *" #> notification.title.is andThen
        (notification.isRead.is match {
          case false => {
            ".title *" #> asNotViewed _ &
            ".created *" #> asNotViewed _ &
            ".source *" #> asNotViewed _
          }
          case true => PassThru
        })
      }
  }
}