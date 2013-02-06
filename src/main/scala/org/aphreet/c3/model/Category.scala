package org.aphreet.c3.model

import net.liftweb.mapper._
import net.liftweb.util.FieldError
import net.liftweb.common.Full

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

class Category extends LongKeyedMapper[Category] with IdPK with OneToMany[Long, Category]{

  def getSingleton = Category

   object name extends MappedString(this,64){
    override def validations = nonEmpty _ :: isUnique _ :: Nil

    private def isUnique(s: String): List[FieldError] = {
      if(!Category.find(Cmp(Category.name, OprEnum.Eql, Full(s.toLowerCase), None, Full("LOWER"))).isEmpty)
        List(FieldError(this, "Category with name " + s + " already exists"))
      else Nil
    }

    private def nonEmpty(s: String) =
      if(s.isEmpty) List(FieldError(this, "Category's name cannot be empty"))
      else Nil

  }

  object tags extends MappedOneToMany(Tag, Tag.category)

  // TODO ajantis: find some standard cascading approach
  override def delete_! = {
    this.tags.foreach(_.delete_!)
    super.delete_!
  }

  object linkedGroup extends MappedLongForeignKey(this, Group)

}

object Category extends Category with LongKeyedMetaMapper[Category] {

  override def dbTableName = "categories"
  override def fieldOrder = Nil

}