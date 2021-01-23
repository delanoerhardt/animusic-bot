package animusic

import java.awt.Color

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.EmbedBuilder

class DiscordEventHandler(val id: Long) extends ListenerAdapter {
  val prefix = ":3"

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if(this.id == event.getAuthor.getIdLong || !event.getMessage.getContentDisplay.startsWith(prefix)) {
      return
    }

    event.getMessage.getContentDisplay.split(prefix).drop(1).foldLeft("")(_ + _).filterNot("/{}\"".toSet) match {
      case "help" =>
        event.getChannel.sendMessage("Type :3 followed by the name of the anime you wish to search musics of, ie: \":3one piece\"")
          .reference(event.getMessage).queue()
      case name: String =>
        val callbackHandler = new CallbackHandler()
        
        event.getChannel
          .sendMessage(new EmbedBuilder().setColor(new Color(0x66baff)).addField("", s"Fetching songs for ${name}...", false).build)
          .reference(event.getMessage)
          .queue(callbackHandler.onMessageSent)
    
        SongRequest.requestSongs(name, 0, callbackHandler)
      case _ =>
    }
  }

  override def onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent): Unit = {
    if(this.id == event.getUserIdLong())
      return

    var deltaPage = 0

    event.getReactionEmote.getName match {
      case "👉" => deltaPage = +1
      case "👈" => deltaPage = -1
      case _ => return
    }

    val callbackHandler = new CallbackHandler()
    callbackHandler.channel = event.getChannel
    callbackHandler.messageId = event.getMessageIdLong

    val oldQueryPage = RedisInterface.getMessageQuery(event.getMessageId).getOrElse(null)

    if(oldQueryPage == null)
      return

    val quantity = RedisInterface.getEntryAmmount(event.getMessageId).getOrElse("12").toInt
    
    if(quantity < 12 && deltaPage == +1)
      return
    
    val queryAndPage = oldQueryPage.split(":")

    if((queryAndPage(1).toInt + deltaPage).max(0) == queryAndPage(1).toInt)
      return

    val nextPageList = (queryAndPage(0), (queryAndPage(1).toInt + deltaPage).max(0))

    val nextPageKey = s"${nextPageList._1}:${nextPageList._2}"

    val cachedQueryResult = RedisInterface.getCachedQuery(nextPageKey).getOrElse(null)

    if(cachedQueryResult == null || cachedQueryResult.length == 0) {
      SongRequest.requestSongs(nextPageList._1, nextPageList._2, callbackHandler)
      return
    }

    callbackHandler.entryAmmount = cachedQueryResult.head.getOrElse("0").toInt
    
    if(callbackHandler.entryAmmount == 0)
      return

    callbackHandler.nameAndLinksList = (for {Some(a) <- cachedQueryResult.tail} yield a)
    callbackHandler.query = nextPageList._1
    callbackHandler.page = nextPageList._2

    callbackHandler.sendEmbedWithSongs()

    RedisInterface.updateExpire(oldQueryPage)
  }
}