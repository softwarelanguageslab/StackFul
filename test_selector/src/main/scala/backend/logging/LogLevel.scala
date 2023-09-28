package backend.logging

sealed abstract class LogLevel(val level: Int) {
  def >(other: LogLevel): Boolean = {
    this.level > other.level
  }
  def ==(other: LogLevel): Boolean = {
    this.level == other.level
  }
  def >=(other: LogLevel): Boolean = {
    this.level >= other.level
  }
}
case object Debugging extends LogLevel(0)
case object Verbose extends LogLevel(1)
case object Normal extends LogLevel(2)
case object Error extends LogLevel(3)

trait LogAction[T] {
  def d(x: => T): Unit = doLog(Debugging, x)
  def v(x: => T): Unit = doLog(Verbose, x)
  def n(x: => T): Unit = doLog(Normal, x)
  protected def doLog(messageLevel: LogLevel, x: => T): Unit = {
    if (messageLevel >= logLevel) {
      doAction(x)
    }
  }
  def e(x: => T): Unit = doLog(Error, x)
  protected def logLevel: LogLevel
  protected def doAction(x: => T): Unit

}