package models

import play.api.Play.current
import java.util.Date

import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import se.radley.plugin.salat._
import se.radley.plugin.salat.Binders._
import mongoContext._

import scala.util.matching.Regex

case class Story(
                  id: ObjectId = new ObjectId,
                  story_id: Int,
                  hash: String,
                  text: String,
                  tags: Option[List[String]] = None,
                  created_at: Date = new Date(),
                  updated_at: Option[Date] = None
                ) extends HumanReadable

object Story extends StoryDAO with StoryJson

trait HumanReadable {
  def story_id: Int

  def text: String

  def tags: Option[List[String]]

  def pretty(): String = {
    story_id.toString + "\n\n" + text
  }
}

trait StoryDAO extends ModelCompanion[Story, ObjectId] {
  def collection = mongoCollection("story")

  val dao = new SalatDAO[Story, ObjectId](collection) {}

  // Indexes
  collection.createIndex(DBObject("story_id" -> 1), DBObject("unique" -> true))

  // Criteries
  def getCriteriaForText(text: String): Regex = {
    s"(?i).*$text.*".r
  }

  // Queries
  def findByStoryId(story_id: Int): Option[Story] =
    dao.findOne(MongoDBObject("story_id" -> story_id))

  def removeByStoryId(story_id: Int): Unit =
    dao.remove(MongoDBObject("story_id" -> story_id))

  def findRandom(): Option[Story] = {
    val n = dao.count(MongoDBObject())
    val r = new scala.util.Random
    Some(dao.find(MongoDBObject()).sort(MongoDBObject("story_id" -> -1)).skip(r.nextInt(n.toInt)).limit(1).toList.head)
  }


  def countByText(text: String): Int = {
    //  dao.collection.db.command(MongoDBObject("text" -> "story", "search" -> text, "language" -> "ru"))
    dao.count(MongoDBObject("text" -> getCriteriaForText(text))).toInt
  }

  def count(): Int = {
    dao.count(MongoDBObject()).toInt
  }

  def getLastStoryId(): Int = {
    dao.find(MongoDBObject()).sort(MongoDBObject("story_id" -> -1)).limit(1).toList.head match {
      case s: Story => s.story_id
      case _ => 0
    }
  }

  def findByText(text: String, offset: Option[Int]): Option[Story] = {
    val results = dao.find(MongoDBObject("text" -> getCriteriaForText(text))).sort(MongoDBObject("story_id" -> -1))
    val rq = results.skip(offset.getOrElse {
      val n = countByText(text)
      val r = new scala.util.Random
      r.nextInt(n)
    }).limit(1)

    val list = rq.toList
    Some(list.head)
  }

}


/** Trait used to convert to and from json */
trait StoryJson {

  implicit val storyJsonWrite = new Writes[Story] {
    def writes(s: Story): JsValue = {
      Json.obj(
        "id" -> s.id,
        "story_id" -> s.story_id,
        "hash" -> s.hash,
        "text" -> s.text,
        "tags" -> s.tags,
        "created_at" -> s.created_at,
        "updated_at" -> s.updated_at
      )
    }
  }

  implicit val storyJsonRead = (
    (__ \ 'id).read[ObjectId] ~
      (__ \ 'story_id).read[Int] ~
      (__ \ 'hash).read[String] ~
      (__ \ 'text).read[String] ~
      (__ \ 'tags).readNullable[List[String]] ~
      (__ \ 'created_at).read[Date] ~
      (__ \ 'updated_at).readNullable[Date]) (Story.apply _)

}