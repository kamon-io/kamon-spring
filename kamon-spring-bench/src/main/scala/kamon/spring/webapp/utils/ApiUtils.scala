package kamon.spring.webapp.utils

trait ApiUtils {

  def withDelay[A](timeInMillis: Long)(thunk: => A): A = {
    if (timeInMillis > 0) Thread.sleep(timeInMillis)
    thunk
  }
}

object ApiUtils {
  val defaultDuration: Long = 1000 // millis
}
