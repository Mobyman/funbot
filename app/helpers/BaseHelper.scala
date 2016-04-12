package helpers

/**
  * Created by mobyman on 4/2/16.
  */
object BaseHelper {

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

}
