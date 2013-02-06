package org.aphreet.c3.snippet

import net.liftweb.util.BindHelpers._
import net.liftweb.sitemap.Loc
import net.liftweb.http.S
import xml.{NodeSeq, Text}
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.util.Helpers._

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */

class Breadcrumbs {

  def breadcrumb = "*" #> {
    val allLocs: List[Loc[_]] =
      for {
        currentLoc <- S.location.toList
        loc <- currentLoc.breadCrumbs
      } yield loc

    val currentLoc = allLocs.lastOption

    def setActiveClass(loc: Loc[_]) = {
      if(loc == currentLoc.get)
        ".link [class+]" #> "active" & // attach class
        ".divider" #> NodeSeq.Empty // remove last divider
      else
        ".link [class!]" #> "active" // remove class
    }


    "li *" #> allLocs.map {
      loc => {
        val propsBuilder = new LocPropertiesBuilder(loc, allLocs)

        ".link *" #> propsBuilder.buildLocTitle() &
        ".link [href]" #> propsBuilder.buildLocLink() &
        setActiveClass(loc)
      }
    }
  }

}

class LocPropertiesBuilder[T](loc: Loc[T], allLocs: List[Loc[_]])(implicit manifest : Manifest[T]){

  lazy val valueBox: Box[T] = findContextValueForLoc(loc)

  def findContextValueForLoc(loc: Loc[T]): Box[T] = {
    loc.currentValue.isEmpty match {
      case false => loc.currentValue
      case true => {
        def compareLocParamTypes[A, B](l1: Loc[A],l2: Loc[B])(implicit m1 : Manifest[A], m2 : Manifest[B]): Boolean = {
          m1 == m2
        }

        allLocs.dropWhile(loc != _) match {
          case Nil => Empty
          case xs => xs.tail.filter(l => !l.currentValue.isEmpty && compareLocParamTypes(loc, l)).headOption.map(_.currentValue.open_!.asInstanceOf[T])
        }
      }
    }
  }

  def buildLocLink(): NodeSeq = {
    val linkOption: Option[NodeSeq] = valueBox match {
      case Full(v) =>
        loc.createLink(v)
      case _ =>
        loc.createDefaultLink
    }
    linkOption.getOrElse(Text("#"))
  }

  def buildLocTitle(): NodeSeq = {
    val titleOption: NodeSeq = valueBox match {
      case Full(v) =>
        loc.title(v)
      case _ =>
        loc.title
    }
    titleOption
  }
}