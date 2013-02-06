package org.aphreet.c3.snippet.users.snippet

import net.liftweb.util.BindHelpers._
import org.aphreet.c3.model.User

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

class UserListPage extends UserHelpers{
  def list = {
    val users = User.findAll()
    ".user *" #> users.map {
      user => toCssBindings(user) }
  }
}