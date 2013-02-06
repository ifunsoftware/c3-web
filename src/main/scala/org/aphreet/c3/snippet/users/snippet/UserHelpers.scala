package org.aphreet.c3.snippet.users.snippet

import net.liftweb.util.BindHelpers._
import org.aphreet.c3.model.User
import xml.Text

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

trait UserHelpers {
  def toCssBindings(user: User) = {
    ".email *" #> user.email &
    ".mailto [href]" #> Text("mailto:"+user.email.is) &
    ".link [href]" #> user.createLink &
    ".name *" #> user.shortName &
    ".name [class+]" #> (if(user.superUser.is) "admin" else "") &
    ".is_admin *" #> (if(user.superUser.is) "Yes" else "No")
  }
}