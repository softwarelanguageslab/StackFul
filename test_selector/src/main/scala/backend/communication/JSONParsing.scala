package backend.communication

import backend._
import backend.communication.CommonOperations.{jsStringToString, jsValueToJsArray, jsValueToJsObject}
import backend.communication.execution_state._
import backend.coverage_info.BranchCoverageMap
import backend.execution_state.store.{AssignmentsStoreUpdate, TemporaryAssignmentStoreUpdate}
import backend.execution_state.{CodePosition, ExecutionState, SerialCodePosition}
import backend.expression.{BooleanExpression, _}
import backend.tree.constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints._
import backend.tree.follow_path._
import spray.json._

object ExpTypes extends Enumeration {
  val SymbolicNothing, SymbolicBool, SymbolicFloat, SymbolicInt, SymbolicString, SymbolicFunctionInput,
  SymbolicInputInt, SymbolicInputString, SymbolicReturnValue, SymbolicFunction, SymbolicArithmeticExp,
  SymbolicRelationalExp, SymbolicStringOperationExp, SymbolicUnaryExp, SymbolicLogicalBinExpression,
  SymbolicMessageInput, SymbolicEventChosen, RawSymbolicFunctionInput, RawSymbolicReturnValue =
    Value

  val nothingType: String = "Nothing"
  val boolType: String = "SymbolicBool"
  val floatType: String = "SymbolicFloat"
  val intType: String = "SymbolicInt"
  val stringType: String = "SymbolicString"
  val iteType: String = "SymbolicITE"
  val functionInputType: String = "SymbolicFunctionInput"
  val inputBoolField: String = "SymbolicInputBool"
  val inputFloatField: String = "SymbolicInputFloat"
  val inputIntType: String = "SymbolicInputInt"
  val inputStringType: String = "SymbolicInputString"
  val returnValueType: String = "SymbolicReturnValue"
  val symbolicFunctionType: String = "SymbolicFunction"
  val arithmeticExpType: String = "SymbolicArithmeticExp"
  val relationalExpType: String = "SymbolicRelationalExp"
  val stringOperationExpType: String = "SymbolicStringOperationExp"
  val unaryExpType: String = "SymbolicUnaryExp"
  val logicanBinExpType: String = "SymbolicLogicalBinExpression"
  val messageInputType: String = "SymbolicMessageInput"
  val eventChosenType: String = "SymbolicEventChosen"
  val rawFunctionInputType: String = "RawSymbolicFunctionInput"
  val rawReturnValueType: String = "RawSymbolicReturnValue"
}

object IdFields {
  val idField: String = "id"
  val functionIdField: String = "functionId"
  val processIdField: String = "processId"
  val timesCalledField: String = "timesCalled"
  val identifierfield: String = "_identifier"
  val executionStateField: String = "executionState"
}

object ExpFields {
  val typeField: String = "type"
  val idField: String = "id"
  val inputTypeField: String = "inputType"
  val operatorField: String = "operator"
  val argsField: String = "args"
  val argumentField: String = "argument"
  val argumentsField: String = "arguments"
  val leftField: String = "left"
  val rightField: String = "right"

  val bField: String = "b"
  val fField: String = "f"
  val iField: String = "i"
  val sField: String = "s"

  val predExpField: String = "pred"
  val thenExpField: String = "then"
  val elseExpField: String = "else"

  val processIdChosenField: String = "processIdChosen"
  val targetIdChosenField: String = "targetIdChosen"
  val messageTypeField: String = "messageType"
  val messageInputTypeField: String = "message_input_type"

  val processIdField: String = "processId"
  val targetIdField: String = "targetId"

  val identifierField: String = "_identifier"
  val finishedTestRunField: String = "finished_run"

  val branchCoverageInfoField: String = "branchCoverageInfo"
  val eventHandlersCoverageInfoField: String = "eventHandlersCoverageInfo"

  val serialField: String = "serial"
  val environmentField: String = "environment"
}

