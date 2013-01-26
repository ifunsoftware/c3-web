package org.aphreet.c3.lib {

import net.liftweb._
import http._
import util._
import org.aphreet.c3.apiaccess.C3
import org.aphreet.c3.service.groups.impl.GroupServiceImpl
import org.aphreet.c3.service.groups.messages.impl.MessageStorageServiceImpl
import org.aphreet.c3.service.groups.wiki.impl.WikiServiceImpl
import org.aphreet.c3.service.notifications.impl.NotificationServiceImpl

/**
 * A factory for generating new instances of Date.  You can create
 * factories for each kind of thing you want to vend in your application.
 * An example is a payment gateway.  You can change the default implementation,
 * or override the default implementation on a session, request or current call
 * stack basis.
 */
object DependencyFactory extends Factory {

  implicit object time extends FactoryMaker(Helpers.now _)
  
  implicit object c3 extends FactoryMaker(C3.apply _ )

  implicit object wikiService extends FactoryMaker(WikiServiceImpl.create _)

  implicit object messageService extends FactoryMaker(MessageStorageServiceImpl.apply _)

  implicit object groupService extends FactoryMaker(GroupServiceImpl.create _)

  implicit object notificationService extends FactoryMaker(NotificationServiceImpl.create _)

  /**
   * objects in Scala are lazily created.  The init()
   * method creates a List of all the objects.  This
   * results in all the objects getting initialized and
   * registering their types with the dependency injector
   */
  private def init() {
    List(time, c3, wikiService, messageService, groupService, notificationService)
  }

  init()
}

}
