/**
 * Copyright (c) 2011, Mikhail Malygin
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
package org.aphreet.c3.util.helpers

import net.liftweb.http.{StreamingResponse, S}
import net.liftweb.common.Full
import com.ifunsoftware.c3.access.C3System
import org.aphreet.c3.model.C3Path
import org.aphreet.c3.lib.DependencyFactory._

object C3Streamer{

  def apply(groupId: String, path: List[String], extension:String) = {
    () => {

      val c3 = inject[C3System].open_!

      try{
        val file = c3.getFile(C3Path(groupId, path, extension))
        val metadata = file.metadata

        val stream = file.versions.last.getDataStream
        val length = stream.length
        val contentType = metadata.getOrElse("content.type", "application/octet-stream")

        //If you see an error here, it is an issue of the IDEA scala plugin
        Full(StreamingResponse(stream, ()=> stream.close(), length, List("Content-Type" -> contentType), Nil, 200))
      } catch {
        case e: Exception => {
          e.printStackTrace()
          S.notice("No file found!")
          S.redirectTo("/groups/" + groupId + "/files/"+path.init.mkString("/"))
        }
      }
    }
  }
}
