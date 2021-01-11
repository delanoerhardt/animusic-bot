package animusic

import Utils.getKey

import scala.util.Failure
import scala.util.Success

object Main {
  def main(args: Array[String]): Unit = {
    val key: String = getKey() match {
      case Failure(exception) =>
        println(exception)
        System.exit(1)
        ""
      case Success(value) => value
    }

    Animusic.run(key)
  }
}