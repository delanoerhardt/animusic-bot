package animusic

import scala.concurrent.Future

import net.dv8tion.jda.api.{JDA, JDABuilder}
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction

import com.redis._

import okhttp3.{OkHttpClient, MediaType}

object Animusic {
  MessageAction.setDefaultMentionRepliedUser(false)

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val redisClients = new RedisClientPool("localhost", 6379)

  val client = new OkHttpClient()

  val MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8")
  
  val TIME_TO_EXPIRE_INTERACTION: Int = 30

  val TIME_TO_EXPIRE_QUERIES: Int = 180

  def run(key: String) = {
    val a: JDA = JDABuilder.createDefault(key).addEventListeners(new ListenerAdapter {
      override def onReady(event: ReadyEvent) = println("Listening")
    }).build()
    a.addEventListener(new EventHandler(a.getSelfUser.getIdLong))
  }

  def storeQueryResult(key: String, list: List[String]) = Future {
    redisClients.withClient {
      client => {
        list.foreach { client.rpush(key, _) }
        client.expire(key, TIME_TO_EXPIRE_QUERIES)
      }
    }
  }

  def storeMessageQuery(key: String, queryPage: String) = Future {
    redisClients.withClient {
      client => {
        client.setex(key, TIME_TO_EXPIRE_INTERACTION, queryPage)
      }
    }
  }
  
  def updateExpire(oldQueryPage: String) = Future {
    redisClients.withClient {
      client => {
        client.expire(oldQueryPage, TIME_TO_EXPIRE_QUERIES)
      }
    }
  }

  def getMessageQuery(messageId: String) = {
    redisClients.withClient {
      client => {
        client.get(messageId)
      }
    }
  }

  def getCachedQuery(nextPageKey: String) = {
    redisClients.withClient {
      client => {
        client.lrange(nextPageKey, 0, -1)
      }
    }
  }
}