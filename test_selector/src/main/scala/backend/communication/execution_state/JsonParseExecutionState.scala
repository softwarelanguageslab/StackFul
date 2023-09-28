package backend.communication.execution_state

import backend.communication._
import backend.execution_state._
import spray.json._

object JsonParseExecutionState
  extends JsonReader[ExecutionState]
  with JsonWriter[ExecutionState] {

  import Fields._
  import backend.communication.CommonOperations._

  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  override def read(jsValue: JsValue): ExecutionState = {
    val jsObject = jsValueToJsObject(jsValue)
    val stateType = getStringField(jsObject, typeField)
    stateType match {
      case Fields.basicExecutionStateType => JsonParseCodeLocationExecutionState.read(jsValue)
      case Fields.targetEndExecutionStateType => JsonTargetEndExecutionState.read(jsValue)
      case Fields.targetTriggeredExecutionStateType => JsonTargetTriggeredExecutionState.read(jsValue)
    }
  }

  override def write(executionState: ExecutionState): JsValue = executionState match {
    case es: CodeLocationExecutionState => JsonParseCodeLocationExecutionState.write(es)
    case es: TargetTriggeredExecutionState => JsonTargetTriggeredExecutionState.write(es)
    case es: TargetEndExecutionState => JsonTargetEndExecutionState.write(es)
  }
  object Fields {
    val basicExecutionStateType: String = "BASIC"
    val targetEndExecutionStateType: String = "TARGET_END"
    val targetTriggeredExecutionStateType: String = "TARGET_TRIGGERED"

    val typeField: String = "_type"
  }
}
