package org.aphreet.c3.snippet.categories.snippet

import org.aphreet.c3.model.{User, Category, Tag}
import net.liftweb.mapper.{By, OprEnum, Cmp}
import net.liftweb.common.Full
import net.liftweb.http.{SHtml, S}
import net.liftweb.util.BindHelpers._
import net.liftweb.util.Helpers
import xml.NodeSeq

/**
 * @author Serjk
 * C3 Web team
 */
class AddTag {

  def adm = {
    User.currentUser match {
      case Full(user) => {
        "* *" #> ((x: NodeSeq) => x)
      }
      case _ =>{
        ".add_cat" #> NodeSeq.Empty
      }
    }
  }

  def render = {
    var categoryName = ""
    val tag = Tag.create

    def process() = {
      val category = Category.find(Cmp(Category.name, OprEnum.Eql, Full(categoryName.toLowerCase), None, Full("LOWER")))

      if(category.isEmpty)
        S.error("Category with name " + categoryName + " is not found!")
      else {
        tag.category(category.open_!)
        tag.validate match {
          case Nil => {
            tag.save()
            S.notice("Tag is added.")
          }
          case xs => {
            xs.foreach(fe => S.error(fe.msg))
          }
        }
      }
    }
    "name=categoryName" #> SHtml.onSubmit(categoryName = _)&
    "name=TagName" #> SHtml.onSubmit(tag.name(_))&
    "type=submit" #> SHtml.onSubmitUnit(process _)
  }

}
