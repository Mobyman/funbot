package models

import play.api.Play.current
import java.util.Date
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import se.radley.plugin.salat._
import se.radley.plugin.salat.Binders._
import mongoContext._

case class User(
  id: ObjectId = new ObjectId,
  chat_id: Int,
  ideer: Boolean,
  created_at: Date = new Date(),
  updated_at: Option[Date] = None
)

object User extends UserDAO with UserJson

trait UserDAO extends ModelCompanion[User, ObjectId] {
  def collection = mongoCollection("user")

  val dao = new SalatDAO[User, ObjectId](collection) {}

  // Indexes
  collection.createIndex(DBObject("ideer" -> 1))
  collection.createIndex(DBObject("chat_id" -> 1), DBObject("unique" -> true))

  def ideer(chat_id: Int, flag: Boolean) = {
    User.update(
      DBObject("chat_id" -> chat_id),
      DBObject("$set" -> DBObject("ideer" -> flag, "updated_at" -> new Date())),
      upsert=true, multi=false, WriteConcern.Safe
    )
  }

  def getIdeers: Set[Int] = {
    User.find(DBObject("ideer" -> true)).map(x => x.chat_id).toSet
  }

}


/** Trait used to convert to and from json */
trait UserJson {

  implicit val userJsonWrite = new Writes[User] {
    def writes(s: User): JsValue = {
      Json.obj(
        "id" -> s.id,
        "chat_id" -> s.chat_id,
        "ideer" -> s.ideer,
        "created_at" -> s.created_at,
        "updated_at" -> s.updated_at
      )
    }
  }

  implicit val userJsonRead = (
    (__ \ 'id).read[ObjectId] ~
      (__ \ 'chat_id).read[Int] ~
      (__ \ 'ideer).read[Boolean] ~
      (__ \ 'created_at).read[Date] ~
      (__ \ 'updated_at).readNullable[Date]) (User.apply _)

}