object ConstraintFields {
  val branchConstraintValue: String = "CONSTRAINT"
  val fixedConstraintValue: String = "FIXED"
  val stopTargetsConstraintValue: String = "STOP"
  val targetConstraintValue: String = "TARGET"
  val targetEndConstraintValue: String = "END_TARGET"
  val assignmentConstraintValue: String = "ASSIGNMENT"
  val enterScopeConstraintValue: String = "ENTER_SCOPE"
  val exitScopeConstraintValue: String = "EXIT_SCOPE"

  val typeField: String = "_type"
  val symbolicField: String = "_symbolic"
  val wasTrueField: String = "_wasTrue"
  val eventsAlreadyChosenField: String = "events_chosen"
  val processesInfoField: String = "processes_info"
  val constraintField: String = "constraint"
  val executionStateField: String = "_executionState"
  val storeField: String = "_store"
  val idField: String = "id"
  val identifierField: String = "_identifier"
  val identifiersField: String = "_variables"
  val startingProcessField: String = "startingProcess"
  val scopeIdField: String = "_scopeId"
}

object SMTSolveProcessFields {
  val branchSeqField: String = "branch_seq"
  val eventSeqField: String = "event_seq"
  val pathConstraintField: String = "PC"
  val iterationField: String = "iteration"
  val prefixField: String = "prefix"
  val suffixField: String = "suffix"
  val correspondingInputsField: String = "corresponding_inputs"
  val argumentsField: String = "arguments"
  val functionIdField: String = "function_id"
  val processIdField: String = "processId"
  val nrOfEnabledEventsField: String = "nrOfEnabledEvents"
  val preConditionsField: String = "pre_conditions"
  val pathConditionsField: String = "path_conditions"
  val globalSymbolicInputsField: String = "global_symbolic_inputs"
  val prescribedEventsField: String = "prescribed_events"

  val typeField: String = "type"
  val concreteField: String = "concrete"
  val symbolicField: String = "symbolic"

  val processesInfoField: String = "processes_info"
  val totalNrOfEventsField: String = "totalNrOfEvents"
}

object MetaMessageTypes extends Enumeration {
  val TerminationRequestType: MetaMessageTypes.Value = Value
}

object OperatorNames {
  val greaterThanType: String = ">"
  val greaterThanEqualType: String = ">="
  val lessThanType: String = "<"
  val lessThanEqualType: String = "<="
  val equalType: String = "=="
  val nonEqualType: String = "!="

  val plusType: String = "+"
  val minusType: String = "-"
  val timesType: String = "*"
  val divideType: String = "/"
  val moduloType: String = "%"

  val notType: String = "!"
  val intInverseType: String = "-"
  val andType: String = "&&"
  val orType: String = "||"
  val boolEqualType: String = "bool_equal"

  val intToString: String = "int_to_string"
  val stringToInt: String = "string_to_int"
  val stringEqualType: String = "string_equal"
  val stringIncludesType: String = "string_includes"
  val stringPrefixType: String = "string_prefix"
  val stringSuffixType: String = "string_suffix"
  val stringSubstringType: String = "string_substring"
  val stringAppendType: String = "string_append"
  val stringReplaceType: String = "string_replace"
  val stringAtType: String = "string_at"
  val stringIndexOfType: String = "string_index_of"
  val stringLengthType: String = "string_length"

  val eventChosenType: String = "event_chosen"
  val eventNotChosenType: String = "event_not_chosen"

  def operatorToString(operator: SymbolicOperator): String = operator match {
    case IntGreaterThan => OperatorNames.greaterThanType
    case IntGreaterThanEqual => OperatorNames.greaterThanEqualType
    case IntLessThan => OperatorNames.lessThanType
    case IntLessThanEqual => OperatorNames.lessThanEqualType
    case IntEqual => OperatorNames.equalType
    case IntNonEqual => OperatorNames.nonEqualType

    case IntPlus => OperatorNames.plusType
    case IntMinus => OperatorNames.minusType
    case IntTimes => OperatorNames.timesType
    case IntDiv => OperatorNames.divideType
    case IntModulo => OperatorNames.moduloType
    case IntInverse => OperatorNames.intInverseType

    case BooleanEqual => OperatorNames.boolEqualType
    case LogicalNot => OperatorNames.notType
    case LogicalAnd => OperatorNames.andType
    case LogicalOr => OperatorNames.orType

    case IntToString => OperatorNames.intToString
    case StringToInt => OperatorNames.stringToInt
    case StringEqual => OperatorNames.stringEqualType
    case StringIsSubstring => OperatorNames.stringIncludesType
    case StringPrefixOf => OperatorNames.stringPrefixType
    case StringSuffixOf => OperatorNames.stringSuffixType
    case StringGetSubstring => OperatorNames.stringSubstringType
    case StringAppend => OperatorNames.stringAppendType
    case StringReplace => OperatorNames.stringReplaceType
    case StringAt => OperatorNames.stringAtType
    case StringIndexOf => OperatorNames.stringIndexOfType
    case StringLength => OperatorNames.stringLengthType

    case EventChosenOperator => OperatorNames.eventChosenType
    case EventNotChosenOperator => OperatorNames.eventNotChosenType
  }

