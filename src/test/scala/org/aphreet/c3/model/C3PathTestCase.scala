package org.aphreet.c3.model

import junit.framework.TestCase
import junit.framework.Assert._

class C3PathTestCase extends TestCase{

  def testWikiParse(){

    val path = "/groupName/wiki/Main"

    val c3Path = C3Path(path)

    assertEquals(WikiType, c3Path.resourceType)
    assertEquals("Main", c3Path.resourceName)
    assertEquals("groupName", c3Path.groupName)
    assertEquals("/group/groupName/wiki/Main", c3Path.resourceUri)
  }

  def testFileParse(){
    val path = "/groupName/files/directory1/directory2/file.jpg"

    val c3Path = C3Path(path)

    assertEquals(FileType, c3Path.resourceType)
    assertEquals("directory1/directory2/file.jpg", c3Path.resourceName)
    assertEquals("groupName", c3Path.groupName)
    assertEquals("/group/groupName/files/directory1/directory2/file.jpg", c3Path.resourceUri)
  }
}
