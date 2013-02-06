package org.aphreet.c3.comet

import net.liftweb.http.ListenerManager
import net.liftweb.actor.LiftActor
import net.liftweb.common.Logger
import org.aphreet.c3.model.{Group, User, Message}
import org.aphreet.c3.lib.DependencyFactory._
import java.util
import org.aphreet.c3.service.groups.messages.MessageStorageService

/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 *         iFunSoftware
 */
class MessageServer(val group: Group) extends LiftActor with ListenerManager {

  private val logger = Logger(classOf[MessageServer])
  private lazy val msgService = inject[MessageStorageService].open_!

  override def lowPriority = {
    case MessageServerMsg(user, messageGroup, content, tags) if content.length > 0 =>
      val msg = Message(group.id.is.toString, user.id.is.toString, content, util.UUID.randomUUID().toString, tags)
      logger.debug("Received a message: " + msg + ". Saving...")
      msgService.save(msg)
      updateListeners()
    case _ =>
  }

  def createUpdate = MessageServerUpdate(msgService.findAll(group).take(15).toList)

}

object MessageServerFactory{

  private val logger = Logger("MessageServerFactory")

  // message servers simple cache =)
  private var messageServers = Map[String, MessageServer]()

  def apply(group: Group): MessageServer = {
    messageServers.get(group.name.is) match {
      case Some(ms) => ms
      case _ => addMessageServer(group)
    }
  }

  private def addMessageServer(group: Group): MessageServer = {
    logger.info("Creating new message server for group " + group.name.is)
    val ms = new MessageServer(group)
    val newMap = messageServers + (group.name.is -> ms)
    messageServers.synchronized{
      messageServers = newMap
    }
    ms
  }
}

case class MessageServerMsg(user: User, group: Group, msg: String, tags: List[String])
case class MessageServerUpdate(msgs: List[Message])

