package animusic

import java.io.StringWriter

import javax.json.Json
import javax.json.stream.JsonParser

import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.events.ReadyEvent

import okhttp3.{OkHttpClient, RequestBody, Request, MediaType, Callback, Call, Response}
import java.io.IOException

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
        val request = new Request.Builder()
          .url(url)
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
          .post(EventHandler.getBody(anime, 0))
          .build()

        EventHandler.client.newCall(request).enqueue(new Callback {
          override def onFailure(call: Call, e: IOException) = {
            e.printStackTrace()
          }

          override def onResponse(call: Call, response: Response): Unit = {
            for(i <- (0 until response.headers.size).toList)
              println(s"${response.headers.name(i)}: ${response.headers.value(i)}")

            /* println(response.body.string())
            response.close()
            return */


            val jsonParser = Json.createParser(response.body.byteStream())

            var nameLinkList: List[String] = List()

            var readTitleOrLink = false
            while(jsonParser.hasNext) {
              val event = jsonParser.next
              event match {
                case JsonParser.Event.KEY_NAME =>
                  if(jsonParser.getString().equals("titles") || jsonParser.getString.equals("spotify_url"))
                    readTitleOrLink = true
                case JsonParser.Event.VALUE_STRING if(readTitleOrLink) =>
                  readTitleOrLink = false
                  println(jsonParser.getString())
                  nameLinkList = nameLinkList :+ (jsonParser.getString)
                case _ =>
              }
            }

            var finalMessage = ""
            var previousWasLink = true
            nameLinkList.foreach((el) => {
              if(!el.startsWith("https://open.spotify.com/track/")) {
                previousWasLink = false
                finalMessage += el
              }
              else if(!previousWasLink) {
                previousWasLink = true
                finalMessage += s"\t\t${el}\n"
              }
            })
            println(finalMessage)
            event.getChannel.sendMessage(finalMessage).reference(event.getMessage).queue

            response.close()
          }
        })
    }
  }
}

object EventHandler {
  private val client = new OkHttpClient()

  private val MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8")

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

    RequestBody.create(this.MEDIA_TYPE_JSON, jsonString)
  }
}