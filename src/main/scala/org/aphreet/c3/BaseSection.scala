package org.aphreet.c3

import net.liftweb.sitemap.{Menu, Loc}

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

abstract class Section {
  val menus: List[Menu]
  val snippetsLocPath: String = currentPackage

  def currentPackage: String = getClass.getPackage.getName
}

object BaseSection extends Section {
  override lazy val menus: List[Menu] = List()
}