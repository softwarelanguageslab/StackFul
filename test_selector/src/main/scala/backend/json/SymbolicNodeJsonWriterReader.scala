package backend.json

import backend.communication.{CommonOperations, UnexpectedJSONFormat}
import backend.tree._
import backend.tree.constraints.Constraint
import spray.json._

class SymbolicNodeJsonWriterReader[C <: Constraint : JsonReader : JsonWriter]
  extends JsonWriter[SymbolicNode[C]]
    with JsonReader[SymbolicNode[C]] {

  val constraintJsonReader: JsonReader[C] = implicitly[JsonReader[C]]
  val constraintJsonWriter: JsonWriter[C] = implicitly[JsonWriter[C]]

  import FieldNames._

  def read(jsValue: JsValue): SymbolicNode[C] = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val typeValue = CommonOperations.getField(jsObject, typeField)
    typeValue match {
      case FieldNames.branchNodeType => readBranchSymbolicNode(jsObject)
      case FieldNames.mergedNodeType => MergedNode[C]()
      case FieldNames.leafNodeType => RegularLeafNode[C]()
      case FieldNames.safeNodeType => readSafeNode(jsObject)
      case FieldNames.unexploredNodeType => UnexploredNode[C]()
      case FieldNames.unsatisfiableNodeType => UnsatisfiableNode[C]()
      case _ => throw UnexpectedJSONFormat(typeValue)
    }
  }

  def write(node: SymbolicNode[C]): JsValue = node match {
    case bsn: BranchSymbolicNode[C] => writeBranchSymbolicNode(bsn)
    case _: MergedNode[C] => writeEmptyNodeOfType(mergedNodeType)
    case _: RegularLeafNode[C] => writeEmptyNodeOfType(leafNodeType)
    case ssn: SafeNode[C] => writeSafeNode(ssn)
    case _: UnexploredNode[C] => writeEmptyNodeOfType(unexploredNodeType)
    case _: UnsatisfiableNode[C] => writeEmptyNodeOfType(unsatisfiableNodeType)
  }

  private def writeBranchSymbolicNode(bsn: BranchSymbolicNode[C]): JsValue = {
    JsObject(
      typeField -> branchNodeType,
      branchConstraintField -> constraintJsonWriter.write(bsn.constraint),
      thenBranchField -> write(bsn.thenBranch.to),
      elseBranchField -> write(bsn.elseBranch.to)
    )
  }

  private def writeSafeNode(ssn: SafeNode[C]): JsObject = {
    JsObject(typeField -> safeNodeType, childField -> write(ssn.node))
  }

  private def writeEmptyNodeOfType(typeName: JsString): JsObject = {
    JsObject(typeField -> typeName)
  }

  private def readBranchSymbolicNode(jsObject: JsObject): BranchSymbolicNode[C] = {
    val constraint =
      constraintJsonReader.read(CommonOperations.getField(jsObject, branchConstraintField))
    val thenBranch = read(CommonOperations.getField(jsObject, thenBranchField))
    val elseBranch = read(CommonOperations.getField(jsObject, elseBranchField))
    // TODO should also read private fields of BranchSymbolicNode
    new BranchSymbolicNode(constraint, ThenEdge.to(thenBranch), ElseEdge.to(elseBranch))
  }

  private def readSafeNode(jsObject: JsObject): SafeNode[C] = {
    SafeNode(read(CommonOperations.getField(jsObject, childField)))
  }

  object FieldNames {
    val branchNodeType: JsString = JsString("type_branch")
    val mergedNodeType: JsString = JsString("type_merged")
    val singleNodeType: JsString = JsString("type_single")
    val safeNodeType: JsString = JsString("type_safe")
    val leafNodeType: JsString = JsString("type_leaf")
    val unexploredNodeType: JsString = JsString("type_unexplored")
    val unsatisfiableNodeType: JsString = JsString("type_unsat")

    val typeField: String = "type"
    val branchConstraintField: String = "branch"
    val thenBranchField: String = "thenBranch"
    val elseBranchField: String = "elseBranch"
    val childField: String = "child"
  }

}
