package backend.communication.execution_state

import backend.communication._
import backend.execution_state._

import spray.json._

object JsonTargetEndExecutionState
  extends JsonReader[TargetEndExecutionState]
  with JsonWriter[TargetEndExecutionState] {

  import CommonOperations._
  import Fields._
  import JsonParseExecutionState.Fields._

  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  override def read(jsValue: JsValue): TargetEndExecutionState = {
    val jsObject = jsValueToJsObject(jsValue)
    val id = getIntField(jsObject, idField)
    TargetEndExecutionState(id)
  }
  override def write(state: TargetEndExecutionState): JsValue = {
    val jsId = JsNumber(state.id)
    val jsType = JsString(targetEndExecutionStateType)
    val fields = Map(typeField -> jsType, idField -> jsId)
    JsObject(fields)
  }
  object Fields {
    val idField: String = "_id"
  }
}
