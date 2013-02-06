package org.aphreet.c3.snippet.groups.snippet

import net.liftweb.util.BindHelpers._
import net.liftweb.http.S
import net.liftweb.common.{Failure, Full}
import net.liftweb.util.{PassThru, CssSel}
import xml.NodeSeq

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
class GroupTabMenu {

  val tabs: String => List[(String, GroupTab)] =
    (id: String) => List("about" -> AboutTab(id),
                        "files" -> FilesTab(id),
                        "messages" -> MessagesTab(id),
                        //"wiki" -> WikiTab(id),
                        "members" -> MembersTab(id))

  def render: CssSel = {
    val activeTab = S.attr("active")
    val groupId = S.attr("group_id")

    def tabMenu(id: String, active: String) = {
      "li" #> tabs(id).map{
        case (key, tab) =>
          "a *" #> tab.name &
          "a [href]" #> tab.path &
          "li [class+]" #> (if(key == active) "active" else "")
      }
    }

    val cssSel = for{
      active <- activeTab ?~ "Active tab is undefined!"
      groupId <- groupId ?~ "Current group id is undefined"
    } yield tabMenu(groupId, active)

    lazy val empty = "* *" #> NodeSeq.Empty

    cssSel match {
      case Full(c) => c
      case f: Failure => S.error(f.msg); empty
      case _ => empty
    }

  }
}

// consider to move tab definitions to specific Group Locs -- as paths depend on locs
sealed abstract class GroupTab(val name: String, val path: String)
case class AboutTab(groupId: String) extends GroupTab("About", "/groups/" + groupId)
case class FilesTab(groupId: String) extends GroupTab("Files", "/groups/" + groupId + "/files/")
case class MessagesTab(groupId: String) extends GroupTab("Messages", "/groups/" + groupId + "/messages")
//case class WikiTab(groupId: String) extends GroupTab("Wiki", "/groups/" + groupId + "/wiki")
case class MembersTab(groupId: String) extends GroupTab("Members", "/groups/" + groupId + "/members")