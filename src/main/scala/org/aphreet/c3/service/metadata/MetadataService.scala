package org.aphreet.c3.service.metadata

import akka.actor.Actor
import org.aphreet.c3.service.metadata.MetadataServiceProtocol._
import org.aphreet.c3.lib.DependencyFactory._
import com.ifunsoftware.c3.access.{C3Resource, C3System}
import akka.util.duration._
import akka.routing.FromConfig
import akka.actor
import org.aphreet.c3.util.C3Loggable
import org.aphreet.c3.lib.metadata.Metadata
import Metadata._

/**
 * Copyright iFunSoftware 2013
 * @author Dmitry Ivanov
 */
class MetadataService extends Actor with C3Loggable{

  private val c3 = inject[C3System].open_!

  val workersRouted =
    context.actorOf(actor.Props[MetadataServiceWorker].withRouter(FromConfig()), name = "metadataServiceWorkerRoutedActor")

  def receive = {
    case CheckForMetadataUpdates => {
      // query C3 for resources with special S4 meta processed tag
      logger.debug("Querying C3 system for S4 processed resources...")
      c3.query(Map((S4_PROCESSED_FLAG_META -> "true")), res => self ! ProcessC3Resource(res))
    }
    case task @ ProcessC3Resource(res) => {
      logger.debug("C3 resource " + res.address + " is retrieved. Forwarding for processing...")
      // forward to actual workers to process
      workersRouted forward task
    }
    case msg => logger.error("Unknown message is received: " + msg)
  }

  this.context.system.scheduler.schedule(5 minutes, 5 minutes, self, CheckForMetadataUpdates)
}

object MetadataServiceProtocol {
  object CheckForMetadataUpdates
  case class ProcessC3Resource(resource: C3Resource)
}