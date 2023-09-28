package backend.communication.json

import backend.communication.CommonOperations._
import backend.communication._
import backend.communication.execution_state._
import backend.execution_state.ExecutionState
import backend.expression._


import spray.json._

object SymbolicInputIdJsonReaderWriter extends JsonReader[SymbolicInputId] with JsonWriter[SymbolicInputId] {
  private def parseRegularId(jsObject: JsObject): RegularId = {
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    RegularId(id = id, processId = processId)
  }

  private def writeRegularId(id: RegularId): JsObject = {
    val map = Map(
      IdFields.idField -> JsNumber(id.id),
      IdFields.processIdField -> JsNumber(id.processId))
    JsObject(map)
  }

  private def parseFunctionId(jsObject: JsObject): FunctionId = {
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val functionId = CommonOperations.getIntField(jsObject, IdFields.functionIdField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    FunctionId(functionId = functionId, id = id, processId = processId)
  }

  private def writeFunctionId(id: FunctionId): JsObject = {
    val map = Map(
      IdFields.idField -> JsNumber(id.id),
      IdFields.functionIdField -> JsNumber(id.functionId),
      IdFields.processIdField -> JsNumber(id.processId))
    JsObject(map)
  }

  private def parseFunctionReturnId(jsObject: JsObject): FunctionReturnId = {
    val timesCalled = CommonOperations.getIntField(jsObject, IdFields.timesCalledField)
    val functionId = CommonOperations.getIntField(jsObject, IdFields.functionIdField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    FunctionReturnId(functionId = functionId, timesCalled = timesCalled, processId = processId)
  }

  private def writeFunctionReturnId(id: FunctionReturnId): JsObject = {
    val map = Map(
      IdFields.timesCalledField -> JsNumber(id.timesCalled),
      IdFields.functionIdField -> JsNumber(id.functionId),
      IdFields.processIdField -> JsNumber(id.processId))
    JsObject(map)
  }

  private def parseFunctionInputId(jsObject: JsObject): FunctionInputId = {
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val timesCalled = CommonOperations.getIntField(jsObject, IdFields.timesCalledField)
    val functionId = CommonOperations.getIntField(jsObject, IdFields.functionIdField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    FunctionInputId(id = id, functionId = functionId, timesCalled = timesCalled, processId = processId)
  }

  private def writeFunctionInputId(id: FunctionInputId): JsObject = {
    val map = Map(
      IdFields.idField -> JsNumber(id.id),
      IdFields.timesCalledField -> JsNumber(id.timesCalled),
      IdFields.functionIdField -> JsNumber(id.functionId),
      IdFields.processIdField -> JsNumber(id.processId))
    JsObject(map)
  }

  private def parseExecutionStateId(jsObject: JsObject): ExecutionStateId = {
    val executionStateJsObject = CommonOperations.getObjectField(jsObject, IdFields.executionStateField)
    val executionState = implicitly[JsonReader[ExecutionState]].read(executionStateJsObject)
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    ExecutionStateId(executionState, processId = processId, id = id)
  }

  private def writeExecutionStateId(id: ExecutionStateId): JsObject = {
    val map = Map(
      IdFields.executionStateField -> implicitly[JsonWriter[ExecutionState]].write(id.executionStateId),
      IdFields.idField -> JsNumber(id.id),
      IdFields.processIdField -> JsNumber(id.processId))
    JsObject(map)
  }

  override def read(jsValue: JsValue): SymbolicInputId = {
    val jsObject = jsValueToJsObject(jsValue)
    val optTimesCalled = getOptIntField(jsObject, IdFields.timesCalledField)
    if (optTimesCalled.isDefined && getOptIntField(jsObject, IdFields.idField).isDefined) {
      parseFunctionInputId(jsObject)
    } else if (optTimesCalled.isDefined) {
      parseFunctionReturnId(jsObject)
    } else if (getOptIntField(jsObject, IdFields.functionIdField).isDefined) {
      parseFunctionId(jsObject)
    } else if (getOptObjectField(jsObject, IdFields.executionStateField).isDefined) {
      parseExecutionStateId(jsObject)
    } else if (getOptIntField(jsObject, IdFields.idField).isDefined) {
      parseRegularId(jsObject)
    } else {
      throw UnexpectedJSONFormat(jsValue)
    }
  }
  override def write(inputId: SymbolicInputId): JsValue = inputId match {
    case id: RegularId => writeRegularId(id)
    case id: FunctionId => writeFunctionId(id)
    case id: FunctionInputId => writeFunctionInputId(id)
    case id: FunctionReturnId => writeFunctionReturnId(id)
    case id: ExecutionStateId => writeExecutionStateId(id)
  }
}
