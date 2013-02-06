package org.aphreet.c3.util

import net.liftweb.common.Loggable

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
trait C3Loggable extends Loggable{
  def trace(msg: => AnyRef) { logger trace msg }

  def trace(msg: => AnyRef, t: Throwable) { logger.trace(msg, t) }

  def info(msg: => AnyRef) { logger info msg }

  def info(msg: => AnyRef, t: Throwable) { logger.info(msg, t) }

  def warn(msg: => AnyRef) { logger warn msg }

  def warn(msg: => AnyRef, t: Throwable) { logger.warn(msg, t) }

  def critical(msg: => AnyRef) { logger error msg }

  def critical(msg: => AnyRef, t: Throwable) { logger.error(msg, t) }
}