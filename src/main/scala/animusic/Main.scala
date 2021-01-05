package animusic

import Utils.getKey

import net.dv8tion.jda.api.{JDA, JDABuilder}

import scala.util.Failure
import scala.util.Success

object Main {
  def main(args: Array[String]): Unit = {
    val key: String = getKey() match {
      case Failure(exception) =>
        print(exception)
        System.exit(1)
        ""
      case Success(value) => value
    }
    
    val a: JDA = JDABuilder.createDefault(key).build()
    a.addEventListener(new EventHandler(a.getSelfUser.getId))
  }
}