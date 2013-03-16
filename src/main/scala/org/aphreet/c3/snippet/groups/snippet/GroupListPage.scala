package org.aphreet.c3.snippet.groups.snippet

import com.ifunsoftware.c3.access.C3System
import net.liftweb.common.{Failure, Full, Empty, Logger}
import xml.{Text, NodeSeq}
import org.aphreet.c3.model._
import net.liftweb.http.{RequestVar, S, SHtml}
import net.liftweb.http.js.{JsCmds, JsCmd}
import org.aphreet.c3.service.groups.GroupService
import net.liftweb.util.Helpers._
import net.liftweb.util.BindHelpers._
import org.aphreet.c3.lib.DependencyFactory
/**
 * @author Serjk (mailto: serjk91@gmail.com)
 */
class GroupListPage {

  lazy val c3 = DependencyFactory.inject[C3System].open_!

  lazy val groupService = DependencyFactory.inject[GroupService].open_!

  lazy val logger = Logger(classOf[GroupListPage])

  def list = {
    val groupList =if(User.currentUser.open_!.superUser.is) Group.findAll().toList else User.currentUser.open_!.groups.toList

    ".container_groups" #> groupList.map{ group:Group => {

      def deleteGroup(): JsCmd = {
        if(groupService.removeGroup(group)){
          JsCmds.Replace(group.id.is.toString, NodeSeq.Empty)
        } else JsCmds.Alert("Group is not removed! Please check logs for details")
      }
      if (User.currentUser.open_!.superUser){
        ".container_groups [id]"#> group.id.is &
          ".container_groups *" #>
            ((n: NodeSeq) => SHtml.ajaxForm(
              ("a *" #> group.name.is &
                "a [href]" #> ("/groups/"+group.id) andThen
                "* *" #> SHtml.memoize(f => f ++ SHtml.hidden(deleteGroup _))).apply(n)
            ))
      } else{
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
    var tags = ""

    def saveMe(){
      newGroup.validate match {
        case Nil => {
          newGroup = newGroup.owner(User.currentUser.open_!)
          groupService.createGroup(newGroup,tags) match {
            case Full(g) => {
              if (sameCategory != "false"){
                val newCategory = Category.create.name(g.name.is).linkedGroup(g)
                newCategory.validate match {
                  case Nil => {
                    newCategory.save()
                    S.notice("Added group and category: " + g.name + " is added")
                  }
                  case xs =>
                    xs.foreach(f => S.error(f.msg))
                }
              } else{
                S.notice("Added group: " + g.name)
              }
            }
            case Failure(msg, _, _) => S.error(msg)
            case _ => S.error("Group is not created")
          }
        }
        case xs =>
          xs.foreach(f => S.error(f.msg))
      }
    }
    "name=name" #> SHtml.onSubmit(newGroup.name(_))&
      "name=description" #> SHtml.onSubmit(newGroup.description(_))&
      "name=sameCategory" #> SHtml.onSubmit(sameCategory = _) &
      "name=tags_edit" #> SHtml.onSubmit(tags = _) &
      "type=submit" #> SHtml.onSubmitUnit(saveMe)
  }
}