  def relationalOperationToBinarySymbolicOperator(operator: String): BinaryRelationalOperator =
    operator match {
      /* Integer relational operations */
      case OperatorNames.greaterThanType => IntGreaterThan
      case OperatorNames.greaterThanEqualType => IntGreaterThanEqual
      case OperatorNames.lessThanType => IntLessThan
      case OperatorNames.lessThanEqualType => IntLessThanEqual
      case OperatorNames.equalType => IntEqual
      case OperatorNames.nonEqualType => IntNonEqual

      /* String relational operations */
      case OperatorNames.stringEqualType => StringEqual
      case OperatorNames.stringIncludesType => StringIsSubstring
      case OperatorNames.stringPrefixType => StringPrefixOf
      case OperatorNames.stringSuffixType => StringSuffixOf
    }
  def stringToIntArithmeticOp(operator: String): IntegerArithmeticalOperator = operator match {
    case OperatorNames.plusType => IntPlus
    case OperatorNames.minusType => IntMinus
    case OperatorNames.timesType => IntTimes
    case OperatorNames.divideType => IntDiv
    case OperatorNames.moduloType => IntModulo
  }
  def stringToStringExpressionOp(operator: String): StringOperator = operator match {
    case OperatorNames.stringEqualType => StringEqual
    case OperatorNames.stringIncludesType => StringIsSubstring
    case OperatorNames.stringPrefixType => StringPrefixOf
    case OperatorNames.stringSuffixType => StringSuffixOf
    case OperatorNames.stringSubstringType => StringGetSubstring
    case OperatorNames.stringAppendType => StringAppend
    case OperatorNames.stringReplaceType => StringReplace
    case OperatorNames.stringAtType => StringAt
    case OperatorNames.stringIndexOfType => StringIndexOf
    case OperatorNames.stringLengthType => StringLength
  }
  def stringToLogicalUnaryOp(operator: String): UnaryOperator = operator match {
    case OperatorNames.notType => LogicalNot
    case OperatorNames.intInverseType => IntInverse
  }
  def stringToLogicalBinaryOp(operator: String): LogicalBinaryOperator = operator match {
    case OperatorNames.andType => LogicalAnd
    case OperatorNames.orType => LogicalOr
  }
}

trait PCElementConverter[C <: Constraint] {
  @throws[UnexpectedJSONFormat]
  def convertPCElement(element: JsValue): PCElement[C]
}

object ParseBranchCoverageMap extends JsonReader[Option[BranchCoverageMap]] {
  object BranchCoverageInfoFields {
    val codePositionField: String = "codePosition"
    val elseBranchTakenField: String = "elseBranch"
    val thenBranchTakenField: String = "thenBranch"
  }

  override def read(jsValue: JsValue): Option[BranchCoverageMap] = {
    val jsObject = jsValueToJsObject(jsValue)
    jsObject.fields.get(ExpFields.branchCoverageInfoField) match {
      case None => None
      case Some(jsValue) =>
        val codePositionsMap = jsValueToJsArray(jsValue)
        val codePositionDirectionsTuples = codePositionsMap.elements.toList.map(tupleJsValue => {
          val tupleJsObject = jsValueToJsObject(tupleJsValue)
          val codePosition = implicitly[JsonReader[SerialCodePosition]].read(
            tupleJsObject.fields(BranchCoverageInfoFields.codePositionField))
          val elseBranchTaken = tupleJsObject.fields(
            BranchCoverageInfoFields.elseBranchTakenField).asInstanceOf[JsBoolean].value
          val thenBranchTaken = tupleJsObject.fields(
            BranchCoverageInfoFields.thenBranchTakenField).asInstanceOf[JsBoolean].value
          val directions = Set[Direction]().union(if (elseBranchTaken) Set(ElseDirection) else Set()).union(
            if (thenBranchTaken) Set(ThenDirection) else Set())
          (codePosition, directions)
        })
        val map = codePositionDirectionsTuples.foldLeft(Map[CodePosition, Set[Direction]]())((acc, tuple) => {
          acc + tuple
        })
        Some(BranchCoverageMap(map))
    }
  }
}

