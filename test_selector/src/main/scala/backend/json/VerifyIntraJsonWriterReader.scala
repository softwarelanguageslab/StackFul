package backend.json

import backend.RegularPCElement
import backend.communication.CommonOperations
import backend.modes.{ExploreTree, VerifyIntra}
import backend.tree.constraints.EventConstraint
import spray.json._

object VerifyIntraJsonWriterReader extends JsonWriter[VerifyIntra] with JsonReader[VerifyIntra] {

  def read(jsValue: JsValue): VerifyIntra = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val treeOutputPath: String =
      CommonOperations.getStringField(jsObject, FieldNames.treeOutputPathField)
    val exploreTreeProcessJsValue: JsValue =
      CommonOperations.getField(jsObject, FieldNames.exploreTreeProcessField)
    val exploreTreeProcess: ExploreTree[EventConstraint, RegularPCElement[EventConstraint]] = ExploreTreeJsonWriter.read(
      exploreTreeProcessJsValue)
    val verifyIntra = new VerifyIntra(treeOutputPath, exploreTreeProcess)
    verifyIntra
  }

  def write(solver: VerifyIntra): JsObject = {
    JsObject(
      FieldNames.treeOutputPathField -> JsString(solver.treeOutputPath),
      FieldNames.exploreTreeProcessField -> ExploreTreeJsonWriter.write(solver.exploreTree)
    )
  }

  object FieldNames {
    val treeOutputPathField: String = "tree_output"
    val exploreTreeProcessField: String = "explore_process"
  }

}
