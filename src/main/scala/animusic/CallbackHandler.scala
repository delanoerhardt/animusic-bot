package animusic

import java.io.IOException
import java.awt.Color

import scala.concurrent.Future

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.EmbedBuilder

import javax.json.Json
import javax.json.stream.JsonParser

import okhttp3.{Callback, Call, Response}

class CallbackHandler() extends Callback {
  var response: Response = null
  var channel: MessageChannel = null
  var messageId: Long = 0L
  var query: String = null
  var page: Int = 0
  var entryAmmount: Int = 0

  var nameAndLinksList: List[String] = List()

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def onFailure(call: Call, e: IOException) = {
    e.printStackTrace()
  }

  override def onResponse(call: Call, response: Response): Unit = {
    this.synchronized { 
      this.response = response
      val (nameAndLinksList, query, page, entryAmmount) = CallbackHandler.parseJsonResponse(response)

      response.body.close()
      response.close()
      
      Animusic.storeQueryResult(s"${query}:${page}", nameAndLinksList, entryAmmount)

      if(nameAndLinksList.length == 0)
        return
      
      this.query = query
      this.page = page
      this.entryAmmount = entryAmmount
      
      this.nameAndLinksList = nameAndLinksList
      
      if(this.channel != null) {
        sendEmbedWithSongs()
      }
    }
  }
  
  def onMessageSent(message: Message): Unit = {
    this.synchronized {
      this.channel = message.getChannel
      this.messageId = message.getIdLong

      this.page = 0
      
      if(this.response != null) {
        sendEmbedWithSongs()
      }
    }
  }

  def sendEmbedWithSongs(): Unit = {

    if(this.nameAndLinksList.length == 0)
      return
    
    Animusic.storeMessageQuery(this.messageId.toString, s"${this.query}:${this.page}", this.entryAmmount)

    val embedMessage = CallbackHandler.getEmbedMessage(nameAndLinksList)

    channel.editMessageById(messageId, embedMessage)
      .queue(
        _.addReaction("👈").queue(
          _ => channel.addReactionById(messageId, "👉").queue()
        )
      )
  }
}

object CallbackHandler {

  // TODO store title ammount per page and use it to avoid requesting an empty page
  def parseJsonResponse(response: Response): (List[String], String, Int, Int) = {
    val jsonParser = Json.createParser(response.body.byteStream())

    var nameAndLinksList: List[String] = List()

    var page = -1
    var query = ""
    var entryAmmount = 0

    var readTitleOrLink = false
    var ignoreNextTitles = false
    var readPageNumber = false
    var readQuery = true
    while(jsonParser.hasNext) {
      val event = jsonParser.next
      event match {
        case JsonParser.Event.KEY_NAME =>
        jsonParser.getString() match {
          case "titles" => 
            readTitleOrLink = !ignoreNextTitles
            ignoreNextTitles = false
            
            if(readTitleOrLink)
              entryAmmount += 1
          case "spotify_url" =>
            readTitleOrLink = true
          case "title" =>
            readTitleOrLink = true
            ignoreNextTitles = true
            
            entryAmmount += 1
          case "page" =>
            readPageNumber = true
          case "query" =>
            readQuery = true
          case _ =>
        }
        case JsonParser.Event.VALUE_STRING if(readTitleOrLink) =>
          readTitleOrLink = false
          nameAndLinksList = nameAndLinksList :+ (jsonParser.getString)
        case JsonParser.Event.VALUE_STRING if(readQuery) =>
          readQuery = false
          query = jsonParser.getString
        case JsonParser.Event.VALUE_NUMBER if (readPageNumber) =>
          readPageNumber = false
          page = jsonParser.getInt
        case _ =>
      }
    }

    jsonParser.close()

    (nameAndLinksList, query, page, entryAmmount)
  }

  def getEmbedMessage(nameAndLinksList: List[String]) = {
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