abstract class JSONParsing[C <: Constraint : ConstraintNegater : Optimizer] {

  import JsonParseSymbolicExp._

  @throws[UnexpectedJSONFormat]
  def parseDirections(direction: JsValue): List[Direction] = direction match {
    case JsString(path) => stringToPath(path)
    case _ => throw UnexpectedJSONFormat(direction)
  }

  @throws[UnexpectedJSONFormat]
  def parseTargetTriggered(targetTriggeredJSON: JsValue): TargetTriggered =
    targetTriggeredJSON match {
      case jsObject: JsObject =>
        val processId: Int = CommonOperations.getIntField(jsObject, ExpFields.processIdField)
        val targetId: Int = CommonOperations.getIntField(jsObject, ExpFields.targetIdField)
        TargetTriggered(processId, targetId)
      case _ => throw UnexpectedJSONFormat(targetTriggeredJSON)
    }

  def parseEventHandlersCoverageInfo(jsObject: JsObject): List[String] = {
    val jsList = CommonOperations.getArrayField(jsObject, ExpFields.eventHandlersCoverageInfoField).toList
    jsList.map(jsValue => jsStringToString(jsValue))
  }

  def parseProcessesInfo(jsonInput: JsObject): List[Int] = {
    CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.processesInfoField)
      .toList
      .zipWithIndex
      .map({
        case (jsObject: JsObject, index) =>
          val processId = CommonOperations.getIntField(jsObject, ExpFields.idField)
          assert(
            processId == index) // Check whether the list of processes is ordered sequentially: i.e., process 0 comes just before process 1
          val totalNrOfEvents =
            CommonOperations.getIntField(jsObject, SMTSolveProcessFields.totalNrOfEventsField)
          totalNrOfEvents
        case tuple => throw UnexpectedJSONFormat(tuple._1)
      })
  }

  protected def symbolicExpToBooleanSymbolicExp(
    symbolicExp: SymbolicExpression
  ): BooleanExpression = {
    symbolicExp match {
      case bce: BooleanExpression => bce
      case convertibleExp: ConvertibleToBooleanExpression => convertibleExp.toBool
    }
  }

  @throws[JSONParsingException]
  protected[communication] def checkType(
    jsonInput: JsValue,
    acceptedTypes: List[String]
  ): JsObject = {
    jsonInput match {
      case jsonInput: JsObject =>
        val actualType = CommonOperations.getType(jsonInput)
        if (!acceptedTypes.contains(actualType)) {
          throw UnexpectedInputType(actualType)
        } else {
          jsonInput
        }
      case _ => throw UnexpectedJSONFormat(jsonInput)
    }
  }

  @throws[UnexpectedJSONFormat]
  protected[communication] def argJsonValueToConcreteValue(
    concreteValueJsonObject: JsObject
  ): (ConcreteValue, SymbolicExpression) = {
    val concreteValueType =
      CommonOperations.getStringField(concreteValueJsonObject, SMTSolveProcessFields.typeField)
    val concreteJsonValue =
      CommonOperations.getField(concreteValueJsonObject, SMTSolveProcessFields.concreteField)
    val symbolicJsonValue =
      CommonOperations.getField(concreteValueJsonObject, SMTSolveProcessFields.symbolicField)
    val identifier = JsonParseIdentifier.parseIdentifier(concreteValueJsonObject)
    val concreteValue = concreteValueType match {
      case name if TypeNames.isBool(name) =>
        SymbolicBool(CommonOperations.jsBooleanToBoolean(concreteJsonValue), identifier)
      case name if TypeNames.isInt(name) =>
        SymbolicInt(CommonOperations.jsNumberToInt(concreteJsonValue), identifier)
      case name if TypeNames.isString(name) =>
        SymbolicString(CommonOperations.jsStringToString(concreteJsonValue), identifier)
    }
    val symbolicValue = symbolicToExp(symbolicJsonValue)
    (concreteValue, symbolicValue)
  }

  protected def getPCElementConverter: PCElementConverter[C]

  @throws[UnexpectedInputType]
  private[communication] def extractFormula(jsonInput: JsObject, fieldName: String): Formula

}

