package org.aphreet.c3.util.helpers

import java.util
import net.liftweb.util.TimeHelpers._
import java.text.SimpleDateFormat

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
object DateTimeHelpers {

  private val todayFormatter = new SimpleDateFormat("HH:mm")
  private val customDateFormatter = new SimpleDateFormat("dd MMM yyyy")

  def todayTimeOrPastDate(date: util.Date): String = {
    if (millisToDays(date.getTime) == millisToDays(now.getTime)){ // today
      todayFormatter.format(date)
    } else  // not today
      customDateFormatter.format(date)
  }
}