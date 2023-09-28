package backend.communication

import spray.json._

class JsonParseSequence[T : JsonReader : JsonWriter]
  extends JsonWriter[List[T]]
    with JsonReader[List[T]] {

  def write(events: List[T]): JsValue = {
    JsArray(events.map(implicitly[JsonWriter[T]].write).toVector)
  }
  def read(jsValue: JsValue): List[T] = {
    val jsArray = CommonOperations.jsValueToJsArray(jsValue)
    jsArray.elements.map(implicitly[JsonReader[T]].read).toList
  }
}