class ExploreTreeJsonParsing extends EventConstraintJSONParsing {

  def parse(parsedJSON: ParsedJSON): ExploreModeJSONInput = {
    val jsObject = checkType(parsedJSON.json, List("explore", "e"))
    val inputType = CommonOperations.getStringField(jsObject, ExploreModeFields.inputTypeField)
    inputType match {
      case ExploreModeFields.newPathType => parseJsonExploreMode(jsObject)
    }
  }
  @throws[UnexpectedJSONFormat]
  protected[communication] def parseJsonExploreMode(jsonInput: JsObject): ExploreModeInput[EventConstraint, RegularPCElement[EventConstraint]] = {
    val iteration = CommonOperations.getIntField(jsonInput, "iteration")
    val processesInfo: List[Int] = parseProcessesInfo(jsonInput)
    val pathConstraint = parsePC(CommonOperations.getField(jsonInput, "PC"), processesInfo)
    val finishedTestRun = CommonOperations.getBooleanField(jsonInput, ExpFields.finishedTestRunField)
    val optCoverageInfo = ParseBranchCoverageMap.read(jsonInput)
    val optEventHandlerCoverageInfo = parseEventHandlersCoverageInfo(jsonInput)
    ExploreModeInput[EventConstraint, RegularPCElement[EventConstraint]](
      iteration, pathConstraint, processesInfo, finishedTestRun, optCoverageInfo, Some(optEventHandlerCoverageInfo))(EventConstraintNegater, EventConstraintOptimizer)
  }
  protected[communication] def parsePC(jsValue: JsValue, processesInfo: List[Int]): PathConstraint[EventConstraint] = {
    var stopTestingEncountered = true
    val jsArray = CommonOperations.jsValueToJsArray(jsValue)
    val converter = EventConstraintPCConverter(processesInfo)
    val tempPC = jsArray.elements.map(converter.convertPCElement).toList
    /* StopTestingTarget constraints have all automatically been set to true during parsing,
     * because parser only looks at individual constraints instead of the complete path and hence
     * did now know whether or not the path implied that more events had been fired.
     * Loop over the path again, in reverse order, and set the first StopTestingTargets
     * (i.e., the last StopTestingTargets in the non-reversed constraint) to false,
     * while all other StopTestingTargets are set to true.
     */
    tempPC.reverse.map((pcElement: RegularPCElement[EventConstraint]) => pcElement match {
      case RegularPCElement(constraint: StopTestingTargets, _) =>
        RegularPCElement[EventConstraint](constraint, stopTestingEncountered)
      case element@RegularPCElement(_: TargetChosen, _) =>
        stopTestingEncountered = false
        element
      case other => other
    }).reverse
  }
  object ExploreModeFields {
    val inputTypeField: String = "explore_type"

    val newPathType: String = "get_new_path"
  }
}

class ExploreTreeMergeJsonParsing extends ConstraintESJSONParsing {

