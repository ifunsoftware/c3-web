/**
 * Copyright (c) 2011, Dmitry Ivanov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *

 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.snippet

import net.liftweb.util.BindHelpers._
import net.liftweb.http.{SHtml, S}
import xml.{Text, NodeSeq}
import net.liftweb.common.Full
import net.liftweb.mapper.By
import org.aphreet.c3.model.{Group, User}

 
class UserForm {

  def add(form: NodeSeq) : NodeSeq = {
    val invokedAs = S.invokedAs
    var user = User.create


    def newUser(form: NodeSeq): NodeSeq = {
      def saveMe(): Unit = {
        user.validate match {
          case Nil => {
            user.save

            S.notice("Added user: " + user.firstName+" "+user.lastName); S.redirectTo("/users/")
          }
          case xs => S.error(xs) ; S.mapSnippet(invokedAs, newUser)

        }
      }

      bind("user", form,
           "firstName" -> user.firstName.toForm,
           "lastName" -> user.lastName.toForm,
           "admin" -> user.superUser.toForm,
           "submit" -> SHtml.submit("add", saveMe))
    }

    newUser(form)
  }

  def edit (form: NodeSeq) : NodeSeq = {

    val invokedAs = S.invokedAs


    def editUser(form: NodeSeq, user: User) : NodeSeq = {
      def saveUser (): Unit = {
        user.validate match {
          case Nil => {
            user.save
            S.notice("User " + user.firstName+" "+user.lastName + " saved."); S.redirectTo("/users/")
          }
          case xs => {
            S.error(xs)
            S.redirectTo("/user/"+user.email.is)
          }
        }
      }

      bind("user", form,
                 "firstName" -> user.firstName.toForm,
                 "lastName" -> user.lastName.toForm,
                 "email" -> user.email.toForm,
                 "superUser" -> user.superUser.toForm,
                 "usergroups" -> user.groups.toForm,
                 "submit" -> SHtml.submit("Save",saveUser) )
    }


    S.param("useremail") match {
      case Full(email) => {
        User.find(By(User.email,email)) match {
          case Full(user: User) => {
            editUser(form,user)
          }
          case _ => Text("User wasn't found.")
        }
      }
      case _ => {
        Text("User e-mail is not set.")
      }
    }

  }

}