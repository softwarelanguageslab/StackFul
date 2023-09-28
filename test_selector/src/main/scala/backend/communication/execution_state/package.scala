package backend.communication

import backend.execution_state._
import spray.json.{JsonReader, JsonWriter}

package object execution_state {

  implicit val jsonReaderExecutionState: JsonReader[ExecutionState] = JsonParseExecutionState
  implicit val jsonWriterExecutionState: JsonWriter[ExecutionState] = JsonParseExecutionState

  implicit val jsonReaderBasicExecutionState: JsonReader[CodeLocationExecutionState] = JsonParseCodeLocationExecutionState
  implicit val jsonWriterBasicExecutionState: JsonWriter[CodeLocationExecutionState] = JsonParseCodeLocationExecutionState

  implicit val jsonReaderTargetEndExecutionState: JsonReader[TargetEndExecutionState] = JsonTargetEndExecutionState
  implicit val jsonWriterTargetEndExecutionState: JsonWriter[TargetEndExecutionState] = JsonTargetEndExecutionState

  implicit val jsonParseSerialCodePosition: JsonParseCodePosition[SerialCodePosition] = JsonParseSerialCodePosition

  implicit val jsonReaderFunctionId: JsonReader[FunctionId] = JsonParseFunctionId
  implicit val jsonWriterFunctionId: JsonWriter[FunctionId] = JsonParseFunctionId

  implicit val jsonReaderCallArrivalFunctionId: JsonReader[CallArrivalFunctionPair] = JsonParseCallArrivalFunctionPair
  implicit val jsonWriterCallArrivalFunctionId: JsonWriter[CallArrivalFunctionPair] = JsonParseCallArrivalFunctionPair

  implicit val jsonReaderSerialCodePosition: JsonReader[SerialCodePosition] = JsonParseSerialCodePosition
  implicit val jsonWriterSerialCodePosition: JsonWriter[SerialCodePosition] = JsonParseSerialCodePosition

  implicit val jsonReaderSerialFunctionId: JsonReader[SerialFunctionId] = JsonParseSerialFunctionId
  implicit val jsonWriterSerialFunctionId: JsonWriter[SerialFunctionId] = JsonParseSerialFunctionId

  implicit val jsonReaderFunctionStack: JsonReader[FunctionStack] = JsonParseFunctionStack
  implicit val jsonWriterFunctionStack: JsonWriter[FunctionStack] = JsonParseFunctionStack

  implicit val jsonReaderSymbolicStore: JsonReader[SymbolicStore] = JsonParseSymbolicStore
  implicit val jsonWriterSymbolicStore: JsonWriter[SymbolicStore] = JsonParseSymbolicStore

}
