package backend.communication

import backend.communication.execution_state._
import backend.execution_state.{ExecutionState, TargetEndExecutionState}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend._
import backend.tree.constraints.{ConstraintNegater, EventConstraint}
import backend.tree.constraints.event_constraints.{EventConstraintNegater, StopTestingTargets}
import spray.json._

class ConstraintESPCConverter(val processesInfo: List[Int]) extends PCElementConverter[ConstraintES] {

  val eventConstraintPCConverter: EventConstraintPCConverter = EventConstraintPCConverter(processesInfo)(
    EventConstraintNegater)
  @throws[UnexpectedJSONFormat]
  def convertPCElement(element: JsValue): PCElementWithStoreUpdate[ConstraintES] =
    element match {
      case jsObject: JsObject => CommonOperations.getStringField(jsObject, ConstraintFields.typeField) match {
        case ConstraintFields.assignmentConstraintValue =>
          val assignmentConstraint = BasicConstraintJSONParsing.convertAssignmentConstraint(jsObject)
          StoreUpdatePCElement(assignmentConstraint)
        case ConstraintFields.enterScopeConstraintValue =>
          val enterScopeConstraint = BasicConstraintJSONParsing.convertEnterScopeConstraint(jsObject)
          StoreUpdatePCElement(enterScopeConstraint)
        case ConstraintFields.exitScopeConstraintValue =>
          val exitScopeConstraint = BasicConstraintJSONParsing.convertExitScopeConstraint(jsObject)
          StoreUpdatePCElement(exitScopeConstraint)
        case _ =>
          val RegularPCElement(constraint, isTrue) = eventConstraintPCConverter.convertPCElement(element)
          val executionState: ExecutionState = getExecutionState(constraint, jsObject)
          val constraintES: ConstraintES = EventConstraintWithExecutionState(constraint, executionState)
          ConstraintWithStoreUpdate(constraintES, isTrue)
      }
      case _ => throw UnexpectedJSONFormat(element)
    }
  protected def getExecutionState(constraint: EventConstraint, jsObject: JsObject): ExecutionState = constraint match {
    case StopTestingTargets(id, _, _, _) => TargetEndExecutionState(id)
    case _ =>
      val jsExecutionState = CommonOperations.getObjectField(jsObject, ConstraintFields.executionStateField)
      implicitly[JsonReader[ExecutionState]].read(jsExecutionState)
  }
}

class ConstraintESJSONParsing extends JSONParsing[ConstraintES]()(ConstraintESNegater, ConstraintESOptimizer) {

  private[communication] implicit val constraintESNegater: ConstraintNegater[ConstraintES] = ConstraintESNegater
  @throws[UnexpectedInputType]
  private[communication] def extractFormula(jsonInput: JsObject, fieldName: String): Formula = {
    val pathConditionsJson = CommonOperations.getArrayField(jsonInput, fieldName)
    val pathConditions: Formula = pathConditionsJson
      .map(getPCElementConverter.convertPCElement)
      .flatMap(_.toConstraint match {
        case Some(EventConstraintWithExecutionState(bc: BranchConstraint, _)) => List(bc.exp)
        case Some(constraint) => throw UnexpectedInputValue(constraint)
        case None => Nil
      })
      .toList
    pathConditions
  }
  def getPCElementConverter: ConstraintESPCConverter = new ConstraintESPCConverter(Nil)
}
