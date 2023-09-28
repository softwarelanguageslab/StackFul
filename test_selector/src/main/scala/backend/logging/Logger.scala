package backend.logging

object Logger extends LogAction[String] {
  protected var logLevel: LogLevel = Error
  def setLogLevel(newLevel: LogLevel): Unit = {
    logLevel = newLevel
  }
  protected def doAction(s: => String): Unit = {
    println(s)
  }
}
