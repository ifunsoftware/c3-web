package org.aphreet.c3.model

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
import net.liftweb.mapper._
import net.liftweb.util.FieldError
import net.liftweb.common.Full

class Tag extends LongKeyedMapper[Tag] with IdPK {

  def getSingleton = Tag

  object category extends MappedLongForeignKey(this,Category)

  object name extends MappedString(this, 256){
    override def validations = nonEmpty _ :: isUniqueWithinCategory _ :: Nil

    private def isUniqueWithinCategory(s: String): List[FieldError] = {
      if(!Tag.find(Cmp(Tag.name, OprEnum.Eql, Full(s.toLowerCase), None, Full("LOWER")), By(Tag.category, category)).isEmpty)
        List(FieldError(this, "Tag with name " + s + " already exists within category"))
      else Nil
    }

    private def nonEmpty(s: String) =
      if(s.isEmpty) List(FieldError(this, "Tag's name cannot be empty"))
      else Nil
  }


}

object Tag extends Tag with LongKeyedMetaMapper[Tag] {

  override def dbTableName = "tags"
  override def fieldOrder = Nil

}