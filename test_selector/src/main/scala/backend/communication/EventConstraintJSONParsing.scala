package backend.communication


import spray.json._

import backend._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.event_constraints._
import backend.tree.constraints.{ConstraintNegater, EventConstraint}

case class EventConstraintPCConverter(processesInfo: List[Int])
  (implicit val eventConstraintNegater: ConstraintNegater[EventConstraint]) extends PCElementConverter[EventConstraint] {
  @throws[UnexpectedJSONFormat]
  def convertPCElement(element: JsValue): RegularPCElement[EventConstraint] = element match {
    case jsObject: JsObject => CommonOperations.getStringField(jsObject, ConstraintFields.typeField) match {
      case ConstraintFields.branchConstraintValue =>
        val isTrue: Boolean =
          CommonOperations.getBooleanField(jsObject, ConstraintFields.wasTrueField)
        val bc: EventConstraint = BasicConstraintJSONParsing.convertBranchConstraint(jsObject)
        RegularPCElement(bc, isTrue)(eventConstraintNegater)
      case ConstraintFields.targetConstraintValue =>
        val targetChosen: EventConstraint = convertJsObjectToTargetChosen(
          jsObject.fields(ConstraintFields.symbolicField).asJsObject,
          processesInfo)
        RegularPCElement(targetChosen, isTrue = true)(eventConstraintNegater)
      case ConstraintFields.targetEndConstraintValue =>
        val stopTesting: EventConstraint = convertJsObjectToStopTesting(jsObject, processesInfo)
        // isTrue temporarily set to true, but will be given a proper value afterwards
        RegularPCElement(stopTesting, isTrue = true)(eventConstraintNegater)
      case _ => throw UnexpectedJSONFormat(element)
    }
    case _ => throw UnexpectedJSONFormat(element)
  }

  protected def convertJsObjectToStopTesting(jsObject: JsObject, processesInfo: List[Int]): StopTestingTargets = {
    val jsCurrentEvent = CommonOperations.getObjectField(jsObject, ConstraintFields.symbolicField)
    val id = CommonOperations.getIntField(jsCurrentEvent, ConstraintFields.idField)
    val startingProcess = CommonOperations.getIntField(jsObject, ConstraintFields.startingProcessField)
    StopTestingTargets(id, processesInfo, Nil, startingProcess)
  }

  protected def convertJsObjectToTargetChosen(
    jsObject: JsObject,
    processesInfo: List[Int]
  ): TargetChosen = {
    val id = CommonOperations.getIntField(jsObject, ExpFields.idField)
    val processIdChosen = CommonOperations.getIntField(jsObject, ExpFields.processIdChosenField)
    val targetIdChosen = CommonOperations.getIntField(jsObject, ExpFields.targetIdChosenField)
    val initialisedMap = processesInfo.indices.foldLeft(Map[Int, Set[Int]]())((map, idx) => map + (idx -> Set()))
    val startingProcess = CommonOperations.getIntField(jsObject, ConstraintFields.startingProcessField)
    val targetChosen = TargetChosen(
      id,
      processIdChosen,
      targetIdChosen,
      initialisedMap,
      ProcessesInfo(processesInfo, allowCreatingExtraTargets = true),
      startingProcess,
      Nil)
    targetChosen
  }
}


class EventConstraintJSONParsing extends JSONParsing[EventConstraint]() {

  private[communication] val pcElementConverter = EventConstraintPCConverter(Nil)
  @throws[UnexpectedInputType]
  private[communication] def extractFormula(jsonInput: JsObject, fieldName: String): Formula = {
    val pathConditionsJson = CommonOperations.getArrayField(jsonInput, fieldName)
    val pathConditions: Formula = pathConditionsJson
      .map(getPCElementConverter.convertPCElement)
      .map((pcELement: PCElement[EventConstraint]) =>
        pcELement.toConstraint match {
          case Some(bc: BranchConstraint) => bc.exp
          case other => throw UnexpectedInputValue(other)
        })
      .toList
    pathConditions
  }
  def getPCElementConverter: PCElementConverter[EventConstraint] = pcElementConverter
}
