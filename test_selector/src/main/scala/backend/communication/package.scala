package backend

import backend.coverage_info.BranchCoverageMap
import backend.execution_state.ExecutionState
import backend.expression._
import backend.solvers.SolverResult
import backend.tree.constraints._
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.follow_path.{Direction, TargetTriggered}
import spray.json._

package object communication {

  implicit val symbolicSolverResultJsonWriter: JsonWriter[SolverResult] = SolverResultJsonWriter

  implicit val jsonReaderTarget: JsonReader[(Int, Int)] = JsonParseTarget
  implicit val jsonWriterTarget: JsonWriter[(Int, Int)] = JsonParseTarget

  implicit val jsonReaderEventWithId: JsonReader[(Int, Int, Int)] = JsonParseEventWithId
  implicit val jsonWriterEventWithId: JsonWriter[(Int, Int, Int)] = JsonParseEventWithId

  implicit val jsonReaderEventSequence: JsonReader[List[(Int, Int)]] = new JsonParseSequence()
  implicit val jsonWriterEventSequence: JsonWriter[List[(Int, Int)]] = new JsonParseSequence

  implicit val jsonReaderEventWithIdSequence: JsonReader[List[(Int, Int, Int)]] = new JsonParseSequence()
  implicit val jsonWriterEventWithIdSequence: JsonWriter[List[(Int, Int, Int)]] = new JsonParseSequence

  sealed trait JSONParsingException extends Exception
  sealed trait JSONInput
  sealed trait MetaInput extends JSONInput
  sealed trait ExploreModeJSONInput extends JSONInput
  sealed trait ExploreMergeModeJSONInput extends JSONInput
  sealed trait FunctionSummaryInput extends JSONInput
  sealed trait VerifyIntraInput extends JSONInput

  case class UnexpectedInputType(observedType: String) extends JSONParsingException {
    override def toString: String = {
      s"Unexpected input type: $observedType"
    }
  }

  case class UnexpectedJSONFormat(observedValue: JsValue) extends JSONParsingException {
    override def toString: String = {
      s"Unexpected JSON format: $observedValue"
    }
  }

  case class UnexpectedInputValue(value: Any) extends JSONParsingException {
    override def toString: String = {
      s"Unexpected value: $value"
    }
  }

  case class UnexpectedFieldType(fieldName: String, expectedType: String, jsObject: JsObject)
    extends JSONParsingException {
    override def toString: String = {
      s"Expected field $fieldName of object $jsObject to be of type $expectedType"
    }
  }

  case class MissingField(fieldName: String, jsObject: JsObject) extends JSONParsingException {
    override def toString: String = {
      s"No field $fieldName in $jsObject"
    }
  }

  case class ExploreModeInput[C <: Constraint : ConstraintNegater : Optimizer, Element <: PCElement[C]](
    iteration: Int,
    pathConstraint: PathConstraintWith[C, Element],
    processesInfo: List[Int],
    finishedTestRun: Boolean,
    coverageInfo: Option[BranchCoverageMap],
    eventHandlerCoverageInfo: Option[List[String]]
  )
    extends ExploreModeJSONInput

  case class ExploreMergeModeInput(exploreInput: ExploreModeInput[ConstraintES, PCElementWithStoreUpdate[ConstraintES]])
    extends ExploreMergeModeJSONInput
  case class TryMerge(
    iteration: Int,
    executionState: ExecutionState,
    pathConstraint: PathConstraintWithStoreUpdates[ConstraintES],
    prescribedEvents: List[(Int, Int)],
    i: Int /* TODO Facilitates debugging: number of try-merges sent */
  )
    extends ExploreMergeModeJSONInput

  case class SolveModeInput[C <: Constraint](
    pathConstraint: PathConstraint[C],
    eventSequence: List[TargetTriggered],
    branchSequence: List[Direction]
  )
    extends JSONInput

  case class FSComputePrefix(
    values: List[(ConcreteValue, SymbolicExpression)],
    correspondingInputs: List[SymbolicInput],
    prefixPath: PathConstraint[EventConstraint]
  )
    extends FunctionSummaryInput

  case class FSExploreFunction(
    iteration: Int,
    functionId: Int,
    values: List[(ConcreteValue, SymbolicExpression)],
    correspondingInputs: List[SymbolicInput],
    prefixPath: PathConstraint[EventConstraint],
    suffixPath: PathConstraint[EventConstraint],
    processId: Int,
    nrOfEnabledEvents: Int
  )
    extends FunctionSummaryInput

  case class FSCheckSatisfiesFormula(
    iteration: Int,
    functionId: Int,
    values: List[(ConcreteValue, SymbolicExpression)],
    correspondingInputs: List[SymbolicInput],
    preConditions: Formula,
    pathConditions: Formula,
    globalSymbolicInputs: List[(SymbolicInput, SymbolicExpression)]
  )
    extends FunctionSummaryInput

  class VIExploreModeInput[C <: Constraint : ConstraintNegater : Optimizer](val exploreModeInput: ExploreModeInput[C, RegularPCElement[C]]) extends VerifyIntraInput
  case class VIConnectPaths[C <: Constraint : ConstraintNegater : Optimizer](
    pc1: PathConstraint[C],
    pc2: PathConstraint[C]
  ) extends VerifyIntraInput
  case object CommunicationException extends Exception
  case object TerminationRequest extends MetaInput

}
