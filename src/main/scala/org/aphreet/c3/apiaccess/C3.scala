package org.aphreet.c3.apiaccess

import net.liftweb.util.Props
import com.ifunsoftware.c3.access.{C3SystemFactory, C3System}
import net.liftweb.common.Logger
import net.liftweb.http.LiftRules
import net.liftweb.http.provider.servlet.HTTPServletContext

object C3 {

  private val log = Logger("C3")
  lazy val c3System = createC3System()

  def apply():C3System = {
    c3System
  }

  private def createC3System():C3System = {
    val bundleContext = LiftRules.context match {
      case context:HTTPServletContext => context.ctx.getAttribute("osgi-bundlecontext") match {
        case null => null
        case value => value
      }
      case _ => null
    }

    val domain = Props.get("c3_domain_name") openOr "anonymous"

    if (bundleContext != null){
      log.info("Found bundle context, trying to obtain local C3System instance")

      new C3SystemFactory().createLocalSystem(domain, bundleContext)
    } else {

      val host = Props.get("c3_host") openOr("http://localhost:7373")

      val secret = Props.get("c3_domain_secret") openOr ""

      val proxy = System.getenv("HTTP_PROXY")

      if (proxy != null){
        val hostAndPort = proxy.replaceFirst("^http://", "").split(":", 2)
        val proxyHost = hostAndPort(0)
        val proxyPort = hostAndPort(1).toInt

        new C3SystemFactory().createSystem(host, domain ,secret, 100, proxyHost, proxyPort)
      }else{
        new C3SystemFactory().createSystem(host, domain, secret)
      }
    }
  }

}