/**
 * Copyright (c) 2014, Dmitry Ivanov
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
package util

import net.liftweb.util.Helpers._
import net.liftweb.common.Box

import com.ifunsoftware.c3.access.{MetadataRemove, C3Resource}

import org.aphreet.c3.lib.metadata.Metadata._


trait C3ResourcesMetadataSupport {
  implicit def toMetadataReachResource(res: C3Resource): WebMetadata = WebMetadata(res)
}

object C3ResourcesMetadataSupport extends C3ResourcesMetadataSupport

case class WebMetadata(res: C3Resource) {
  import WebMetadata._

  lazy val tags: Set[String] =
    tagsRaw.map(value => value.split(TagSeparator).map(_.trim).toSet).getOrElse(Set())

  lazy val ownerId       : Box[Long]   = res.metadata.get(OWNER_ID_META).flatMap(asLong)
  lazy val groupId       : Box[String] = res.metadata.get(GROUP_ID_META)
  lazy val tagsRaw       : Box[String] = res.metadata.get(TAGS_META)
  lazy val isProcessedS4 : Boolean     = res.metadata.get(S4_PROCESSED_FLAG_META).flatMap(asBoolean).getOrElse(false)

  def removeProcessedS4Flag(): Unit = res.update(MetadataRemove(List(S4_PROCESSED_FLAG_META)))
}

object WebMetadata {
  val TagSeparator = ","
  val S4ProcessedFlag = S4_PROCESSED_FLAG_META
}
