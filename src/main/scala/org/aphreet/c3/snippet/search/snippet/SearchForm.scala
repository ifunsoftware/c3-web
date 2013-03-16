package org.aphreet.c3.snippet.search.snippet

import net.liftweb.util.Helpers._
import org.aphreet.c3.model.{MessagesType, C3Path, Tag, Category}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.{JsCmd, jquery, JsCmds}
import jquery.JqJsCmds
import xml.{Unparsed, XML, NodeSeq}
import net.liftweb.http.js.jquery.JqJsCmds._
import org.aphreet.c3.lib.DependencyFactory._
import com.ifunsoftware.c3.access.{SearchResultEntry, C3System}
import net.liftweb.util.CssSel
import java.text.SimpleDateFormat
import org.aphreet.c3.lib.metadata.Metadata

/**
 * @author Serjk (mailto: serjk91@gmail.com)
 */
class SearchForm {

  private val c3 = inject[C3System].open_!
  private val dateFormat = new SimpleDateFormat("MMM dd, yyyy")

  val selectedTagsContainerId = "selected_tags"
  val tagTemplate = <li class="tag"><a href="#" class="label btn-info name">Sample tag</a></li>

  def miniSearch ={
    def process(query: String){
      if (!query.isEmpty){
        S.redirectTo("/search?query=" + urlEncode(query))
      }
      else{
        S.notice("Empty search query")
      }
    }
    "name=query [value]" #> S.param("query") &
      "name=query" #> SHtml.onSubmit(process _)
  }

  def render = {

    var query = S.param("query").openOr("")
    var tags: Set[String] = Set()
    var entryHtml = NodeSeq.Empty

    def selectTag(tag: Tag): JsCmd = {
      tags = tags + tag.name.is

      JsCmds.Replace("tag_" + tag.id.is, NodeSeq.Empty) &
      JqJsCmds.AppendHtml(selectedTagsContainerId,
        <li id={"sel_tag_" + tag.id.is} class="label btn-info sel-tag">
          <span>{tag.name.is}</span>
          <a onclick={SHtml.ajaxInvoke(() => unselectTag(tag))._2.cmd.toJsCmd}>
            <i class="icon-remove-sign icon-white"></i>
          </a>
        </li>
      )
    }

    def unselectTag(tag: Tag): JsCmd = {
      tags = tags - tag.name.is
      JqJsCmds.AppendHtml("category_" + tag.category.get + "_tags", tagToCss(tag)(tagTemplate)) &
      JsCmds.Replace("sel_tag_" + tag.id.is, NodeSeq.Empty)
    }

    def tagToCss(tag: Tag) = {
      "li [id]" #> ("tag_" + tag.id.is) &
      ".name *" #> tag.name.is &
      ".name [onclick]" #> SHtml.ajaxInvoke(() => selectTag(tag))
    }

    def doSearch(): JsCmd = {
      val results: List[SearchResultEntry] = query match {
        case "" => Nil
        case s => c3.search(s)
      }
      val t = entryHtml
      JsCmds.SetHtml("results", results.flatMap(entry => toCss(entry).apply(entryHtml)))
    }

    def toCss(result: SearchResultEntry): CssSel = {
      val resource = c3.getResource(result.address, List("c3.ext.fs.path"))
      val c3Path = C3Path(resource.metadata.getOrElse("c3.ext.fs.path", ""))
      val nodeName = resource.systemMetadata.getOrElse("c3.fs.nodename", "<Unknown>")
      val content = result.fragments.headOption.flatMap(_.strings.headOption)
      val resourceName = c3Path.resourceType match {
        case MessagesType => "Message in group: " + c3Path.groupName
        case _ => c3Path.resourceName
      }
      val tags = resource.metadata.get(Metadata.TAGS_META).map(_.split(",").toList).getOrElse(Nil)

      ".result_header *" #> nodeName &
      ".result_link [href]" #> c3Path.resourceUri &
      ".result_link *" #> c3Path.resourceUri &
      ".result_content *" #> Unparsed(content.getOrElse("")) &
      ".result_date *" #> dateFormat.format(resource.date) &
      ".tag *" #> {
        ".label *" #> tags
      }
    }

    ".category *" #> Category.findAll().map {
      category => {
        val tags = category.tags.toList
        "ul [id]" #> ("category_" + category.id.is + "_tags") &
        ".name *" #> category.name.is &
        ".tag" #> tags.map { tag =>
          tagToCss(tag)
        }
      }
    } &
    ".search_form *" #> { xml =>
      SHtml.ajaxForm(
        (".search_query [value]" #> query &
         ".search_query" #> SHtml.onSubmit(query = _) &
         "type=submit" #> (xml => xml ++ SHtml.hidden(doSearch _))).apply(xml)
      )
    } &
    "#results *" #> {
      ".entry" #> (xml => { entryHtml = xml; xml }) &
      ".entry *" #> (if(query.isEmpty) Nil else c3.search(query)).map { res: SearchResultEntry =>
        toCss(res)
      }
    }
  }

}
