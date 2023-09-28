package backend.json

import backend.RegularPCElement
import backend.communication._
import backend.communication.execution_state._
import backend.execution_state.ExecutionState
import backend.execution_state.store._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, EventConstraintWithExecutionState}
import backend.tree.constraints.event_constraints._
import spray.json._

class BasicConstraintJsonReaderWriter
  extends JsonWriter[BasicConstraint]
    with JsonReader[BasicConstraint] {

  val jsonParsing: BasicConstraintJSONParsing.type = BasicConstraintJSONParsing

  val expWriter: SymbolicExpressionJsonWriter = SymbolicExpressionJsonWriter()
  def read(jsValue: JsValue): BasicConstraint = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    CommonOperations.getStringField(jsObject, ConstraintFields.typeField) match {
      case ConstraintFields.branchConstraintValue => readBranchConstraint(jsObject)
    }
  }
  private[json] def readBranchConstraint(jsObject: JsObject): BranchConstraint = {
    val jsSymbolic = CommonOperations.getField(jsObject, ConstraintFields.symbolicField)
    val symbolic = JsonParseSymbolicExp.symbolicToBooleanExp(JsonParseSymbolicExp.symbolicToExp(jsSymbolic))
    basic_constraints.BranchConstraint(symbolic, Nil)
  }
  def write(constraint: BasicConstraint): JsObject = constraint match {
    case bct: BranchConstraint => writeBranchConstraint(bct)
  }
  private[json] def writeBranchConstraint(bc: BranchConstraint): JsObject = {
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.branchConstraintValue),
      ConstraintFields.symbolicField -> expWriter.write(bc.exp))
  }
  private[json] def readAssignmentConstraint(jsObject: JsObject): TemporaryAssignmentStoreUpdate = {
    val identifier = CommonOperations.getStringField(jsObject, ConstraintFields.identifierField)
    val jsSymbolic = CommonOperations.getField(jsObject, ConstraintFields.symbolicField)
    val symbolic = JsonParseSymbolicExp.symbolicToBooleanExp(JsonParseSymbolicExp.symbolicToExp(jsSymbolic))
    TemporaryAssignmentStoreUpdate(identifier, symbolic)
  }
  private[json] def writeAssignmentConstraint(ac: AssignmentsStoreUpdate): JsObject = {
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.assignmentConstraintValue),
      ConstraintFields.identifiersField -> JsArray(ac.assignments.toVector.map(tuple =>
        JsObject(
          ConstraintFields.identifierField -> JsString(tuple._1),
          ConstraintFields.symbolicField -> expWriter.write(tuple._2)))
      ))
  }
  private[json] def writeEnterScopeConstraint(ec: EnterScopeUpdate): JsObject = {
    val jsIdentifiers: JsArray = JsArray(ec.identifiers.map(string => JsString(string)).toVector)
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.enterScopeConstraintValue),
      ConstraintFields.identifiersField -> jsIdentifiers,
      ConstraintFields.scopeIdField -> JsNumber(ec.scopeId))
  }
  private[json] def writeExitScopeConstraint(ec: ExitScopeUpdate): JsObject = {
    val jsIdentifiers: JsArray = JsArray(ec.identifiers.map(string => JsString(string)).toVector)
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.exitScopeConstraintValue),
      ConstraintFields.identifiersField -> jsIdentifiers,
      ConstraintFields.scopeIdField -> JsNumber(ec.scopeId))
  }

}

