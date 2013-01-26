package org.aphreet.c3.service.notifications

import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.model.Notification
import net.liftweb.actor.LiftActor
import org.aphreet.c3.service.notifications.NotificationManagerProtocol.{CreateNotification, MarkAsRead}

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
object NotificationManager extends LiftActor with C3Loggable{
  private val notificationService = inject[NotificationService].open_!

  def messageHandler = {
    case CreateNotification(notifyMessage) => notificationService.sendNotification(notifyMessage)
    case MarkAsRead(notification) => notificationService.markAsRead(notification)

    case msg => logger.error("Unkhown message is received: " + msg) // default handling
  }

}

object NotificationManagerProtocol {
  abstract sealed class NotificationManagerCmd
  case class CreateNotification(notifyMessage: NotifyMsg)
  case class MarkAsRead(notification: Notification)
}

