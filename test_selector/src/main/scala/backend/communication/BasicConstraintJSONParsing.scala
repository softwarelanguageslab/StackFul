package backend.communication

import backend.execution_state.store.{EnterScopeUpdate, ExitScopeUpdate, TemporaryAssignmentStoreUpdate}
import spray.json.{JsObject, JsValue}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.BasicConstraint
import backend.{Formula, PCElement, RegularPCElement}

object BasicConstraintPCConverter extends PCElementConverter[BasicConstraint] {

  @throws[UnexpectedJSONFormat]
  def convertPCElement(element: JsValue): RegularPCElement[BasicConstraint] = element match {
    case jsObject: JsObject =>
      val constraintType = CommonOperations.getStringField(jsObject, ConstraintFields.typeField)
      constraintType match {
        case ConstraintFields.branchConstraintValue =>
          val isTrue: Boolean =
            CommonOperations.getBooleanField(jsObject, ConstraintFields.wasTrueField)
          val bc: BasicConstraint = BasicConstraintJSONParsing.convertBranchConstraint(jsObject)
          RegularPCElement(bc, isTrue)
        case _ => throw UnexpectedJSONFormat(element)
      }
    case _ => throw UnexpectedJSONFormat(element)
  }
}

object BasicConstraintJSONParsing extends JSONParsing[BasicConstraint] {

  import JsonParseSymbolicExp._

  def convertBranchConstraint(jsObject: JsObject): BranchConstraint = {
    val symbolicJsValue = jsObject.fields(ConstraintFields.symbolicField)
    val booleanSymbolicExp = symbolicExpToBooleanSymbolicExp(symbolicToExp(symbolicJsValue))
    BranchConstraint(booleanSymbolicExp, Nil)
  }
  def convertAssignmentConstraint(jsObject: JsObject): TemporaryAssignmentStoreUpdate = {
    val identifier = CommonOperations.getStringField(jsObject, ConstraintFields.identifierField)
    val jsExp = CommonOperations.getField(jsObject, ConstraintFields.symbolicField)
    val exp = symbolicToExp(jsExp)
    TemporaryAssignmentStoreUpdate(identifier, exp)
  }
  def convertEnterScopeConstraint(jsObject: JsObject): EnterScopeUpdate = {
    val jsVariables = CommonOperations.getArrayField(jsObject, ConstraintFields.identifiersField)
    val variables = jsVariables.map(CommonOperations.asString).filter(!JsonParseIdentifier.ignoreIdentifier(_))
    val scopeId = CommonOperations.getIntField(jsObject, ConstraintFields.scopeIdField)
    EnterScopeUpdate(variables.toSet, scopeId)
  }
  def convertExitScopeConstraint(jsObject: JsObject): ExitScopeUpdate = {
    val jsVariables = CommonOperations.getArrayField(jsObject, ConstraintFields.identifiersField)
    val variables = jsVariables.map(CommonOperations.asString)
    val scopeId = CommonOperations.getIntField(jsObject, ConstraintFields.scopeIdField)
    ExitScopeUpdate(variables.toSet, scopeId)
  }
  @throws[UnexpectedInputType]
  private[communication] def extractFormula(jsonInput: JsObject, fieldName: String): Formula = {
    val pathConditionsJson = CommonOperations.getArrayField(jsonInput, fieldName)
    val pathConditions: Formula = pathConditionsJson
      .map(getPCElementConverter.convertPCElement)
      .map((pcELement: PCElement[BasicConstraint]) =>
        pcELement.toConstraint match {
          case Some(BranchConstraint(exp, _)) => exp
        })
      .toList
    pathConditions
  }
  def getPCElementConverter: PCElementConverter[BasicConstraint] = BasicConstraintPCConverter
}
