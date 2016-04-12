package components

import scalaj.http.{Http, HttpOptions}
import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object Analytic {
  val conf = ConfigFactory.load()

  case class Message(uid: Int, command: String, message: Map[String, String])
  val token = conf.getString("analytic.token")


  class AnalyticActor extends Actor {
    def receive = {
      case m: Message => Analytic.send(m.uid, m.command, m.message)
    }
  }

  val system = ActorSystem("AnalyticSystem")
  val actor = system.actorOf(Props(new AnalyticActor))

  def send(uid: Int, command: String, body: Map[String, String]): Unit = {
    Http(s"https://api.botan.io/track?token=$token&uid=$uid&name=$command")
      .postData(scala.util.parsing.json.JSONObject(body).toString())
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.readTimeout(10000)).execute()
  }
}
