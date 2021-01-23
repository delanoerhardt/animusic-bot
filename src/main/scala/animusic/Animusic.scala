package animusic

import java.awt.Color

import scala.util.{Failure, Success}

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.{JDA, JDABuilder}
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.events.ReadyEvent

import Utils._

object Animusic {
  MessageAction.setDefaultMentionRepliedUser(false)

  def main(args: Array[String]): Unit = {
    val key: String = getKey() match {
      case Failure(exception) =>
        println(exception)
        System.exit(1)
        ""
      case Success(value) => value
    }

    val a: JDA = JDABuilder.createDefault(key).addEventListeners(new ListenerAdapter {
      override def onReady(event: ReadyEvent) = println("Listening")
    }).setActivity(Activity.playing(":3help")).build()
    a.addEventListener(new EventHandler(a.getSelfUser.getIdLong))
  }

  def generateEmbedMessage(nameAndLinksList: List[String]) = {
    var finalMessage = ""
    
    var previousWasLink = false
    var linkNumber = 1

    val embedBuilder = new EmbedBuilder().setColor(new Color(0x66baff))

    nameAndLinksList.foreach((el) => {
      if(!el.startsWith("https://open.spotify.com/")) {
        linkNumber = 1
        previousWasLink = false

        if(finalMessage.length != 0)
          embedBuilder.addField("", finalMessage, false)

        finalMessage = f"${el}%s \u200b \u200b \u200b"
      } else if(!previousWasLink) {
        finalMessage += f" \u200b [[link${linkNumber}%d]](${el}%s)"
 
        previousWasLink = true
        linkNumber += 1
      } else {
        finalMessage += f" \u200b [[link${linkNumber}%d]](${el}%s)"

        linkNumber += 1
      }
    })

    embedBuilder.addField("", finalMessage, false).build()
  }
}