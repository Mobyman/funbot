package components.bot

import com.typesafe.config.ConfigFactory
import components.Analytic
import components.Analytic.Message
import components.parsers.Ideer
import helpers.BaseHelper
import info.mukel.telegram.bots._
import info.mukel.telegram.bots.api.ChatAction
import models.{Story, User}


object IdeerBot extends TelegramBot(Utils.tokenFromFile("./ideer.token")) with Polling with Commands {

  import info.mukel.telegram.bots.OptionPimps._

  val conf = ConfigFactory.load()

  val search = scala.collection.mutable.Map[Int, String]()
  val searchPosition = scala.collection.mutable.Map[Int, Int]()

  def adminNotify(s: String): Unit = {
    sendMessage(conf.getInt("telegram.adminid"), s)
  }

  def sendToSubscribers(s: String): Unit = {
    User.getIdeers.map {
      user => replyTo(user) {
        s
      }
    }
  }

  on("start") { (sender, args) =>
    Analytic.actor ! Message(sender, "start", Map())
    replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
      "Привет! В нашей базе уже *" + Story.count() + "* секретов! \n" +
        "*Подпишись на новые секреты:\n /subscribe*\n"
        "Если хочешь прочитать случайную историю набери\n /random\n" +
        "Хочешь получить историю, зная ее id? Введи \n/get <id истории>\n" +
        "Ищешь историю? Введи \n/search <поисковый запрос>\n например \n*'/search котики'*\n"
    }
  }

  on("random") { (sender, args) =>
    Analytic.actor ! Message(sender, "random", Map())
    sendChatAction(sender, ChatAction.Typing)
    replyTo(sender, disableWebPagePreview = true) {
      Story.findRandom() match {
        case Some(x) => x.pretty()
        case _ => "Ничего на найдено :("
      }
    }
  }

  on("search") { (sender, args) =>
    val searchString = args.mkString(" ")
    Analytic.actor ! Message(sender, "search", Map("q" -> searchString))
    if (searchString.isEmpty || searchString.length < 3) {
      replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
        "Формат сообщения: /search <поисковый запрос> (не менее 4 символов)"
      }

    } else {
      search(sender) = searchString
      searchPosition(sender) = 0

      val message: StringBuilder = StringBuilder.newBuilder
      message.append("*Поиск по запросу: " + search(sender) + "*\n")
      message.append("*Найдено: " + Story.countByText(searchString) + " результатов*\n\n")

      sendChatAction(sender, ChatAction.Typing)

      replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
        Story.findByText(searchString, 0) match {
          case Some(x) => message.append(x.pretty()); message.append("\n\nЧитать далее: /next"); message.mkString
          case _ => "Ничего на найдено :("
        }
      }
    }
  }

  on("reset") { (sender, args) =>
    val isSearch = search.isDefinedAt(sender)
    Analytic.actor ! Message(sender, "reset", Map("success" -> isSearch.toString))
    if (isSearch) {
      search.remove(sender)
      searchPosition.remove(sender)
      replyTo(sender, disableWebPagePreview = true) {
        "Поиск сброшен."
      }
    }
  }


  on("next") { (sender, args) =>
    if (!search.isDefinedAt(sender)) {
      replyTo(sender, disableWebPagePreview = true) {
        "Вначале поищите что-нибудь..."
      }
    }

    searchPosition(sender) += 1
    val searchString = search(sender)
    val pos = searchPosition(sender)
    val max = Story.countByText(searchString)

    Analytic.actor ! Message(sender, "next", Map("search" -> searchString, "pos" -> pos.toString))

    if (pos == max) {
      replyTo(sender, disableWebPagePreview = true) {
        "Больше результатов нет, сбросить условия поиска? /reset"
      }
    }

    replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
      val message: StringBuilder = StringBuilder.newBuilder
      message.append("Поиск по запросу: *" + searchString + s"* ($pos/$max)\n\n")
      Story.findByText(search(sender), pos) match {
        case Some(x) => message.append(x.pretty()); message.append("\n\nЧитать далее: /next"); message.mkString
        case _ => "Ничего на найдено :("
      }
    }
  }

  on("get") { (sender, args) => {
    replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
      BaseHelper.toInt(args.head) match {
        case Some(id) =>
          Story.findByStoryId(id) match {
            case Some(x) =>
              Analytic.actor ! Message(sender, "get", Map("id" -> args.mkString, "success" -> "1"))
              x.pretty()

            case None =>
              Analytic.actor ! Message(sender, "get", Map("id" -> args.mkString, "success" -> "0"))
              "Секрет не найден"
          }
        case None => "Укажите ID секрета"
      }
    }
  }
  }

  on("secret.remove-story") { (sender, args) => {

    replyTo(sender, disableWebPagePreview = true, parseMode = "Markdown") {
      BaseHelper.toInt(args.head) match {
        case Some(id) =>
          Story.removeByStoryId(id)
          "ok"
        case None => "?"
      }
    }
  }
  }

  on("subscribe") { (sender, args) => {
    replyTo(sender) {
      Analytic.actor ! Message(sender, "subscribe", Map("enable" -> "1"))
      User.ideer(sender, flag = true)
      "Подписка оформлена."
    }
  }
  }

  on("unsubscribe") { (sender, args) => {
    Analytic.actor ! Message(sender, "subscribe", Map("enable" -> "0"))
    replyTo(sender) {
      User.ideer(sender, flag = false)
      "Подписка удалена."
    }
  }
  }

  on("secret.id") { (sender, args) =>
    replyTo(sender, disableWebPagePreview = true) {
      sender.toString
    }
  }

  on("secret.update") { (sender, args) =>
    replyTo(sender, disableWebPagePreview = true) {
      Ideer.incrementDownload()
      "ok"
    }
  }


}


