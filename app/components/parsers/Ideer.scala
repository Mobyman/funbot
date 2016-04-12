package components.parsers

import play.api.Logger
import java.lang.Thread.sleep
import java.security.MessageDigest

import models.Story
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scalaj.http.Http
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import com.mongodb.casbah.WriteConcern
import components.bot.IdeerBot


object Ideer {
  val system = ActorSystem("IdeerSystem")
  val actor = system.actorOf(Props(new IdeerActor))

  def run(): Unit = {
    for (i <- 0 until 48525 by 15) {
      actor ! ("parsePage", i)
      sleep(500)
    }
  }

  def incrementDownload() = {
    val localLastStoryId = Story.getLastStoryId()

    val response = Http("http://ideer.ru/").asString
    if (response.code != 200) {
      throw new Exception("Error response code: " + response.body)
    }

    val doc = Jsoup.parse(response.body)
    val stories = doc >> extractor(".secret", secretsExtractor)
    val remoteLastStoryId = stories.toList.head.story_id

    if (remoteLastStoryId > localLastStoryId) {
      val nextStoryId = localLastStoryId + 1
      for (i <- nextStoryId to remoteLastStoryId) {
        actor ! ("parseStory", i)
      }
    }
  }

  class IdeerActor extends Actor {
    def receive = {

      case ("parseStory", story_id: Int) =>
        val response = Http(s"http://ideer.ru/" + story_id.toString + "/ ").asString

        if (response.code == 301) {
          self ! PoisonPill
        }

        if (response.code != 200) {
          actor ! ("parseStory", story_id)
          self ! PoisonPill
        }

        val doc = Jsoup.parse(response.body)
        val stories = doc >> extractor(".secret > .shortContent:not(.closedfix)", secretExtractor)
        stories.map(s => {
          actor ! ("newStory", s.pretty())
          actor ! ("adminNotify", s"[Ideer]: Added\n" + s.pretty)
          Story.save(s, WriteConcern.Safe)
        })

      case ("newStory", story: String) =>
        IdeerBot.sendToSubscribers(story)

      case("adminNotify", text: String) => IdeerBot.adminNotify(text)

      case ("parsePage", page: Int) =>
        val response = Http("http://ideer.ru/").param("page", page.toString).asString

        if (response.code == 301) {
          self ! PoisonPill
        }

        if (response.code != 200) {
          actor ! ("parsePage", page)
          self ! PoisonPill
        }

        val doc = Jsoup.parse(response.body)
        val stories = doc >> extractor(".secret", secretsExtractor)
        stories.map(s => Story.save(s, WriteConcern.Safe))

      case x => Logger.warn("Unexpected message" + x.toString)
    }
  }

  def md5(s: String): String = {
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02x".format(_)).mkString
  }

  def secretsExtractor: Elements => Seq[Story] = {
    _.map {
      elem =>
        val text = elem.text
        val tags = elem.select(".tag").map(_.text).toList
        Story(
          story_id = elem.select(".secret-number-list a").text.replace("#", "").toInt,
          text = text,
          tags = Some(tags),
          hash = md5(text)
        )
    }
  }

  def secretExtractor: Elements => Seq[Story] = {
    _.map {
      elem =>
        val text = elem.text
        val tags = elem.parent().select(".tag").map(_.text).toList
        Story(
          story_id = elem.parent().select(".secret-number").text.replace("#", "").toInt,
          text = text,
          tags = Some(tags),
          hash = md5(text)
        )
    }
  }
}

