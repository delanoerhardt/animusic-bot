package animusic

import java.io.StringWriter
import java.awt.Color

import javax.json.Json

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.EmbedBuilder

import okhttp3.{RequestBody, Request}

class EventHandler(val id: Long) extends ListenerAdapter {

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if(this.id == event.getAuthor.getIdLong || !event.getMessage.getContentDisplay.startsWith(";-;")) {
      return
    }

    val command = event.getMessage.getContentDisplay.split(";-;").drop(1).foldLeft("")(_ + _)

    println(s"Command ${command} from ${event.getAuthor.getName} received in ${event.getGuild.getName}")

    command match {
      case "die" =>
        event.getJDA.shutdownNow()
      case query: String =>
        val name = query.filterNot("/{}\"".toSet)
        val callbackHandler = new CallbackHandler()
        
        event.getChannel
          .sendMessage(new EmbedBuilder().setColor(new Color(0x66baff)).addField("", s"Fetching songs for ${name}...", false).build)
          .reference(event.getMessage)
          .queue(callbackHandler.onMessageSent)

        EventHandler.requestSongs(name, 0, callbackHandler)
    }
  }

  override def onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent): Unit = {
    if(this.id == event.getUserIdLong())
      return

    var deltaPage = 0

    event.getReactionEmote.getName match {
      case "👉" => deltaPage = +1
      case "👈" => deltaPage = -1 // TODO vai dar erro qnd tiver em 0
      case _ => return
    }

    val callbackHandler = new CallbackHandler()
    callbackHandler.channel = event.getChannel
    callbackHandler.messageId = event.getMessageIdLong

    Animusic.getMessageQuery(event.getMessageId) match {
      case Some(oldQueryPage) => {

        val queryAndPage = oldQueryPage.split(":")

        val nextPageList = (queryAndPage(0), (queryAndPage(1).toInt + deltaPage).max(0))

        if((queryAndPage(1).toInt + deltaPage).max(0) == queryAndPage(1).toInt)
          return

        val nextPageKey = s"${nextPageList._1}:${nextPageList._2}"

        Animusic.getCachedQuery(nextPageKey) match {
          case Some(cachedQueryResult) if(cachedQueryResult.length != 0) => {
            
            callbackHandler.nameAndLinksList = (for {Some(a) <- cachedQueryResult} yield a)
            callbackHandler.query = nextPageList._1
            callbackHandler.page = nextPageList._2

            callbackHandler.sendEmbedWithSongs()
          }
          case _ => {
            EventHandler.requestSongs(nextPageList._1, nextPageList._2, callbackHandler)
          }
        }

        Animusic.updateExpire(oldQueryPage)
      }
      case _ => return
    }
  }
}

object EventHandler {

  def url: String = {
    "https://p4b7ht5p18-1.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser%20(lite)&x-algolia-application-id=P4B7HT5P18&x-algolia-api-key=cd90c9c918df8b42327310ade1f599bd"
  }

  def getRequest(query: String, page: Int): Request = {
    new Request.Builder()
      .url(EventHandler.url)
      .header("Accept", "application/json")
      .header("Accept-Language", "en-US,en;q=0.9")
      .header("Connection", "keep-alive")
      .header("Content-Type", "application/json")
      .header("Origin", "https://aniplaylist.com")
      .header("Referer", "https://aniplaylist.com/")
      .header("Sec-Fetch-Dest", "empty")
      .header("Sec-Fetch-Mode", "cors")
      .header("Sec-Fetch-Site", "cross-site")
      .header("Sec-GPC", "1")
      .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.101 Safari/537.36")
      .post(EventHandler.getBody(query, page))
      .build()
  }

  def requestSongs(query: String, page: Int, callbackHandler: CallbackHandler) = {
    Animusic.client.newCall(EventHandler.getRequest(query, page)).enqueue(callbackHandler)
  }

  def getBody(name: String, page: Int): RequestBody = {
    val writer = new StringWriter
    val jsonGenerator = Json.createGenerator(writer)
    jsonGenerator
      .writeStartObject()
        .writeStartArray("requests")
          .writeStartObject()
            .write("indexName", "songs_prod")
            .write("params", s"query=${name.replace(" ", "%20")}&hitsPerPage=12&maxValuesPerFacet=100&page=${page}&highlightPreTag=__ais-highlight__&highlightPostTag=__%2Fais-highlight__&facets=%5B%22song_type%22%2C%22markets%22%2C%22season%22%2C%22label%22%5D&tagFilters=")
          .writeEnd()
        .writeEnd()
      .writeEnd().close()
    
    val jsonString = writer.toString()

    writer.close()

    RequestBody.create(Animusic.MEDIA_TYPE_JSON, jsonString)
  }
}