  def parse(parsedJSON: ParsedJSON): ExploreMergeModeJSONInput = {
    val jsObject = checkType(parsedJSON.json, List("explore_merge", "em"))
    val inputType = CommonOperations.getStringField(jsObject, ExploreMergeModeFields.inputTypeField)
    inputType match {
      case ExploreMergeModeFields.newPathType => parseJsonExploreMode(jsObject)
      case ExploreMergeModeFields.tryMergeType => parseJsonTryMerge(jsObject)
    }
  }
  protected[communication] def parseJsonExploreMode(jsObject: JsObject): ExploreMergeModeInput = {
    val iteration = CommonOperations.getIntField(jsObject, "iteration")
    val processesInfo: List[Int] = parseProcessesInfo(jsObject)
    val jsPrescribedEvents = CommonOperations.getField(jsObject, SMTSolveProcessFields.prescribedEventsField)
    val prescribedEvents = implicitly[JsonReader[List[(Int, Int)]]].read(jsPrescribedEvents)
    val pathConstraint = parsePC(CommonOperations.getField(jsObject, "PC"), processesInfo, prescribedEvents.length)
    val finishedTestRun = CommonOperations.getBooleanField(jsObject, ExpFields.finishedTestRunField)
    val optCoverageInfo = ParseBranchCoverageMap.read(jsObject)
    val optEventHandlerCoverageInfo = parseEventHandlersCoverageInfo(jsObject)
    ExploreMergeModeInput(ExploreModeInput(iteration, pathConstraint, processesInfo, finishedTestRun, optCoverageInfo, Some(optEventHandlerCoverageInfo)))
  }
  @throws[UnexpectedJSONFormat]
  def parseJsonTryMerge(jsObject: JsObject): TryMerge = {
    val i = CommonOperations.getIntField(jsObject, "i")
    val jsExecutionState = CommonOperations.getObjectField(jsObject, ExploreMergeModeFields.executionStateField)
    val executionState = implicitly[JsonReader[ExecutionState]].read(jsExecutionState)
    val iteration = CommonOperations.getIntField(jsObject, SMTSolveProcessFields.iterationField)
    val processesInfo: List[Int] = parseProcessesInfo(jsObject)
    val jsPrescribedEvents = CommonOperations.getField(jsObject, SMTSolveProcessFields.prescribedEventsField)
    val prescribedEvents = implicitly[JsonReader[List[(Int, Int)]]].read(jsPrescribedEvents)
    val pathConstraint = parsePC(CommonOperations.getField(jsObject, "PC"), processesInfo, prescribedEvents.length)
    TryMerge(iteration, executionState, pathConstraint, prescribedEvents, i)
  }
  protected[communication] def parsePC(
    jsValue: JsValue,
    processesInfo: List[Int],
    nrOfPrescribedEvents: Int
  ): PathConstraintWithStoreUpdates[ConstraintES] = {
    val jsArray = CommonOperations.jsValueToJsArray(jsValue)
    val converter = new ConstraintESPCConverter(processesInfo)
    val tempPC = jsArray.elements.map(converter.convertPCElement).toList
    /* StopTestingTarget constraints have all automatically been set to true during parsing,
     * because parser only looks at individual constraints instead of the complete path and hence
     * did now know whether or not the path implied that more events had been fired.
     * Loop over the path again, in reverse order, and set the first StopTestingTargets
     * (i.e., the last StopTestingTargets in the non-reversed constraint) to false,
     * while all other StopTestingTargets are set to true.
     */
    val stopTestingConstraintsCorrected: PathConstraintWithStoreUpdates[ConstraintES] = tempPC.map {
      case ConstraintWithStoreUpdate(constraint@EventConstraintWithExecutionState(stt: StopTestingTargets, _), _) =>
        ConstraintWithStoreUpdate(constraint: ConstraintES, stt.id >= nrOfPrescribedEvents - 1)
      case element@ConstraintWithStoreUpdate(EventConstraintWithExecutionState(_: TargetChosen, _), _) =>
        element
      case other => other
    }
    val assignmentConstraintsMerged = stopTestingConstraintsCorrected.foldLeft(
      Nil: PathConstraintWithStoreUpdates[ConstraintES])((pc, pcElement) => pcElement match {
      case StoreUpdatePCElement(tac: TemporaryAssignmentStoreUpdate) =>
        if (JsonParseIdentifier.ignoreIdentifier(tac.identifier)) {
          pc
        } else {
          pc.lastOption match {
            case Some(StoreUpdatePCElement(AssignmentsStoreUpdate(map))) =>
              val extendedMap = map :+ (tac.identifier, tac.exp)
              val assignmentsStoreUpdate = AssignmentsStoreUpdate(extendedMap)
              pc.init :+ StoreUpdatePCElement(assignmentsStoreUpdate) // Replace last AssignmentsConstraint
            case _ =>
              val assignmentsStoreUpdate = AssignmentsStoreUpdate(List((tac.identifier, tac.exp)))
              pc :+ StoreUpdatePCElement(assignmentsStoreUpdate)
          }
        }
      case _ => pc :+ pcElement
    })
    assignmentConstraintsMerged
  }
  object ExploreMergeModeFields {
    val inputTypeField: String = "explore_type"

