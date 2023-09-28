package backend.communication.execution_state

import backend.communication._
import backend.execution_state.SerialCodePosition
import spray.json._

trait JsonParseCodePosition[T] {
  def convertCodePosition(jsValue: JsValue): T
}

object CodePositionFields {
  val serialField: String = "_serial"
  val fileField: String = "_file"
}

object JsonParseSerialCodePosition
  extends JsonParseCodePosition[SerialCodePosition]
    with JsonReader[SerialCodePosition]
    with JsonWriter[SerialCodePosition] {

  import CodePositionFields._
  import CommonOperations._

  override def read(jsValue: JsValue): SerialCodePosition = convertCodePosition(jsValue)
  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def convertCodePosition(jsValue: JsValue): SerialCodePosition = {
    val jsObject = jsValueToJsObject(jsValue)
    val serial = getIntField(jsObject, serialField)
    val file = getStringField(jsObject, fileField)
    SerialCodePosition(file, serial)
  }
  override def write(position: SerialCodePosition): JsValue = {
    val fields = Map(fileField -> JsString(position.file), serialField -> JsNumber(position.serial))
    JsObject(fields)
  }
}
