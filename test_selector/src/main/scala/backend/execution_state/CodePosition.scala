package backend.execution_state

trait CodePosition
object CodePosition {
  def dummyPosition: CodePosition = SerialCodePosition("", -1)
}

case class SerialCodePosition(file: String, serial: Int) extends CodePosition {
  override def toString: String = s"$serial@$file"
}
