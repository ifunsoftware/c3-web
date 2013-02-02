package org.aphreet.c3.snippet.search.snippet

import xml.NodeSeq
 import net.liftweb.http.js.JsCmds._
 import net.liftweb.http.js.JsCmds
 import net.liftweb.util.Helpers._
 import net.liftweb.http.{S, SHtml}
 import org.aphreet.c3.lib.DependencyFactory._
 import com.ifunsoftware.c3.access.{C3System, SearchResultEntry}
 import org.apache.commons.httpclient.util.URIUtil
 import java.text.SimpleDateFormat
import org.aphreet.c3.model.{MessagesType, C3Path, Tag, Category}
import net.liftweb.mapper.By

class SearchSnippet {

  private val c3 = inject[C3System].open_!

  def miniSearch ={

    def process(query: String){
      if (!query.isEmpty){
        S.redirectTo(urlEncode("/search?query=" + queryParam(Nil,query)))
      }
      else{
        S.notice("Empty search query")
      }
    }
    val query = S.param("query").openOr("")

    "name=query [value]" #> query &
    "name=query" #> SHtml.onSubmit(process _)
  }

  private def queryParam(tags: List[String],query:String) = {
    //TODO:query params =  tags + query text
    query
  }

  def search = {
    "* *" #> ((x: NodeSeq) => x ++ Script(OnLoad(JsCmds.JsShowId("categories_s"))))
  }

  val tagSeparator = ","

  def render = {
    var tags: List[String] = Nil
    var query = S.param("query").openOr("")

    def process(){

      if (!tags.isEmpty || !query.isEmpty){
        S.redirectTo("/search?query=" + queryParam(tags,query))
      }
      else{
        S.notice("Entry param search")
      }
    }

    "name=query [value]" #> query &
    "name=tags" #> SHtml.onSubmit(v => tags = v.split(tagSeparator).toList)&
    "name=query" #> SHtml.onSubmit(query = _)&
    "type=submit" #> SHtml.onSubmitUnit(process)
  }

  def result = {
    val query = S.param("query")
    val results: List[SearchResultEntry] = query.map(c3.search(_)).openOr(Nil)

    if (results.isEmpty){
      ".conteyner_result" #> NodeSeq.Empty
    }else{
      ".conteyner_result *" #> results.map {
        entry => toCss(entry)
      }
    }
  }
private lazy val dateFormat: SimpleDateFormat = new SimpleDateFormat("dd/MM/yyyy")
 private def toCss(entry: SearchResultEntry) = {
    val resource = c3.getResource(entry.address, List("c3.ext.fs.path"))
//    val metadata = resource.metadata
    val path = resource.systemMetadata.getOrElse("c3.ext.fs.path", "")
    var fragment = ""
    entry.fragments.map(f=> fragment = fragment+f.strings.headOption.getOrElse("") )
    val c3Path = C3Path(path)
    var resourceName=""
    c3Path.resourceType match {
      case MessagesType => {resourceName ="message from group:"+c3Path.groupName}
      case _ => {resourceName= c3Path.resourceName}
    }
    ".header_search *" #> resourceName&
    ".address *" #> entry.address &
    ".fragment_search *" #>  fragment &
    ".header_search [href]" #> c3Path.resourceUri &
    ".date_create *" #> dateFormat.format(resource.date)
 }
  def categories = {
    val categories = Category.findAll()
    var flag2 = 0
    def categoryContents(cat: Category) = {
      flag2+=1
      val id = "tab"+flag2
      var id_span = 0
      val tagNames = Tag.findAll(By(Tag.category, cat))
      def groupCat(cat:Category)={
        if (!cat.linkedGroup.isEmpty){
          val group=cat.linkedGroup.open_!
          ".linkGroup [href]" #>("/groups/"+group.id)&
          ".category_cont [class+]" #> "floatLeft" &
          ".category_cont *" #> cat.name.is
        }
        else{
          ".category_cont *" #> cat.name.is &
          ".linkGroup" #> NodeSeq.Empty
        }
      }
      "span" #> tagNames.map{tg:Tag =>{
        id_span+=1
        "span *" #> tg.name &
        "span [id]" #> (id+"_"+id_span)
      }}&
       "ul [id]" #> id &
       ".con_category" #> groupCat(cat)
    }
    ".category_cnt *" #> categories.map{ cat:Category =>  categoryContents(cat) }
  }
}
