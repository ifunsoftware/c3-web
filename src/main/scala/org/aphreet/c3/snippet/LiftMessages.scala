package org.aphreet.c3.snippet

import xml.NodeSeq
import net.liftweb.http.LiftRules
import net.liftweb.http.S._
import net.liftweb.util.Helpers._
/**
 * @author Dmitry Ivanov (mailto: id.ajantis@gmail.com)
 *         iFunSoftware
 *
 * This Lift messages binder can be used w/ Bootstrap and other CSS frameworks
 *
 * Markup example:
 *   <div class="lift:LiftMessages">
 *        <div class="lift_messages_container">
 *            <div class="errors">
 *                <div class="alert alert-error fade in">
 *                    <p class="message">Actual message text</p>
 *                </div>
 *             </div>
 *             <div class="warnings">
 *                 <div class=" alert alert-warning fade in">
 *                     <button type="button" class="close" data-dismiss="alert">×</button>
 *                     <p class="message">Actual message text</p>
 *                 </div>
 *             </div>
 *             <div class="notices">
 *                 <div class="alert alert-info fade in">
 *                     <button type="button" class="close" data-dismiss="alert">×</button>
 *                     <p class="message">Actual message text</p>
 *                 </div>
 *             </div>
 *         </div>
 *   </div>
 *
 */
class LiftMessages {

  protected def messages(ms: List[NodeSeq]) = ms match {
    case Nil =>
       "*" #> NodeSeq.Empty  // clearing everything inside this message class container
    case _ =>
      ".message *" #> ms // binding each message as <.. class="message"> node
  }

  def render = {
     ".lift_messages_container [id]" #> LiftRules.noticesContainerId &
     ".errors *" #> messages(noIdMessages(errors)) &
     ".warnings *" #> messages(noIdMessages(warnings)) &
     ".notices *" #> messages(noIdMessages(notices))
   }

}