package backend.communication.execution_state

import spray.json._

import backend.communication._
import backend.execution_state._

object JsonParseFunctionId
  extends JsonReader[FunctionId]
    with JsonWriter[FunctionId] {

  override def read(jsValue: JsValue): FunctionId = jsValue match {
    case jsNumber: JsNumber => JsonParseSerialFunctionId.read(jsNumber)
    case jsObject: JsObject => JsonParseCallArrivalFunctionPair.read(jsObject)
    case _ => throw UnexpectedInputType(jsValue.toString)
  }
  override def write(functionId: FunctionId): JsValue = functionId match {
    case serial: SerialFunctionId => JsonParseSerialFunctionId.write(serial)
    case pair: CallArrivalFunctionPair => JsonParseCallArrivalFunctionPair.write(pair)
  }
}

object JsonParseSerialFunctionId
  extends JsonReader[SerialFunctionId]
    with JsonWriter[SerialFunctionId] {

  import CommonOperations._

  override def read(jsValue: JsValue): SerialFunctionId = convertFunctionId(jsValue)
  @throws[UnexpectedInputType]
  def convertFunctionId(jsValue: JsValue): SerialFunctionId = {
    SerialFunctionId(jsNumberToInt(jsValue))
  }
  override def write(functionId: SerialFunctionId): JsValue = {
    JsNumber(functionId.serial)
  }
}

object JsonParseCallArrivalFunctionPair
  extends JsonReader[CallArrivalFunctionPair]
    with JsonWriter[CallArrivalFunctionPair] {

  import CommonOperations._

  override def read(jsValue: JsValue): CallArrivalFunctionPair = {
    val jsObject = jsValueToJsObject(jsValue)
    val call = getField(jsObject, Fields.callSerialField) match {
      case serial: JsNumber => Some(jsNumberToInt(serial))
      case JsNull => None
      case _ => throw UnexpectedInputType(jsValue.toString)
    }
    val arrival = getIntField(jsObject, Fields.arrivalSerialField)
    CallArrivalFunctionPair(call, arrival)
  }
  override def write(pair: CallArrivalFunctionPair): JsValue = {
    val jsArrival = JsNumber(pair.arrivalSerial)
    val jsCall = pair.callSerial.map(JsNumber.apply).getOrElse(JsNull)
    JsObject(Map(Fields.arrivalSerialField -> jsArrival, Fields.callSerialField -> jsCall))
  }
  object Fields {
    val arrivalSerialField: String = "arrival"
    val callSerialField: String = "call"
  }
}


object JsonParseFunctionStack
  extends JsonReader[FunctionStack]
    with JsonWriter[FunctionStack] {

  import CommonOperations._

  override def read(jsValue: JsValue): FunctionStack = convertFunctionStack(jsValue)
  @throws[UnexpectedInputType]
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def convertFunctionStack(jsValue: JsValue): FunctionStack = {
    val array = jsValueToJsArray(jsValue)
    val stack: List[FunctionId] = array.elements.toList.map((jsValue: JsValue) => JsonParseFunctionId.read(jsValue))
    FunctionStack(stack)
  }
  override def write(functionStack: FunctionStack): JsValue = {
    val ids: List[JsValue] = functionStack.stack.map((id: FunctionId) => JsonParseFunctionId.write(id))
    JsArray(ids.toVector)
  }
}
