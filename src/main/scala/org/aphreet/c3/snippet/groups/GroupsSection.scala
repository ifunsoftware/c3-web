package org.aphreet.c3.snippet.groups

import net.liftweb.sitemap.{Menu, Loc}
import org.aphreet.c3.Section
import snippet.{GroupPageMembers, GroupPageFiles, GroupPageMessages, GroupPage}

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
object GroupsSection extends Section{
  override lazy val menus: List[Menu] = List(Menu(GroupPage, Menu(GroupPageMessages), Menu(GroupPageFiles), Menu(GroupPageMembers)))
}