package org.aphreet.c3.snippet.groups.snippet

import com.ifunsoftware.c3.access.C3System
import org.aphreet.c3.lib.DependencyFactory._
import net.liftweb.common.{Empty, Logger}
import xml.{Text, NodeSeq}
import org.aphreet.c3.model._
import net.liftweb.http.{RequestVar, S, SHtml}
import net.liftweb.util.BindHelpers._
import net.liftweb.mapper.By
import net.liftweb.http.js.{JsCmds, JsCmd}
import org.aphreet.c3.snippet.{FileUploadDialog, CreateDirectoryDialog}
import net.liftweb.http.js.JsCmds.Alert
import net.liftweb.common.Full
import xml.Text
import org.aphreet.c3.service.groups.GroupService

/**
 * @author Serjk (mailto: serjk91@gmail.com)
 */
class GroupListPage {

  lazy val c3 = inject[C3System].open_!
  lazy val groupService = inject[GroupService].open_!
  lazy val logger = Logger(classOf[GroupListPage])

  def listUser = {
    val users = User.findAll().filter(_.id.is != User.currentUser.open_!.id.is)
    ".contUser" #> users.map( user =>{
      ".contUser *" #> user.shortName &
      ".contUser [value]" #> user.email
    })
  }

  def list = {
    val groupList = User.currentUser.open_!.groups.toList

    ".container_groups" #> groupList.map{ group:Group => {

      def deleteGroup(): JsCmd = {
        if(groupService.removeGroup(group)){
          JsCmds.Replace(group.id.is.toString, NodeSeq.Empty)
        } else JsCmds.Alert("Group is not removed! Please check logs for details")
      }
      if (User.currentUser.open_!.superUser)
      {
        ".container_groups [id]"#> group.id.is &
          ".container_groups *" #>
            ((n: NodeSeq) => SHtml.ajaxForm(
              ("a *" #> group.name.is &
                "a [href]" #> ("/groups/"+group.id) andThen
                "* *" #> SHtml.memoize(f => f ++ SHtml.hidden(deleteGroup _))).apply(n)
            ))
      }else{
        "a *" #> group.name.is &
        "a [href]" #> ("/groups/"+group.id) &
         ".delete_group" #> NodeSeq.Empty
      }

    }
    }
  }

  def add = {
    var newGroup = Group.create
    var sameCategory = ""
    var listUserEmails = ""

    def saveMe(){
      newGroup.validate match {
        case Nil => {
          val userEmails = listUserEmails.split('%')
          newGroup = newGroup.owner(User.currentUser.open_!)
          val members = userEmails.flatMap(User.findByEmail _)
          groupService.createGroup(newGroup, members)

          if (sameCategory != "false"){
            val newCategory = Category.create.name(newGroup.name.is).linkedGroup(newGroup)
            newCategory.validate match {
              case Nil => {
                newCategory.save()
                S.notice("Added group and category: " + newGroup.name + " is added")
              }
              case xs =>
                xs.foreach(f => S.error(f.msg))
            }
          } else{
            S.notice("Added group: " + newGroup.name)
          }
        }

        case xs =>
          xs.foreach(f => S.error(f.msg))
      }
    }
    "name=name" #> SHtml.onSubmit(newGroup.name(_))&
      "name=description" #> SHtml.onSubmit(newGroup.description(_))&
      "name=sameCategory" #> SHtml.onSubmit(sameCategory = _) &
      "name=listusers" #> SHtml.onSubmit(listUserEmails = _) &
      "type=submit" #> SHtml.onSubmitUnit(saveMe)
  }
}
