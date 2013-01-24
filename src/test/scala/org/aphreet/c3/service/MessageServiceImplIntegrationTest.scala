package org.aphreet.c3.service

import org.aphreet.c3.service.impl.{GroupServiceImpl, MessageStorageServiceImpl}
import org.aphreet.c3.model.{Message, Group}
import junit.framework.{Assert, TestCase}
import net.liftweb.mapper._
import net.liftweb.db.StandardDBVendor
import net.liftweb.util.Props
import net.liftweb.http.LiftRules
import net.liftweb.common.Logger
import org.junit
import java.util

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

@junit.Ignore
class MessageServiceImplIntegrationTest extends TestCase {

  private final val logger = Logger(classOf[MessageServiceImplIntegrationTest])

  private val service = new MessageStorageServiceImpl
  private val groupService = new GroupServiceImpl

  private var group: Group = null

  override def setUp(){
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr
            "jdbc:h2:lift_test_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }
    group = groupService.createGroup(Group.create.name("TestGroup2"), Nil).open_!
  }

  override def tearDown(){
    groupService.removeGroup(group)
  }

  def testMessageCreation(){

    val msg1 = Message(group.id.is.toString, "1", "This is test message!", util.UUID.randomUUID().toString, Nil)
    val msg2 = Message(group.id.is.toString, "1", "If u don't like C3 get lost", util.UUID.randomUUID().toString, Nil)
    val msg3 = Message(group.id.is.toString, "1", "This is another testing message!", util.UUID.randomUUID().toString, Nil)

    logger.debug("Saving messages of group " + msg1.group)

    service.save(msg1)
    service.save(msg2)
    service.save(msg3)

    logger.debug("Messages are saved. Retrieving back from storage...")

    val result = service.findAll(group).toList.reverse

    logger.debug("Retrieved messages: " + result)

    Assert.assertEquals(3, result.size)
    List(msg1, msg2, msg3).foreach { msg =>
      Assert.assertTrue(result.map(_.content).contains(msg.content))
      Assert.assertTrue(result.map(_.author).contains(msg.author))
      Assert.assertTrue(result.map(_.group).contains(msg.group))
    }

  }

  def testMessageDeletion(){
    // Storing some messages to delete them further
    service.save(Message(group.id.is.toString, "1", "This is test message!", util.UUID.randomUUID().toString, List()))
    service.save(Message(group.id.is.toString, "1", "If u don't like C3 get lost", util.UUID.randomUUID().toString, List()))
    service.save(Message(group.id.is.toString, "1", "This is another testing message!", util.UUID.randomUUID().toString, List()))

    val messages = service.findAll(group)

    logger.debug("Retrieved messages: " + messages)

    Assert.assertTrue(messages.size > 0)

    messages.foreach(service.delete(_))

    val restMessages = service.findAll(group)

    println(restMessages)
    Assert.assertTrue(restMessages.isEmpty)
  }

}