package animusic

import scala.io.Source
import scala.util.Try

object Utils {
  def getKey(): Try[String] = {
    Try(Source.fromFile("key").getLines.next)
  }
}