package org.aphreet.c3.util

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
class C3Exception(val msg: String, val cause: Throwable) extends Exception(msg, cause){

  def this() = this(null, null)

  def this(t: Throwable) = this(null, t)

  def this(msg: String) = this(msg, null)

}