    val newPathType: String = "get_new_path"
    val tryMergeType: String = "try_merge"

    // TryMergeInput
    val executionStateField: String = "execution_state"
  }

}

class FunctionSummariesJsonParsing extends EventConstraintJSONParsing {

  import JsonParseSymbolicExp._

  def parse(parsedJSON: ParsedJSON): FunctionSummaryInput = {
    val jsObject = checkType(parsedJSON.json, List("function_summaries", "fs", "f"))
    parseJsonObjectFunctionSummariesMode(jsObject)
  }

  protected def parseJsonObjectFunctionSummariesMode(jsonInput: JsObject): FunctionSummaryInput = {
    val fsType = CommonOperations.getStringField(jsonInput, "fs_type")
    fsType match {
      case "compute_prefix" => parseJsonFSComputePrefix(jsonInput)
      case "explore_function" => parseJsonFSExploreFunction(jsonInput)
      case "satisfies_formula" => parseJsonFSCheckSatisfiesformula(jsonInput)
    }
  }

  protected def parseJsonFSComputePrefix(jsonInput: JsObject): FSComputePrefix = {
    val jsonArgs = CommonOperations.getArrayField(jsonInput, ExpFields.argumentsField)
    val values: List[(ConcreteValue, SymbolicExpression)] =
      jsonArgs.map((jsValue: JsValue) => argJsonValueToConcreteValue(jsValue.asJsObject)).toList
    val correspondingInputsJson =
      CommonOperations.getArrayField(jsonInput, SMTSolveProcessFields.correspondingInputsField)
    val correspondingInputs: List[SymbolicInput] = correspondingInputsJson
      .map((jsValue: JsValue) => convertJsObjectToSymbolicFunctionInput(jsValue.asJsObject))
      .toList
    val prefix = CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.prefixField)
      .map(EventConstraintPCConverter(Nil).convertPCElement)
      .toList
    FSComputePrefix(values, correspondingInputs, prefix)
  }

  protected def parseJsonFSExploreFunction(jsonInput: JsObject): FSExploreFunction = {
    val iteration = CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.iterationField)
    val functionId = CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.functionIdField)
    val argsJson = CommonOperations.getArrayField(jsonInput, SMTSolveProcessFields.argumentsField)
    val values: List[(ConcreteValue, SymbolicExpression)] =
      argsJson.map((jsValue: JsValue) => argJsonValueToConcreteValue(jsValue.asJsObject)).toList
    val correspondingInputsJson =
      CommonOperations.getArrayField(jsonInput, SMTSolveProcessFields.correspondingInputsField)
    val correspondingInputs: List[SymbolicInput] = correspondingInputsJson
      .map((jsValue: JsValue) => convertJsObjectToSymbolicFunctionInput(jsValue.asJsObject))
      .toList
    val prefix = CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.prefixField)
      .map(EventConstraintPCConverter(Nil).convertPCElement)
      .toList
    val suffix = CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.suffixField)
      .map(EventConstraintPCConverter(Nil).convertPCElement)
      .toList
    val processId = CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.processIdField)
    val nrOfEnabledEvents =
      CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.nrOfEnabledEventsField)
    communication.FSExploreFunction(
      iteration,
      functionId,
      values,
      correspondingInputs,
      prefix,
      suffix,
      processId,
      nrOfEnabledEvents)
  }

  protected def parseJsonFSCheckSatisfiesformula(jsonInput: JsObject): FSCheckSatisfiesFormula = {
    val iteration = CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.iterationField)
    val functionId = CommonOperations.getIntField(jsonInput, SMTSolveProcessFields.functionIdField)
    val argsJson = CommonOperations.getArrayField(jsonInput, ExpFields.argumentsField)
    val values: List[(ConcreteValue, SymbolicExpression)] =
      argsJson.map((jsValue: JsValue) => argJsonValueToConcreteValue(jsValue.asJsObject)).toList
    val correspondingInputsJson =
      CommonOperations.getArrayField(jsonInput, SMTSolveProcessFields.correspondingInputsField)
    val correspondingInputs: List[SymbolicInput] = correspondingInputsJson
      .map((jsValue: JsValue) => convertJsObjectToSymbolicFunctionInput(jsValue.asJsObject))
      .toList
    //    val preConditions = preConditionsJson.map(convertPCElement(_, Nil)).toList
    val preConditions = extractFormula(jsonInput, SMTSolveProcessFields.preConditionsField)
    val pathConditions = extractFormula(jsonInput, SMTSolveProcessFields.pathConditionsField)
    val globalInputsJson =
      CommonOperations.getArrayField(jsonInput, SMTSolveProcessFields.globalSymbolicInputsField)
    val globalInputs: List[(SymbolicInput, ConcreteValue)] = globalInputsJson
      .map((jsValue: JsValue) => {
        val (concreteValue, symbolicInput) =
          argJsonValueToConcreteValue(CommonOperations.jsValueToJsObject(jsValue))
        (symbolicInput.asInstanceOf[SymbolicInput], concreteValue)
      })
      .toList
    FSCheckSatisfiesFormula(
      iteration,
      functionId,
      values,
      correspondingInputs,
      preConditions,
      pathConditions,
      globalInputs)
  }
}

