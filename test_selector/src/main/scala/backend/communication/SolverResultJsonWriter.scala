package backend.communication

import backend._
import backend.communication.execution_state.JsonParseExecutionState
import backend.execution_state.ExecutionState
import backend.expression._
import backend.solvers._
import spray.json._

object SolverResultJsonWriter extends JsonWriter[SolverResult] {

  def write(solverResult: SolverResult): JsValue = solverResult match {
    case SymbolicTreeFullyExplored => JsObject("type" -> JsString("SymbolicTreeFullyExplored"))
    case FunctionFullyExplored(functionId) =>
      JsObject(
        Map[String, JsValue](
          "function_id" -> JsNumber(functionId),
          "type" -> JsString("FunctionFullyExplored")))
    case InvalidPath =>
      JsObject(
        Map[String, JsValue](
          "type" -> JsString("InvalidPath"),
          "message" -> JsString("Path led to an invalid node")))
    case UnsatisfiablePath =>
      JsObject(
        Map[String, JsValue](
          "type" -> JsString("UnsatisfiablePath"),
          "message" -> JsString("Path is unsatisfiable")))
    case NewInput(inputs, optTreePath) =>
      //    case NewInput(inputs, Some(treePath: TreePath[_, ConstraintWithExecutionState])) =>
      val inputObjects = JsArray(inputs.toList.flatMap({
        case (input, ComputedBool(bool)) => addComputedValueList(input, "boolean", JsBoolean(bool))
        case (input, ComputedFloat(float)) => addComputedValueList(input, "float", JsNumber(float))
        case (input, ComputedInt(int)) => addComputedValueList(input, "int", JsNumber(int))
        case (input, ComputedString(string)) => addComputedValueList(input, "string", JsString(string))
        case (_, _: ComputedEvent) => Nil
      }).toVector)
      val map: Map[String, JsValue] = optTreePath match {
        case Some(treePath) =>
          Map[String, JsValue](
            "type" -> JsString("NewInput"),
            "inputs" -> inputObjects,
            "path" -> JsString(pathToString(treePath.getPath)))
        //          if (treePath.isInstanceOf[TreePath[_, ConstraintWithExecutionState]]) {
        //            val ites = TreePath.collectITEs(treePath.asInstanceOf[TreePath[_, ConstraintWithExecutionState]])
        //            val iteObjects: JsArray = JsArray(ites.toVector.map(tuple => {
        //              val (name, exp) = tuple
        //              val jsExp = SymbolicExpressionJsonWriter().write(exp)
        //              JsObject(Map(name -> jsExp))
        //            }))
        //            Map[String, JsValue]("type" -> JsString("NewInput"),
        //              "inputs" -> inputObjects,
        //              "path" -> JsString(pathToString(treePath.getPath)),
        //              "ites" -> iteObjects)
        //          } else {
        //            Map[String, JsValue]("type" -> JsString("NewInput"),
        //              "inputs" -> inputObjects,
        //              "path" -> JsString(pathToString(treePath.getPath)))
        //          }
        case _ =>
          Map[String, JsValue](
            "type" -> JsString("NewInput"),
            "inputs" -> inputObjects)
      }
      JsObject(map)
    case InputAndEventSequence(newInput, events, path) =>
      val newInputJSON = write(newInput)
      val eventSequence = implicitly[JsonWriter[List[(Int, Int)]]].write(events)
      JsObject(
        Map[String, JsValue](
          "type" -> JsString("InputAndEventSequence"),
          "inputs" -> newInputJSON,
          "events" -> eventSequence,
          "path" -> JsString(pathToString(path))
        ))
    case ActionFailed => JsObject("type" -> JsString("ActionFailed"))
    case ActionNotApplied(reason) => JsObject("type" -> JsString("ActionNotApplied"), "reason" -> JsString(reason))
    case ActionSuccessful => JsObject("type" -> JsString("ActionSuccessful"))
  }

  private def idToIdFields(id: SymbolicInputId): Map[String, JsValue] = id match {
    case input: RegularId =>
      Map("processId" -> JsNumber(input.processId), "id" -> JsNumber(input.id))
    case input: FunctionId =>
      Map(
        "functionId" -> JsNumber(input.functionId),
        "processId" -> JsNumber(input.processId),
        "id" -> JsNumber(input.id))
    case input: FunctionReturnId =>
      Map(
        "functionId" -> JsNumber(input.functionId),
        "source" -> JsString("RETURN"),
        "processId" -> JsNumber(input.processId),
        "timesCalled" -> JsNumber(input.timesCalled)
      )
    case input: FunctionInputId =>
      Map(
        "functionId" -> JsNumber(input.functionId),
        "source" -> JsString("ARG"),
        "processId" -> JsNumber(input.processId),
        "timesCalled" -> JsNumber(input.timesCalled),
        "id" -> JsNumber(input.id)
      )
    case input: ExecutionStateId =>
      Map(
        IdFields.executionStateField -> JsonParseExecutionState.write(input.executionStateId),
        IdFields.idField -> JsNumber(input.id),
        IdFields.processIdField -> JsNumber(input.processId)
      )
  }

  private def addComputedValue(
    input: SymbolicInput,
    typeName: String,
    solutionJsValue: JsValue
  ): JsObject = {
    val valueFields: Map[String, JsValue] =
      Map("type" -> JsString(typeName), "value" -> solutionJsValue)
    val idFields: Map[String, JsValue] = idToIdFields(input.id)
    JsObject(valueFields ++ idFields)
  }

  private def addComputedValueList(
    input: SymbolicInput,
    typeName: String,
    solutionJsValue: JsValue
  ): List[JsObject] = {
    List(addComputedValue(input, typeName, solutionJsValue))
  }
}
