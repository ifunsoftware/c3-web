package org.aphreet.c3.loc

import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap.Loc
import net.liftweb.http._
import org.aphreet.c3.model.User
import net.liftweb.http.RewriteRequest
import net.liftweb.http.ParsePath
import net.liftweb.sitemap.Loc.TestValueAccess
import net.liftweb.common.Full
import xml.NodeSeq


/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
trait ItemRewriteLoc[S, T <: PageData] extends Loc[T] {
  type MyT = T
  type MyS = S

  override def link = new Link[T](pathList)
  override def text = LinkText.strToLinkText[T](name)
  override def defaultValue: Box[T] = wrapItem(getDefault)

  val pathPrefix: List[String]
  lazy val pathList: List[String] = pathPrefix

  def getDefault: Box[S] = Empty
  def getItem(id: String): Box[S]
  def wrapItem(itemBox: Box[MyS]): Box[T]

  /**
   * Override this function to provide a canonical URL
   */
  def canonicalUrl(data: T): Box[String] = Empty
  override def params: List[LocParam[T]] =
    Hidden ::
    TestValueAccess[T](_.flatMap(canonicalUrl(_))
                        .filter(_ != S.uri)
                        .map(RedirectResponse(_))) :: Nil

  /**
   * By default the path must end after the item id.
   * Override to handle other cases.
   */
  def finishPath(itemBox: => Box[MyS],
                 restPath: List[String],
                 suffix: String = ""): Box[T] = {
    if (restPath == Nil) wrapItem(itemBox) else Empty
  }

  private class PrefixRewriteRequest(prefix: List[String]) {
    def unapply(in: RewriteRequest): Option[T] = {
      in.path.partPath.splitAt(prefix.length) match {
        case (`prefix`, id :: rest) => {
          //wrapItem(getItem(id))
          finishPath(getItem(id), rest, in.path.suffix)
        }
        case _ => Empty
      }
    }
  }

  private object ItemRewriteRequest extends PrefixRewriteRequest(pathPrefix)

  override def rewrite: LocRewrite = Full({
    case ItemRewriteRequest(item) =>
      (RewriteResponse(ParsePath(pathList, "", true, false), Map.empty, true),
       item)
  })
}

trait PageData {
  def currentUser: Box[User] = User.currentUser
}

/**
 * For URLs of the for /foo/[ID]/suffix
 */
trait SuffixLoc {
  self: ItemRewriteLoc[_, _] =>

  val pathSuffix: List[String]
  override lazy val pathList: List[String] = pathPrefix ++ pathSuffix

  override def link = {
    new Link[MyT](pathList)
  }

  override def finishPath(itemBox: => Box[MyS],
                          restPath: List[String],
                          suffix: String = ""): Box[MyT] = {
    if (restPath == pathSuffix) wrapItem(itemBox) else Empty
  }
}