package animusic

import java.io.IOException

import scala.concurrent.Future

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import okhttp3.{Callback, Call, Response}

class CallbackHandler extends Callback {
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
      val (nameAndLinksList, query, page, entryAmmount) = SongRequest.parseJsonResponse(response)

      response.body.close()
      response.close()
      
      RedisInterface.storeQueryResult(s"${query}:${page}", nameAndLinksList, entryAmmount)

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
    
    RedisInterface.storeMessageQuery(this.messageId.toString, s"${this.query}:${this.page}", this.entryAmmount)

    val embedMessage = Animusic.generateEmbedMessage(nameAndLinksList)

    channel.editMessageById(messageId, embedMessage)
      .queue(
        _.addReaction("👈").queue(
          _ => channel.addReactionById(messageId, "👉").queue()
        )
      )
  }
}