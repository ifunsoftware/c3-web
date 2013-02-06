package org.aphreet.c3.snippet.categories.snippet

import net.liftweb.util.Helpers._
import org.aphreet.c3.model.{User, Tag, Category}
import xml.NodeSeq
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JsCmds.{OnLoad, Script}
import net.liftweb.http.{SHtml, S}
import net.liftweb.common.Full

class CategoryListPage {

  val categories = Category.findAll()


  def list = {
    val categories = Category.findAll()

    def categoryContents(cat: Category) = {
      val id = "category_" + cat.id.is
      val tags: List[Tag] = cat.tags.toList

      ".label_tag" #> tags.map {
        tag: Tag => {
          val formId = "tag_" + tag.id.is

          def deleteTag(): JsCmd = {
            tag.delete_!
            JsCmds.Replace(formId, NodeSeq.Empty)
          }

          ".label_tag [id]" #> formId &
          ".label_tag *" #>
            ((n: NodeSeq) => SHtml.ajaxForm(
              ("span *" #> tag.name.is andThen
               "* *" #> SHtml.memoize(f => f ++ SHtml.hidden(deleteTag _))).apply(n)
            ))
        }
      } &
      ".muted *" #> cat.name &
      ".muted [id]" #> id
    }

    ".well *" #> categories.map{ cat:Category =>  categoryContents(cat) } andThen
    "* *" #> ((x: NodeSeq) => x ++ Script(OnLoad(JsCmds.JsHideId("left-panel"))))
  }

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
    val category = Category.create
    def process() {
      category.validate match {
        case Nil => {
          category.save()
          S.notice("Category is added")
        }
        case xs =>
          xs.foreach(f => S.error(f.msg))
      }
    }
    "name=categories" #> SHtml.onSubmit(category.name(_))&
    "type=submit" #> SHtml.onSubmitUnit(process)
  }

}

