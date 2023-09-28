package backend.communication.execution_state

import backend.communication.CommonOperations._
import backend.execution_state.TargetTriggeredExecutionState
import spray.json._

private object JsonTargetTriggeredExecutionState extends JsonReader[TargetTriggeredExecutionState] with JsonWriter[TargetTriggeredExecutionState] {
  override def read(jsValue: JsValue): TargetTriggeredExecutionState = {
    val jsObject = jsValueToJsObject(jsValue)
    val eventId = getIntField(jsObject, Fields.eventIdField)
    val processId = getIntField(jsObject, Fields.processIdField)
    TargetTriggeredExecutionState(eventId, processId)
  }

  override def write(ttes: TargetTriggeredExecutionState): JsValue = {
    val fields = Map[String, JsValue](
      JsonParseExecutionState.Fields.typeField -> JsString(
        JsonParseExecutionState.Fields.targetTriggeredExecutionStateType),
      Fields.eventIdField -> JsNumber(ttes.eventId),
      Fields.processIdField -> JsNumber(ttes.processId)
    )
    JsObject(fields)
  }

  object Fields {
    val eventIdField: String = "eventId"
    val processIdField: String = "processId"
  }
}
