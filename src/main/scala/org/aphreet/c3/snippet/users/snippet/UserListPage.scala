package org.aphreet.c3.snippet.users.snippet

import net.liftweb.http.SHtml
import net.liftweb.http.js.{ JsCmd, JsCmds }
import net.liftweb.mapper.By
import net.liftweb.util.BindHelpers._
import org.aphreet.c3.model.User
import org.aphreet.c3.util.helpers.{ AdminPageHelper, UserHelper }

import scala.xml.NodeSeq

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

class UserListPage extends UserHelper with AdminPageHelper {

  override lazy val activeLocId = "users"

  def list = {
    val users = User.findAll(By(User.enabled, true))
    val current = User.currentUser.openOrThrowException("User is not detected!")
    ".user-head *" #> (
      if (current.superUser.is) {
        //        ".name-admin *" #> "Admin" &
        ".name-enabled *" #> "Enabled"
      } else {
        //        ".name-admin *" #> NodeSeq.Empty &
        ".name-enabled *" #> NodeSeq.Empty
      }) &
      ".user " #> users.map { user =>
        {

          if (current.superUser.is) {
            def setSuperAdmin(b: Boolean): JsCmd = {
              user.superUser.set(b)
              user.save

              JsCmds.Noop
            }
            def deleteUser(): JsCmd = {
              user.enabled.set(false)
              user.save
              if (!user.enabled.is) {
                JsCmds.Replace(user.id.is.toString, NodeSeq.Empty)
              } else JsCmds.Alert("User is not removed! You don't have administrator rights! Please check logs for details")
            }

            def resetPassword(): JsCmd = {
              val defaultPassword = "qwerty"
              user.password.set(defaultPassword)
              user.save
              JsCmds.Alert("Passwords is changed to 'qwerty'")
            }

            ".user [id]" #> user.id.is &
              ".user *" #>
              ((n: NodeSeq) => SHtml.ajaxForm(
                (toCssBindings(user) &
                  ".is_admin" #> NodeSeq.Empty &
                  ".enabled *" #> (if (user.enabled.is) "Yes" else "No") &
                  (if (user.id != current.id) {
                    ".resetpwd [onclick]" #> SHtml.ajaxInvoke(() => resetPassword) &
                      ".deluser [onclick]" #> SHtml.ajaxInvoke(() => deleteUser) &
                      ".admin_checkbox " #> SHtml.ajaxCheckbox(user.superUser.is, setSuperAdmin(_))
                  } else {
                    ".admin_checkbox" #> NodeSeq.Empty &
                      ".deluser *" #> NodeSeq.Empty &
                      ".resetpwd *" #> NodeSeq.Empty

                  })).apply(n)))

          } else {
            toCssBindings(user) &
              ".is_admin" #> (if (user.superUser.is) "Yes" else "No") &
              ".enabled *" #> NodeSeq.Empty &
              ".deluser *" #> NodeSeq.Empty &
              ".resetpwd *" #> NodeSeq.Empty &
              ".admin_checkbox" #> NodeSeq.Empty
          }
        }

      }
  }
}
