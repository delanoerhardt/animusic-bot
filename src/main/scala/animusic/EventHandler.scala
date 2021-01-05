package animusic

import java.io.StringWriter

import javax.json.Json

import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.events.ReadyEvent

import okhttp3.{OkHttpClient, RequestBody, Request, MediaType}

class EventHandler(val id: String) extends ListenerAdapter {

  MessageAction.setDefaultMentionRepliedUser(false)

  def url: String = {
    "https://p4b7ht5p18-1.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser%20(lite)&x-algolia-application-id=P4B7HT5P18&x-algolia-api-key=cd90c9c918df8b42327310ade1f599bd"
  }

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if(this.id == event.getAuthor.getId || !event.getMessage.getContentDisplay.startsWith(";-;"))
      return

    val command = event.getMessage.getContentDisplay.split(";-;").tail.foldLeft("")(_ + _)

    println(s"Command ${command} from ${event.getAuthor.getName} received in ${event.getGuild.getName}")

    command match {
      case "die" =>
        event.getJDA.shutdown()
      case anime: String =>
        EventHandler.getBody("Vtnc")
        val request = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .header("Accept-Encoding", "gzip, deflate, br")
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
          .post(EventHandler.getBody("Vtnc"))
        event.getChannel.sendMessage(anime).reference(event.getMessage).queue
    }
  }
}

object EventHandler {
  private val client = new OkHttpClient()

  private val MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8")

  def getBody(x: String): RequestBody = {
    //{"requests": [{"indexName": "songs_prod","params": "query=one%20piece&hitsPerPage=12&maxValuesPerFacet=100&page=0&highlightPreTag=__ais-highlight__&highlightPostTag=__%2Fais-highlight__&facets=%5B%22song_type%22%2C%22markets%22%2C%22season%22%2C%22label%22%5D&tagFilters="}]}
    val writer = new StringWriter
    val gen = Json.createGenerator(writer)
    gen.writeStartObject().writeEnd().close()
    println(writer.toString())


    RequestBody.create(this.MEDIA_TYPE_JSON, writer.toString())
  }
}