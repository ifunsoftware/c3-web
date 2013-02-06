package org.aphreet.c3.webdav

import net.sf.webdav.ITransaction
import collection.mutable
import com.ifunsoftware.c3.access.fs.C3FileSystemNode
import java.security.Principal
import org.aphreet.c3.model.{Group, User}

class C3Transaction(val principal:Principal) extends ITransaction{

  def getPrincipal = principal

  val cachedFiles = new mutable.HashMap[String, C3FileSystemNode]

  val createdFiles = new mutable.HashSet[String]()

  def close() {

  }
}

class C3Principal(val user: User) extends Principal{

  def getName = user.email

  val groups: Set[String] = if(user != null){
    user.groups.map((g: Group) => g.id.toString()).toSet
  }else{
    Set()
  }
}
