package org.aphreet.c3.service.groups.messages

import org.aphreet.c3.model.{User, Group, Message}
import java.util
import org.aphreet.c3.util.C3Exception
import net.liftweb.common.Box

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
trait MessageStorageService {

  @throws(classOf[MessageStorageException])
  def findAll(group: Group): Traversable[Message]

  @throws(classOf[MessageStorageException])
  def save(msg: Message): Box[Message]

  @throws(classOf[MessageStorageException])
  def delete(msg: Message): Boolean
}

class MessageStorageException extends C3Exception

