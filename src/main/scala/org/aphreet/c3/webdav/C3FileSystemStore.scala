package org.aphreet.c3.webdav

import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}
import java.io.{File, InputStream}
import java.security.Principal
import org.slf4j.LoggerFactory
import org.aphreet.c3.apiaccess.C3
import com.ifunsoftware.c3.access.fs.C3FileSystemNode
import java.nio.file.{StandardCopyOption, Files}
import com.ifunsoftware.c3.access.{C3AccessException, DataStream}
import javax.servlet.http.HttpServletRequest
import org.aphreet.c3.model.User
import net.liftweb.mapper.By
import org.apache.commons.codec.binary.Base64
import net.liftweb.common.Full
import net.sf.webdav.exceptions.{AccessDeniedException, UnauthenticatedException}

class C3FileSystemStore(val root:File) extends IWebdavStore{

  val log = LoggerFactory.getLogger(getClass)

  val c3System = C3()

  def createPrincipal(request: HttpServletRequest):Principal = {

    val authHeader = request.getHeader("Authorization")

    if(authHeader != null){
      val loginPassword = new String(Base64.decodeBase64(authHeader.replaceFirst("Basic\\s+", "") .getBytes("UTF-8")), "UTF-8")

      val credentials = loginPassword.split(":", 2)

      val mail = credentials(0)
      val password = credentials(1)

      User.find(By(User.email, mail)) match {
        case Full(user) => {
          if(user.password.match_?(password)){
            new C3Principal(user)
          }
          else throw new AccessDeniedException()
        }
        case _ => throw new AccessDeniedException()
      }

    }else{
      throw new UnauthenticatedException()
    }
  }

  def begin(principal: Principal):ITransaction = {
    log.debug("begin()")
    new C3Transaction(principal)
  }

  def checkAuthentication(tx: ITransaction) {
    if(tx.getPrincipal == null){
      throw new AccessDeniedException()
    }
  }

  def commit(tx: ITransaction) {
    log.debug("commit()")
    log.debug("Cached nodes: {}", tx.cachedFiles)

    tx.close()
  }

  def rollback(tx: ITransaction) {
    log.info("rollback()")

    tx.close()
  }

  def destroy() {
    log.info("destroy()")
  }

  def createFolder(tx: ITransaction, uri: String) {
    log.debug("createFolder() " + uri)

    val splitUri = uri.split("/")

    val parent = splitUri.dropRight(1).mkString("/")
    val child = splitUri.last

    log.debug("Going to create directory \"" + child + "\" in the dir " + parent)

    getFSNode(tx, parent).asDirectory.createDirectory(child)
  }

  def createResource(tx: ITransaction, uri: String) {
    log.debug("createResource() " + uri)
    tx.createdFiles.add(uri)
  }

  def getResourceContent(tx: ITransaction, uri: String) = {
    getFSNode(tx, uri).asFile.versions.last.getDataStream
  }

  def setResourceContent(tx: ITransaction, uri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    log.debug("setResourceContent() " + uri)

    val tmpFile = File.createTempFile("dav_upload-", null).toPath

    try{
      Files.copy(content, tmpFile, StandardCopyOption.REPLACE_EXISTING)

      if(tx.createdFiles.contains(uri)){

        val splitUri = uri.split("/")

        val parent = splitUri.dropRight(1).mkString("/")
        val child = splitUri.last

        getFSNode(tx, parent).asDirectory.createFile(child, Map(), DataStream(tmpFile.toFile))

        tx.createdFiles.remove(uri)
      }else{
        getFSNode(tx, uri).update(DataStream(tmpFile.toFile))
      }

      tmpFile.toFile.length()
    }finally {
      Files.deleteIfExists(tmpFile)
    }
  }

  def getChildrenNames(tx: ITransaction, uri: String):Array[String] = {
    log.debug("getChildrenNames() {}", uri )

    val file = getFSNode(tx, uri)

    if(file.isDirectory){
      val dir = file.asDirectory

      val groupFilter = if (uri == "/"){
        (name: String) => tx.getPrincipal.groups.contains(name)
      }else{
        (name: String) => true
      }

      dir.children().map(node =>
        cacheNode(tx, node.fullname, node).name
      ).filter(groupFilter).toArray
    }else{
      Array()
    }
  }

  def getResourceLength(tx: ITransaction, uri: String) = {
    log.info("getResourceLength() " + uri)
    getFSNode(tx, uri).versions.last.length
  }

  def removeObject(tx: ITransaction, uri: String) {
    log.info("removeObject " + uri)
    deleteFromCache(tx, uri)
    c3System.deleteFile(translateUri(uri, tx))
  }

  def getStoredObject(tx: ITransaction, uri: String) = {
    log.debug("getStoredObject() {}", uri)

    try{
      val file = getFSNode(tx, uri)

      val storedObject = new StoredObject
      storedObject.setFolder(file.isDirectory)
      storedObject.setMimeType(file.metadata.getOrElse("content.type", "application/octet-stream"))
      storedObject.setResourceLength(file.versions.last.length)
      storedObject.setCreationDate(file.date)
      storedObject.setLastModified(file.versions.last.date)

      storedObject
    }catch{
      case e: C3AccessException => null
      case e: GroupAccessDeniedException => null
    }
  }

  private def getFSNode(tx:ITransaction, uri:String):C3FileSystemNode = {

    val translatedUri = translateUri(uri.replaceAll("/+", "/"), tx)

    tx.cachedFiles.get(translatedUri) match {
      case Some(node) => node
      case None => {
        cacheNode(tx, uri, c3System.getFile(translatedUri))
      }
    }
  }

  private def translateUri(uri:String, tx:ITransaction):String = {

    val splitUri = uri.split("/", 3).filter(!_.isEmpty)

    val translatedUri = splitUri.length match {
      case 0 => "/"
      case 1 => "/" + verifyGroupAccess(splitUri(0), tx) + "/files"
      case _ => "/" + verifyGroupAccess(splitUri(0), tx) + "/files/" + splitUri(1)
    }

    if (log.isDebugEnabled){
      log.debug("URI " + uri + " translated to " + translatedUri)
    }

    translatedUri
  }

  private def verifyGroupAccess(groupId: String, tx: ITransaction): String = {
    if (!tx.getPrincipal.groups.contains(groupId)){
      log.debug("Access denied to group " + groupId + " for principal " + tx.getPrincipal.getName)
      throw new GroupAccessDeniedException
    }else
      groupId
  }

  private def deleteFromCache(tx:ITransaction, uri:String) = {
    tx.cachedFiles.remove(uri.replaceAll("/+", "/"))
  }

  private def cacheNode(tx:ITransaction, uri:String, node:C3FileSystemNode):C3FileSystemNode = {
    tx.cachedFiles.put(uri.replaceAll("/+", "/"), node)
    node
  }

  implicit def txToc3Tx(tx:ITransaction):C3Transaction = {
    tx.asInstanceOf[C3Transaction]
  }

  implicit def principalToC3Princioal(principal: Principal): C3Principal = {
    principal.asInstanceOf[C3Principal]
  }

  def supportsMoveOperation() = true

  def moveResource(tx: ITransaction, source: String, destination: String) {
    log.debug("Called move from " + source + " to " + destination)
    getFSNode(tx, source).move(translateUri(destination, tx))
  }

  class GroupAccessDeniedException extends Exception
}
