package backend.json

import backend.RegularPCElement
import backend.communication.CommonOperations
import backend.modes._
import backend.tree.constraints.{Constraint, EventConstraint}
import spray.json._

object SMTSolveProcessModeJsonWriterReader extends JsonWriter[Mode] with JsonReader[Mode] {

  def read(jsValue: JsValue): Mode = CommonOperations.jsStringToString(jsValue) match {
    case FieldNames.exploreMode => ExploreTreeMode
    case FieldNames.functionSummariesMode => FunctionSummariesMode
    case FieldNames.solveMode => SolvePathsMode
    case FieldNames.verifyMode => VerifyIntraMode
  }

  def write(mode: Mode): JsString =
    JsString(mode match {
      case ExploreTreeMode => FieldNames.exploreMode
      case FunctionSummariesMode => FieldNames.functionSummariesMode
      case MergingMode => FieldNames.mergeMode
      case SolvePathsMode => FieldNames.solveMode
      case VerifyIntraMode => FieldNames.verifyMode
    })

  object FieldNames {
    val exploreMode: String = "explore"
    val functionSummariesMode: String = "function_summaries"
    val solveMode: String = "solve"
    val verifyMode: String = "verify"
    val mergeMode: String = "merge"
  }
}

class SMTSolveProcessJsonWriterReader(
  val mode: Mode
) extends JsonReader[SMTSolveProcess[_ <: Constraint]]
  with JsonWriter[SMTSolveProcess[_ <: Constraint]] {

  def read(jsValue: JsValue): SMTSolveProcess[_ <: Constraint] = mode match {
    case ExploreTreeMode => ExploreTreeJsonWriter.read(jsValue)
    case VerifyIntraMode => VerifyIntraJsonWriterReader.read(jsValue)
  }

  def write(solveProcess: SMTSolveProcess[_ <: Constraint]): JsObject = solveProcess match {
    case exploreTree: ExploreTree[EventConstraint@unchecked, RegularPCElement[EventConstraint]] => ExploreTreeJsonWriter.write(
      exploreTree)
    case verifyIntra: VerifyIntra => VerifyIntraJsonWriterReader.write(verifyIntra)
  }

}
