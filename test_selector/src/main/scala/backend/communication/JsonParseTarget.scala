package backend.communication

import spray.json.{JsNumber, JsObject, JsValue, JsonReader, JsonWriter}

object JsonParseTarget
  extends JsonWriter[(Int, Int)]
    with JsonReader[(Int, Int)] {

  override def write(tuple: (Int, Int)): JsValue = {
    val fields = Map(EventWithIdFields.processIdField -> JsNumber(tuple._1), EventWithIdFields.targetIdField -> JsNumber(tuple._2))
    JsObject(fields)
  }
  override def read(jsValue: JsValue): (Int, Int) = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val processId = CommonOperations.getIntField(jsObject, EventWithIdFields.processIdField)
    val targetId = CommonOperations.getIntField(jsObject, EventWithIdFields.targetIdField)
    (processId, targetId)
  }
}
