package org.aphreet.c3.snippet.notifications

import org.aphreet.c3.Section
import net.liftweb.sitemap.Menu
import snippet.NotificationPage

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
object NotificationsSection extends Section{
  override lazy val menus: List[Menu] = List(Menu(NotificationPage))
}