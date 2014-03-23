/**
 * Copyright (c) 2013, Dmitry Ivanov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
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

package org.aphreet.c3
package service.metadata

import com.ifunsoftware.c3.access.{C3Resource, C3System}
import com.ifunsoftware.c3.access.C3System._

import org.aphreet.c3.service.metadata.MetadataServiceProtocol._
import org.aphreet.c3.lib.DependencyFactory._
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor
import akka.routing.FromConfig
import actor.{ActorRef, Actor, OneForOneStrategy}
import actor.SupervisorStrategy.Resume

class MetadataService(notificationManager: ActorRef) extends Actor with C3Loggable {
  import context.dispatcher

  private val c3 = inject[C3System].open_!

  override def preStart() {
    scheduleNextQuery()
  }

  val workers = context.actorOf(Props(new MetadataServiceWorker(c3, notificationManager)).withRouter(FromConfig()),
    name = "metadataServiceWorkerRoutedActor")

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
    case _: Exception => Resume    
  }

  def receive = {
    case CheckForMetadataUpdates =>
      // query C3 for resources with special S4 meta processed tag
      logger.debug("Querying C3 system for S4 processed resources...")
      c3.query(Map(S4_PROCESSED_FLAG_META -> "true"), res => self ! ProcessC3Resource(res))
      scheduleNextQuery()

    case task @ ProcessC3Resource(res) =>
      logger.debug(s"C3 resource ${res.address} is retrieved. Forwarding for processing...")
      // forward to actual workers to process
      workers.forward(task)
  }

    case msg => logger.error("Unknown message is received: " + msg)
  }

  private[this] def scheduleNextQuery() =
    context.system.scheduler.scheduleOnce(5 minutes, self, CheckForMetadataUpdates)
}

object MetadataServiceProtocol {
  case object CheckForMetadataUpdates
  case class ProcessC3Resource(resource: C3Resource)
}