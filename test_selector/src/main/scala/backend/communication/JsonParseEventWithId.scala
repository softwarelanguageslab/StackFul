package backend.communication

import spray.json.{JsNumber, JsObject, JsValue, JsonReader, JsonWriter}

object EventWithIdFields {
  val idField: String = "id"
  val processIdField: String = "processId"
  val targetIdField: String = "targetId"
}

object JsonParseEventWithId extends JsonReader[(Int, Int, Int)] with JsonWriter[(Int, Int, Int)] {

  override def write(tuple: (Int, Int, Int)): JsValue = {
    val fields = Map(
      EventWithIdFields.idField -> JsNumber(tuple._1),
      EventWithIdFields.processIdField -> JsNumber(tuple._2),
      EventWithIdFields.targetIdField -> JsNumber(tuple._3))
    JsObject(fields)
  }
  override def read(jsValue: JsValue): (Int, Int, Int) = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val id = CommonOperations.getIntField(jsObject, EventWithIdFields.idField)
    val processId = CommonOperations.getIntField(jsObject, EventWithIdFields.processIdField)
    val targetId = CommonOperations.getIntField(jsObject, EventWithIdFields.targetIdField)
    (id, processId, targetId)
  }
}
