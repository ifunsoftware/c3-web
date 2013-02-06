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

import net.liftweb.mapper.By
import net.liftweb.http.{SHtml, S}
import org.aphreet.c3.model.{UserGroup, User, Group}
import xml.{Text, NodeSeq}
import net.liftweb.common.{Empty, Full}
import net.liftweb.util.BindHelpers._

class GroupAdmin {

  def addUsers(html: NodeSeq) : NodeSeq = {
    S.param("groupname") match {
      case Full(name) => {
        Group.find(By(Group.name,name)) match {
          case Full(group) => {
            bind("addUsers", html,
              "user" -> SHtml.selectObj[User](User.findAll.filter(user => UserGroup.find(By(UserGroup.user,user),By(UserGroup.group,group)).isEmpty).map(user => (user,user.email.is)),Empty, usr => UserGroup.join(usr,group)),
              "submit" -> SHtml.submit("Add", ()=> {S.redirectTo(S.uri)})
            )
          }
          case _ => Text("")
        }
      }
      case _ => Text("")
    }


  }

  def editGroup(html: NodeSeq) : NodeSeq = {


    S.param("groupname") match {
      case Full(name) => {
        Group.find(By(Group.name,name)) match {
          case Full(group) => {
            bind("groupadmin", html, "groupname" -> SHtml.text(group.name, group.name(_)),
              "description" -> group.description.toForm,
              "users" -> {(ns: NodeSeq) => {
                group.users.flatMap(user => bind("groupuser",ns,"username" -> user.email.is,
                  "selectuser" -> {
                    val isCurrent = if(User.currentUser == user) true
                    else false

                    SHtml.checkbox(true,
                      (check) => if(!check && !isCurrent) UserGroup.find(By(UserGroup.user,user),By(UserGroup.group,group)).open_!.delete_! ,
                      // "disable" attribute to disable possibility for user to exclude himself from group
                      if(isCurrent)("disabled" -> "true") else ("enabled" -> "enabled") )
                  }
                ))
              }:NodeSeq },
              "submitGroup" -> SHtml.submit("Save", () => {
                group.validate match {
                  case Nil => {
                    group.save
                    S.redirectTo(S.uri)
                  }
                  case xs => S.error(xs)
                }
              })
            )
          }
          case _ => Text("")
        }
      }
      case _ => Text("")
    }

  }

}