package org.aphreet.c3.snippet.approve.snippet

import net.liftweb.util.BindHelpers._
import net.liftweb.http.{ RequestVar, S }
import net.liftweb.common.{ Failure, Full }
import net.liftweb.util.{ PassThru, CssSel }
import xml.NodeSeq
import org.aphreet.c3.model.{ User, Group }

/**
 * @author Aleksey Tkachev (mailto: imsiral1@mail.ru)
 */

class AdminTabMenu {

  type GroupTabsFunc = () => List[(String, AdminTab)]

  object tabs extends RequestVar[GroupTabsFunc](defaultTabs)

  private def defaultTabs(): List[(String, AdminTab)] =
    List("users" -> UsersTab(),
      "groupApprove" -> GroupApproveTab(),
      "categories" -> CategoriesTab())
  def render: CssSel = {
    val activeTab = S.attr("active")

    def tabMenu(active: String) = {
      "li" #> tabs.get().map {
        case (key, tab) =>
          val iconClass = tab.name match {
            case "Approve Groups" => "icon-star"
            case "Categories"     => "icon-file"
            case "Users"          => "icon-comment"
            case _                => "icon-heart"
          }
          val activeClass = if (key == active) "active" else ""
          "span *" #> tab.name &
            "a [href]" #> tab.path &
            //".iconClass [class+]"#>  iconClass &
            ".btn-small [class+]" #> activeClass
      }
    }

    val cssSel = for {
      active <- activeTab ?~ "Active tab is undefined!"
    } yield tabMenu(active)

    lazy val empty = "* *" #> NodeSeq.Empty

    cssSel match {
      case Full(c)    => c
      case f: Failure => S.error(f.msg); empty
      case _          => empty
    }

  }
}

sealed abstract class AdminTab(val name: String, val path: String)
case class GroupApproveTab() extends AdminTab("Approve Groups", "/admin/group_admin")
case class CategoriesTab() extends AdminTab("Categories", "/admin/categories")
case class UsersTab() extends AdminTab("Users", "/admin")
