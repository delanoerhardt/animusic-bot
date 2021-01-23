package animusic

import java.io.StringWriter

import javax.json.Json
import javax.json.stream.JsonParser

import okhttp3.{OkHttpClient, MediaType, RequestBody, Request, Response}

object SongRequest {
  val client = new OkHttpClient()

  val MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8")

  val url: String = "https://p4b7ht5p18-1.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser%20(lite)&x-algolia-application-id=P4B7HT5P18&x-algolia-api-key=cd90c9c918df8b42327310ade1f599bd"

  def getRequest(query: String, page: Int): Request = {
    new Request.Builder()
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
      .post(getBody(query, page))
      .build()
  }

  def requestSongs(query: String, page: Int, callbackHandler: CallbackHandler) = {
    client.newCall(getRequest(query, page)).enqueue(callbackHandler)
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

    RequestBody.create(MEDIA_TYPE_JSON, jsonString)
  }

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
}