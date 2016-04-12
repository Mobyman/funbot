import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import components.bot.IdeerBot
import components.parsers.Ideer
import play.api._

import scala.concurrent.duration.{Duration, DurationInt}
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ReminderActor extends Actor {
  def receive = {
    case "update-ideer" =>
      Logger.info("Update ideer")
      Ideer.incrementDownload()
  }
}

object Global extends GlobalSettings {
  def reminderDaemon() = {
    Logger.info("Reminder daemon start")
    val system = ActorSystem("ScheduleSystem")
    val reminderActor = system.actorOf(Props(new ReminderActor()))
    system.scheduler.schedule(Duration.Zero, 5 minute, reminderActor, "update-ideer")
  }

  override def onStart(app: Application) {
    reminderDaemon()
    IdeerBot.run()
  }

}