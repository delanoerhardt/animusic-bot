package animusic

import scala.concurrent.Future

import com.redis._

object RedisInterface {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val redisClients = new RedisClientPool("localhost", 6379)

  val TIME_TO_EXPIRE_WITHOU_INTERACTION: Int = 30

  val TIME_TO_EXPIRE_QUERIES: Int = 180

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