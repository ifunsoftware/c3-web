package org.aphreet.c3.lib.metadata

/**
 * Copyright iFunSoftware 2011
 * @author Dmitry Ivanov
 */
object Metadata {
  val DESCRIPTION_META = "x-c3-description"
  val CONTENT_TYPE = "content-type"
  val ACL_META = "x-c3-acl"
  val ORIGINAL_NAME_META = "x-c3-original-name"
  val TAGS_META: String = "x-c3-tags"
  val MSG_CREATOR_META: String = "x-c3-msg-creator"
  val MSG_DATE_META: String = "x-c3-msg-date"
  val EVENT_CREATOR_META: String = "x-c3-event-creator"
  val EVENT_DATE_META: String = "x-c3-event-data"
  val EVENT_PATH_META: String = "x-c3-event-path"
  val EVENT_TYPE_META: String = "x-c3-event-type"
  val S4_PROCESSED_FLAG_META = "x-s4-meta-processed"
  val OWNER_ID_META = "x-c3-web-owner"
  val GROUP_ID_META = "x-c3-web-group"
  val FS_PATH_META = "c3.ext.fs.path"
  val TAGS_SEPARATOR = ","
  val HASH = "hash"

  val systemKeySet: Set[String] = Set(DESCRIPTION_META, ACL_META, TAGS_META, MSG_CREATOR_META, MSG_DATE_META, S4_PROCESSED_FLAG_META, OWNER_ID_META, GROUP_ID_META, FS_PATH_META, HASH)

  def filterSystemKey(meta: scala.collection.Map[String, String]) = {
    meta.filterNot { case (k, v) => systemKeySet.contains(k) }
  }
}