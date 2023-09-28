package backend.communication.execution_state

import backend.communication._
import backend.execution_state._
import spray.json._

object JsonParseCodeLocationExecutionState
  extends JsonReader[CodeLocationExecutionState]
    with JsonWriter[CodeLocationExecutionState] {

  override def read(jsValue: JsValue): CodeLocationExecutionState = convertExecutionState(jsValue)

  import CommonOperations._
  import Fields._
  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  def convertExecutionState(jsValue: JsValue): CodeLocationExecutionState = {
    val jsObject = jsValueToJsObject(jsValue)
    val jsCodePosition = getField(jsObject, codePositionField)
    val codePosition = implicitly[JsonParseCodePosition[SerialCodePosition]].convertCodePosition(jsCodePosition)
    val jsFunctionStack = getField(jsObject, functionStackField)
    val functionStack = JsonParseFunctionStack.convertFunctionStack(jsFunctionStack)
    val jsCurrentEvent = getField(jsObject, currentEventField)
    val currentEvent = readOptTarget(jsCurrentEvent)
    val stackLength = getIntField(jsObject, stackLengthField)
    CodeLocationExecutionState(codePosition, functionStack, currentEvent, stackLength)
  }
  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  private def readOptTarget(jsCurrentEvents: JsValue): Option[(Int, Int, Int)] = {
    if (jsCurrentEvents == JsNull) {
      None
    } else {
      val invokedEvents = implicitly[JsonReader[List[(Int, Int, Int)]]].read(jsCurrentEvents)
      invokedEvents.lastOption
    }
  }
  override def write(state: CodeLocationExecutionState): JsValue = {
    val jsCodePosition = implicitly[JsonWriter[SerialCodePosition]].write(
      state.codePosition.asInstanceOf[SerialCodePosition])
    val jsFunctionStack = implicitly[JsonWriter[FunctionStack]].write(state.functionStack)
    val jsCurrentEvent = state.currentEvent.map(
      event => implicitly[JsonWriter[(Int, Int, Int)]].write(event)).getOrElse(JsNull)
    val fields = Map(
      JsonParseExecutionState.Fields.typeField -> JsString(JsonParseExecutionState.Fields.basicExecutionStateType),
      codePositionField -> jsCodePosition,
      functionStackField -> jsFunctionStack,
      currentEventField -> jsCurrentEvent,
      stackLengthField -> JsNumber(state.stackLength))
    JsObject(fields)
  }
  object Fields {
    val codePositionField: String = "_position"
    val functionStackField: String = "_stack"
    val currentEventField: String = "_currentEventSequence"
    val stackLengthField: String = "_stackLength"
    val targetField: String = "target"
  }
}
