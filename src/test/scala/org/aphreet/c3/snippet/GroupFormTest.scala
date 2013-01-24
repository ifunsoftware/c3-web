package org.aphreet.c3.snippet

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

import groups.snippet.GroupListPage
import net.liftweb.util._
import net.liftweb.common._
import junit.framework.TestCase
import net.liftweb.http.{LiftRules, S, LiftSession}
import net.liftweb.mapper._
import org.aphreet.c3.model._
import org.aphreet.c3.DBSetup

class GroupFormTest extends TestCase {

  private val session : LiftSession = new LiftSession("", StringHelpers.randomString(20), Empty)
  private val groupsData: List[(String, String)] =
    List(("group1", "Very interesting group"),
         ("group2", "Politics and other boring stuff"),
         ("group3", "Sports! Everything about it"),
         ("group4", "Green gardens fans united"),
         ("group5", "We love cats and hate dogs"))

  private var groups: List[Group] = Nil

  override def setUp() {
    // set up your db here

    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr
            "jdbc:h2:lift_test_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    DBSetup.setup()
  }

  override def tearDown(){
    // tear down your db here
    User.currentUser.map(user => user.delete_!).openOr(false)
    Group.findAll().foreach(_.delete_!)
  }

  private def getGroups(){

    val xml = {
        <p class="groupsHeader">Groups</p>
        <ul class="listGroup">
          <li class="container_groups"><a></a><button class="delete_group">&times;</button></li>
        </ul>
    }

    val snippet = new GroupListPage()
    val output = snippet.list(xml)

    println(classOf[GroupListPage].getName + " result output: " + output)

    // Do verification of data returned; assert if something is amiss
    assert ( (output \\ "li").length == groups.size)

    (output \\ "a").foreach { node =>
      assert ( groups.map(_.name.is).contains(node.text))
      assert (node.attributes.get("href").isDefined)
      assert ( groups.map(_.createLink).contains(node.attributes.get("href").get.text) )
    }
  }

  def testValue(){
    // Initialize session state if it is not already
    S.initIfUninitted(session) {
      // Create and log-in the user
      val user : User = User.create
      user.firstName("test")
      user.lastName("user")
      user.save

      createGroups(user, groupsData)

      User.logUserIn(user)
      // Call the test to run
      getGroups()
      ()
    }
    ()
  }

  private def createGroups(owner: User, data: List[(String, String)]){
    groups = data.map { case (name, description) =>
      val group = Group.create.name(name).description(description).owner(owner).saveMe()
      UserGroup.create.user(owner).group(group).save()
      group
    }
  }
}