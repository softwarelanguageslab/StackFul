package backend.execution_state

sealed trait FunctionId
case class SerialFunctionId(serial: Int) extends FunctionId {
  override def toString: String = serial.toString
}
case class CallArrivalFunctionPair(callSerial: Option[Int], arrivalSerial: Int) extends FunctionId {
  override def toString: String = {
    s"{${callSerial.getOrElse("∅")};$arrivalSerial}"
  }
}