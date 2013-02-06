package org.aphreet.c3.snippet.groups.snippet

import org.aphreet.c3.model.{User, UserGroup, Group}
import net.liftweb.common.{Logger, Box}
import xml.{NodeSeq, Text}
import org.aphreet.c3.loc.SuffixLoc
import org.aphreet.c3.snippet.groups.{AbstractGroupPageLoc, GroupPageData}
import net.liftweb.sitemap.Loc.Link
import net.liftweb.util.BindHelpers._
import org.aphreet.c3.util.CurrentUser
import net.liftweb.http.{S, SHtml}
import org.aphreet.c3.lib.DependencyFactory._
import com.ifunsoftware.c3.access.C3System
import net.liftweb.http.js.{JsCmds, JsCmd}
import org.aphreet.c3.service.groups.GroupService

/**
 * @author Serjk (mailto: serjk91@gmail.com)
 */
object GroupPageMembers extends AbstractGroupPageLoc[GroupPageData] with SuffixLoc {
  override val name = "Members"
  override val pathPrefix = "groups" :: Nil
  override val pathSuffix = "members" ::  Nil
  override def getItem(id: String) = Group.find(id)
  override def wrapItem(groupBox: Box[Group]) = groupBox.map(new GroupPageData(_))

  override def link = {
    new Link[GroupPageData](pathPrefix ++ pathSuffix){
      override def pathList(value: GroupPageData): List[String] = pathPrefix ::: value.group.id.is.toString :: Nil ::: pathSuffix
    }
  }
}
class GroupPageMembers(data: GroupPageData) extends GroupPageHelpers{

  lazy val c3 = inject[C3System].open_!
  lazy val groupService = inject[GroupService].open_!

  override lazy val group = data.group
  override lazy val activeLocId = "members"
  val members = group.users.all

  def owner = {
    ".GroupOwner *" #> group.owner.obj.map(_.shortName).openOr("N/A")&
      ".GroupOwner [href]" #> group.owner.obj.map(_.createLink)

  }
  def listUserAdd = {
    var users = User.findAll().filter(_.id.is != User.currentUser.open_!.id.is)
    members.map(user =>{
      users = users.filter(_.id.is != user.id.is)
    })
    if(users.isEmpty || User.currentUser.open_!.email.is != group.owner.obj.map(_.email).open_!.is){
      ".btn-toolbar" #> NodeSeq.Empty
    }
    else{
      ".contUser" #> users.map( user =>{
        ".contUser *" #> user.shortName &
        ".contUser [value]" #> user.email
      })
    }
  }
def listUser = {
    if (User.currentUser.open_!.email.is == group.owner.obj.map(_.email).open_!.is){
      ".ListGroupUser" #> members.map(user =>{
          if (user.email.is != group.owner.obj.map(_.email).open_!.is){
            def deleteUser():JsCmd = {
              if(groupService.removeUserFromGroup(group,user)){
                JsCmds.Replace(user.id.is.toString, NodeSeq.Empty)
              } else JsCmds.Alert("User is not removed! Please check logs for details")
            }
            ".ListGroupUser [id]" #> user.id.is &
            ".ListGroupUser *" #>
              ((n: NodeSeq) => SHtml.ajaxForm(
                (".first_name *" #> user.firstName.is &
                 ".last_name *" #> user.lastName.is &
                 ".email *" #> user.email.is andThen
                 "* *" #> SHtml.memoize(f => f ++ SHtml.hidden(deleteUser _))).apply(n)
              ))
          }else{
            ".first_name *" #> user.firstName.is &
            ".last_name *" #> user.lastName.is &
            ".email *" #> user.email.is &
            ".deluser *" #> NodeSeq.Empty
          }

      })
    }else{
      ".ListGroupUser *" #> members.map(user =>{
        ".first_name *" #> user.firstName.is &
          ".last_name *" #> user.lastName.is &
          ".email *" #> user.email.is &
          ".deluser *" #> NodeSeq.Empty
      })

    }
  }

  def addUserToGroup = {
    var listUserEmails = ""
    def saveMe(){
      val userEmails = listUserEmails.split('%')
      val members = userEmails.flatMap(User.findByEmail _)

      val (added, notAdded) = groupService.addUsersToGroup(group,members).partition(_.isDefined)

      if(!added.isEmpty)
        S.notice("Users are added to group " + group.name.is + ": " + added.mkString(","))
      if(!notAdded.isEmpty)
        // normally shouldn't happen
        S.error("Users are not added to group: " + group.name.is + ": " + notAdded.mkString(","))
    }

    "name=listusers" #> SHtml.onSubmit(listUserEmails = _) &
    "type=submit" #> SHtml.onSubmitUnit(saveMe)
  }

}
