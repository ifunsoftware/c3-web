package org.aphreet.c3

import model._
import net.liftweb.mapper.Schemifier

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */

object DBSetup {
  def setup(){
    Schemifier.schemify(true, Schemifier.infoF _, User, Group, Category, Tag, UserGroup, Notification)
  }
}