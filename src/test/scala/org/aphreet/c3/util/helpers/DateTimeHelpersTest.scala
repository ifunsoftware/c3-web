package org.aphreet.c3.util.helpers

import org.specs._
import net.liftweb.util.TimeHelpers
import DateTimeHelpers._
import java.text.SimpleDateFormat

/**
 * @author Dmitry Ivanov (dmitry.ivanov@reltio.com)
 *         Reltio, Inc.
 */
object DateTimeHelpersTest extends Specification{

  "'DateTimeHelpers.todayTimeOrPastDate' should format a date for current day with pattern like '22:04'" in {
     val currentDayDate = TimeHelpers.dayNow
     currentDayDate.setHours(22)
     currentDayDate.setMinutes(04)
     todayTimeOrPastDate(currentDayDate) must be equalTo("22:04")
  }

  "'DateTimeHelpers' should format a date for date of past or future days with pattern like '15 Jan 2013'" in {
    val someDate = new SimpleDateFormat("dd/MMM/yyyy").parse("22/Jan/2013")
    val someFutureDate = new SimpleDateFormat("dd/MMM/yyyy").parse("05/Feb/2050")

    todayTimeOrPastDate(someDate) must be equalTo("22 Jan 2013")
    todayTimeOrPastDate(someFutureDate) must be equalTo("05 Feb 2050")
  }
}