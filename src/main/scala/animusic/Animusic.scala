package animusic

import scala.concurrent.Future

import scala.util.Failure
import scala.util.Success

import net.dv8tion.jda.api.{JDA, JDABuilder}
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction

import com.redis._

import okhttp3.{OkHttpClient, MediaType}

import Utils._

object Animusic {
  MessageAction.setDefaultMentionRepliedUser(false)

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val redisClients = new RedisClientPool("localhost", 6379)

  val client = new OkHttpClient()

  val MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8")
  
  val TIME_TO_EXPIRE_WITHOU_INTERACTION: Int = 30

  val TIME_TO_EXPIRE_QUERIES: Int = 180

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
    }).build()
    a.addEventListener(new EventHandler(a.getSelfUser.getIdLong))
  }

  def storeQueryResult(key: String, list: List[String], entryAmmount: Int) = Future {
    redisClients.withClient {
      client => {
        client.rpush(key, entryAmmount)
        list.foreach { client.rpush(key, _) }
        client.expire(key, TIME_TO_EXPIRE_QUERIES)
      }
    }
  }

  def storeMessageQuery(messageId: String, queryPage: String, entryAmmount: Int) = Future {
    redisClients.withClient {
      client => {
        client.setex(messageId, TIME_TO_EXPIRE_WITHOU_INTERACTION, queryPage)
        client.setex(s"${messageId}_entryAmmount", TIME_TO_EXPIRE_WITHOU_INTERACTION, entryAmmount.toString())
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

  def getEntryAmmount(messageId: String) = {
    redisClients.withClient {
      client => {
        client.get(s"${messageId}_entryAmmount")
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