class SolvePathJsonParsing extends EventConstraintJSONParsing {

  def parse(parsedJSON: ParsedJSON): SolveModeInput[EventConstraint] = {
    val jsObject = checkType(parsedJSON.json, List("solve", "s"))
    parseJsonObjectSolveMode(jsObject)
  }

  protected def parseJsonObjectSolveMode(jsonInput: JsObject): SolveModeInput[EventConstraint] = {
    val branchSeq: List[Direction] = parseDirections(
      jsonInput.fields(SMTSolveProcessFields.branchSeqField))
    val eventSeq: List[TargetTriggered] = CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.eventSeqField)
      .map(parseTargetTriggered)
      .toList
    val pathConstraint: PathConstraint[EventConstraint] = CommonOperations
      .getArrayField(jsonInput, SMTSolveProcessFields.pathConstraintField)
      .map(EventConstraintPCConverter(Nil).convertPCElement)
      .toList
    SolveModeInput(pathConstraint, eventSeq, branchSeq)
  }
}

class VerifyIntraJsonParsing extends EventConstraintJSONParsing {
  val exploreTreeParsing = new ExploreTreeJsonParsing

  def parse(parsedJSON: ParsedJSON): VerifyIntraInput = {
    val jsObject = checkType(parsedJSON.json, List("explore", "e", "verify_intra", "verify", "v"))
    val parsedInput = parseJsonObjectVerifyIntra(jsObject)
    parsedInput
  }

  def parseJsonObjectVerifyIntra(jsonInput: JsObject): VerifyIntraInput = {
    val viType = CommonOperations.getStringField(jsonInput, "explore_type")
    viType match {
      case "explore" =>
        new VIExploreModeInput(exploreTreeParsing.parseJsonExploreMode(jsonInput))
      case "connect" =>
        val pc1: PathConstraint[EventConstraint] =
          CommonOperations.getArrayField(jsonInput, "pc1").map(EventConstraintPCConverter(Nil).convertPCElement).toList
        val pc2: PathConstraint[EventConstraint] =
          CommonOperations.getArrayField(jsonInput, "pc2").map(EventConstraintPCConverter(Nil).convertPCElement).toList
        VIConnectPaths(pc1, pc2)
    }
  }
}

case class ParsedJSON(json: JsValue) {
  def parseOptMetaInput: Option[MetaInput] = {
    val messageType: String = CommonOperations.getType(json.asJsObject("Expected a JsObject"))
    messageType match {
      case "successfully_terminated" => Some(TerminationRequest)
      case _ => None
    }
  }
}

object ParsedJSON {
  def apply(inputString: String): (Int, ParsedJSON) = {
    val json = JsonParser(inputString)
    val id: Int =
      CommonOperations.getIntField(CommonOperations.jsValueToJsObject(json), "backend_id")
    (id, new ParsedJSON(json))
  }
}