class EventConstraintJsonReaderWriter(val jsonParsing: JSONParsing[EventConstraint])
  extends JsonWriter[EventConstraint]
    with JsonReader[EventConstraint] {

  val expWriter: SymbolicExpressionJsonWriter = SymbolicExpressionJsonWriter()

  private val basicConstraintJsonReaderWriter = new BasicConstraintJsonReaderWriter()

  def write(constraint: EventConstraint): JsObject = constraint match {
    case bct: BranchConstraint => basicConstraintJsonReaderWriter.writeBranchConstraint(bct)
    case stt: StopTestingTargets => writeStopTestingTargets(stt)
    case tch: TargetChosen => writeTargetChosen(tch)
  }

  private def writeStopTestingTargets(stt: StopTestingTargets): JsObject = {
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.stopTargetsConstraintValue),
      IdFields.idField -> JsNumber(stt.id),
      SMTSolveProcessFields.processesInfoField -> JsArray(
        stt.processesInfo.map(id => JsNumber(id)).toVector)
    )
  }

  private def writeTargetChosen(tch: TargetChosen): JsObject = {
    def writeProcessInfo(info: ProcessesInfo): JsObject = {
      JsObject(
        ProcessInfoFields.createExtraTargets -> JsBoolean(info.allowCreatingExtraTargets),
        ProcessInfoFields.processesInfoField -> JsArray(
          info.processesInfo.map(id => JsNumber(id)).toVector)
      )
    }
    JsObject(
      ConstraintFields.typeField -> JsString(ConstraintFields.targetConstraintValue),
      ExpFields.idField -> JsNumber(tch.id),
      ExpFields.processIdChosenField -> JsNumber(tch.processIdChosen),
      ExpFields.targetIdField -> JsNumber(tch.targetIdChosen),
      ConstraintFields.eventsAlreadyChosenField -> writeEventsChosen(tch.eventsAlreadyChosen),
      ConstraintFields.processesInfoField -> writeProcessInfo(tch.info)
    )
  }

  private def writeEventsChosen(events: Map[Int, Set[Int]]): JsArray = {
    def makeJsTuple(pid: Int, eids: Set[Int]): JsObject = {
      JsObject("pid" -> JsNumber(pid), "eids" -> JsArray(eids.map(id => JsNumber(id)).toVector))
    }
    JsArray(events.toList.map((tuple: (Int, Set[Int])) => makeJsTuple(tuple._1, tuple._2)).toVector)
  }

  def read(jsValue: JsValue): EventConstraint = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    CommonOperations.getStringField(jsObject, ConstraintFields.typeField) match {
      case ConstraintFields.branchConstraintValue => basicConstraintJsonReaderWriter.readBranchConstraint(jsObject)
      case ConstraintFields.stopTargetsConstraintValue => readStopTestingTargets(jsObject)
      case ConstraintFields.targetConstraintValue => readTargetChosen(jsObject)
    }
  }

  private def readStopTestingTargets(jsObject: JsObject): StopTestingTargets = {
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val jsArray = CommonOperations.getArrayField(jsObject, SMTSolveProcessFields.processesInfoField)
    val processesInfo = jsArray.map((id: JsValue) => CommonOperations.jsNumberToInt(id)).toList
    val startingProcess = CommonOperations.getIntField(jsObject, ConstraintFields.startingProcessField)
    StopTestingTargets(id, processesInfo, Nil, startingProcess)
  }

  private def readTargetChosen(jsObject: JsObject): TargetChosen = {
    def readProcessInfo(jsObject: JsObject): ProcessesInfo = {
      val createExtraTargets =
        CommonOperations.getBooleanField(jsObject, ProcessInfoFields.createExtraTargets)
      val pidsJsArray =
        CommonOperations.getArrayField(jsObject, ProcessInfoFields.processesInfoField)
      val pids = pidsJsArray.map(CommonOperations.jsNumberToInt).toList
      ProcessesInfo(pids, createExtraTargets)
    }
    def readEventIdsChosen(jsObject: JsObject): (Int, Set[Int]) = {
      val pid = CommonOperations.getIntField(jsObject, "pid")
      val eids = CommonOperations.getArrayField(jsObject, "eids")
      (pid, eids.map((jsValue: JsValue) => CommonOperations.jsNumberToInt(jsValue)).toSet)
    }
    val id = CommonOperations.getIntField(jsObject, ExpFields.idField)
    val processIdChosen = CommonOperations.getIntField(jsObject, ExpFields.processIdChosenField)
    val targetIdChosen = CommonOperations.getIntField(jsObject, ExpFields.targetIdField)
    val eventsAlreadyChosen =
      CommonOperations
        .getArrayField(jsObject, ConstraintFields.eventsAlreadyChosenField)
        .map((jsValue: JsValue) => readEventIdsChosen(CommonOperations.jsValueToJsObject(jsValue)))
        .toMap
    val processesInfo = readProcessInfo(
      CommonOperations.getObjectField(jsObject, ConstraintFields.processesInfoField))
    val startingProcess = CommonOperations.getIntField(jsObject, ConstraintFields.startingProcessField)
    event_constraints.TargetChosen(
      id, processIdChosen, targetIdChosen, eventsAlreadyChosen, processesInfo, startingProcess, Nil)
  }

  object ProcessInfoFields {
    val createExtraTargets: String = "create_extra"
    val processesInfoField: String = "pids"
  }
}

class ConstraintESJsonReaderWriter(val jsonParsing: JSONParsing[EventConstraint])
  extends JsonReader[ConstraintES]
    with JsonWriter[ConstraintES] {

  import CommonOperations._

  val eventConstraintJsonReaderWriter = new EventConstraintJsonReaderWriter(jsonParsing)

  override def read(jsValue: JsValue): ConstraintES = {
    val jsObject = jsValueToJsObject(jsValue)
    val jsState = getField(jsObject, ConstraintFields.executionStateField)
    val state = implicitly[JsonReader[ExecutionState]].read(jsState)
    val jsBasicConstraint = getField(jsObject, ConstraintFields.constraintField)
    val eventConstraint = eventConstraintJsonReaderWriter.read(jsBasicConstraint)
    EventConstraintWithExecutionState(eventConstraint, state)
  }
  override def write(constraint: ConstraintES): JsValue = constraint match {
    case EventConstraintWithExecutionState(bc, state) =>
      val jsState = implicitly[JsonWriter[ExecutionState]].write(state)
      val jsEventConstraint = eventConstraintJsonReaderWriter.write(bc)
      val fields = Map(
        ConstraintFields.constraintField -> jsEventConstraint,
        ConstraintFields.executionStateField -> jsState)
      JsObject(fields)
  }
}

class RegularPCElementJsonWriter[C <: Constraint : ConstraintNegater : JsonWriter : HasJSONParsing](val jsonParsing: JSONParsing[C])
  extends JsonWriter[RegularPCElement[C]] {

  val constraintWriter: JsonWriter[C] = implicitly[JsonWriter[C]]

  def write(element: RegularPCElement[C]): JsValue = {
    JsObject(
      Map[String, JsValue](ConstraintFields.wasTrueField -> JsBoolean(element.isTrue)) ++
        constraintWriter.write(element.constraint).asJsObject.fields)
  }

}
