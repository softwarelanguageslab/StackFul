package backend.communication

import backend.expression._
import backend.communication.json._

import spray.json._

object JsonParseSymbolicExp {

  def symbolicToBooleanExp(exp: SymbolicExpression): BooleanExpression =
    exp match {
      case booleanExp: BooleanExpression => booleanExp
      case convertibleExp: ConvertibleToBooleanExpression => convertibleExp.toBool
    }

  @throws[UnexpectedJSONFormat]
  def symbolicToExp(jsValue: JsValue): SymbolicExpression = {
    jsValue match {
      case jsObject: JsObject =>
        getJsObjectType(jsObject) match {
          case ExpTypes.SymbolicNothing => SymbolicNothingExpression
          case ExpTypes.SymbolicBool => convertJsObjectToSymbolicBool(jsObject)
          case ExpTypes.SymbolicFunctionInput => convertJsObjectToSymbolicFunctionInput(jsObject)
          case ExpTypes.SymbolicInputInt => convertJsObjectToSymbolicInputInt(jsObject)
          case ExpTypes.SymbolicInputString => convertJsObjectToSymbolicInputString(jsObject)
          case ExpTypes.SymbolicInt => convertJsObjectToSymbolicInt(jsObject)
          case ExpTypes.SymbolicString => convertJsObjectToSymbolicString(jsObject)
          case ExpTypes.SymbolicReturnValue => convertJsObjectToSymbolicReturnValue(jsObject)
          case ExpTypes.SymbolicFunction => convertJsObjectToSymbolicFunction(jsObject)
          case ExpTypes.SymbolicArithmeticExp => convertJsObjectToArithmeticalSymbolicExp(jsObject)
          case ExpTypes.SymbolicRelationalExp => convertJsObjectToSymbolicRelationalExp(jsObject)
          case ExpTypes.SymbolicStringOperationExp =>
            convertJsObjectToStringOperationSymbolicExp(jsObject)
          case ExpTypes.SymbolicUnaryExp => convertJsObjectToSymbolicUnaryExp(jsObject)
          case ExpTypes.SymbolicLogicalBinExpression =>
            convertJsObjectToSymbolicLogicalBinRelationExp(jsObject)
          case ExpTypes.SymbolicMessageInput => convertJsObjectToMessageInput(jsObject)
          case ExpTypes.SymbolicEventChosen => convertJsObjectToEventChosen(jsObject)
        }
      case _ => throw UnexpectedJSONFormat(jsValue)
    }
  }

  protected[communication] def getJsObjectType(jsObject: JsObject): ExpTypes.Value = {
    val value = CommonOperations.getStringField(jsObject, ExpFields.typeField)
    value match {
      case ExpTypes.nothingType => ExpTypes.SymbolicNothing
      case ExpTypes.boolType => ExpTypes.SymbolicBool
      case ExpTypes.floatType => ExpTypes.SymbolicFloat
      case ExpTypes.intType => ExpTypes.SymbolicInt
      case ExpTypes.stringType => ExpTypes.SymbolicString
      case ExpTypes.functionInputType => ExpTypes.SymbolicFunctionInput
      case ExpTypes.inputIntType => ExpTypes.SymbolicInputInt
      case ExpTypes.inputStringType => ExpTypes.SymbolicInputString
      case ExpTypes.returnValueType => ExpTypes.SymbolicReturnValue
      case ExpTypes.symbolicFunctionType => ExpTypes.SymbolicFunction
      case ExpTypes.arithmeticExpType => ExpTypes.SymbolicArithmeticExp
      case ExpTypes.relationalExpType => ExpTypes.SymbolicRelationalExp
      case ExpTypes.stringOperationExpType => ExpTypes.SymbolicStringOperationExp
      case ExpTypes.unaryExpType => ExpTypes.SymbolicUnaryExp
      case ExpTypes.logicanBinExpType => ExpTypes.SymbolicLogicalBinExpression
      case ExpTypes.messageInputType => ExpTypes.SymbolicMessageInput
      case ExpTypes.eventChosenType => ExpTypes.SymbolicEventChosen
      case ExpTypes.rawFunctionInputType => ExpTypes.RawSymbolicFunctionInput
      case ExpTypes.rawReturnValueType => ExpTypes.RawSymbolicReturnValue
    }
  }

  protected[communication] def convertJsObjectToSymbolicBool(jsObject: JsObject): SymbolicBool = {
    SymbolicBool(
      CommonOperations.getBooleanField(jsObject, ExpFields.bField),
      JsonParseIdentifier.parseIdentifier(jsObject))
  }

  protected[communication] def convertJsObjectToSymbolicInt(jsObject: JsObject): SymbolicInt = {
    SymbolicInt(
      CommonOperations.getIntField(jsObject, ExpFields.iField),
      JsonParseIdentifier.parseIdentifier(jsObject))
  }

  protected[communication] def convertJsObjectToSymbolicString(
    jsObject: JsObject
  ): SymbolicString = {
    SymbolicString(
      CommonOperations.getStringField(jsObject, ExpFields.sField),
      JsonParseIdentifier.parseIdentifier(jsObject))
  }
  protected[communication] def convertJsObjectToSymbolicFunctionInput(
    jsObject: JsObject
  ): SymbolicInput = {
    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
    val functionId = CommonOperations.getIntField(jsObject, IdFields.functionIdField)
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    val timesCalled = CommonOperations.getIntField(jsObject, IdFields.timesCalledField)
    val inputType = CommonOperations.getStringField(jsObject, ExpFields.inputTypeField)
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    val functionInputId = FunctionInputId(functionId, processId, id, timesCalled)
    inputType match {
      case TypeNames.boolTypeName => SymbolicInputBool(functionInputId, identifier)
      case TypeNames.floatTypeName => SymbolicInputFloat(functionInputId, identifier)
      case TypeNames.intTypeName => SymbolicInputInt(functionInputId, identifier)
      case TypeNames.stringTypeName => SymbolicInputString(functionInputId, identifier)
    }
  }

  protected def convertJsObjectToSymbolicInputInt(jsObject: JsObject): SymbolicInputInt = {
//    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
//    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    val id = implicitly[JsonReader[SymbolicInputId]].read(jsObject)
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    SymbolicInputInt(id, identifier)
  }

  protected def convertJsObjectToSymbolicInputString(jsObject: JsObject): SymbolicInputString = {
//    val id = CommonOperations.getIntField(jsObject, IdFields.idField)
//    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    val id = implicitly[JsonReader[SymbolicInputId]].read(jsObject)
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    SymbolicInputString(id, identifier)
  }

  protected def convertJsObjectToSymbolicReturnValue(jsObject: JsObject): SymbolicIntVariable = {
    val processId = CommonOperations.getIntField(jsObject, IdFields.processIdField)
    val functionId = CommonOperations.getIntField(jsObject, IdFields.functionIdField)
    val timesCalled = CommonOperations.getIntField(jsObject, IdFields.timesCalledField)
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    val functionReturnId =
      FunctionReturnId(functionId = functionId, timesCalled = timesCalled, processId = processId)
    SymbolicIntVariable(SymbolicInputInt(functionReturnId, identifier))
  }
  protected def convertJsObjectToSymbolicFunction(jsObject: JsObject): SymbolicFunction = {
    val serial = CommonOperations.getIntField(jsObject, ExpFields.serialField)
    val listListStringJsonReader = ListJsonReader(
      ListJsonReader((jsValue: JsValue) => CommonOperations.jsStringToString(jsValue)))
    //    val scopes: List[List[String]] = listListStringJsonReader.read(CommonOperations.getField(jsObject, ExpFields.environmentField))
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    SymbolicFunction(serial, identifier)
  }
  protected def convertJsObjectToArithmeticalSymbolicExp(
    jsObject: JsObject
  ): ArithmeticalVariadicOperationExpression = {
    val op = OperatorNames.stringToIntArithmeticOp(
      CommonOperations.getStringField(jsObject, ExpFields.operatorField))
    val args: List[SymbolicExpression] =
      CommonOperations.getArrayField(jsObject, ExpFields.argsField).map(symbolicToExp).toList
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    ArithmeticalVariadicOperationExpression(op, args, identifier)
  }
  protected def convertJsObjectToSymbolicRelationalExp(jsObject: JsObject): RelationalExpression = {
    val op = OperatorNames.relationalOperationToBinarySymbolicOperator(
      CommonOperations.getStringField(jsObject, ExpFields.operatorField))
    val left = symbolicToExp(jsObject.fields(ExpFields.leftField))
    val right = symbolicToExp(jsObject.fields(ExpFields.rightField))
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    RelationalExpression(left, op, right, identifier)
  }
  @throws[UnexpectedInputValue]
  protected def convertJsObjectToStringOperationSymbolicExp(
    jsObject: JsObject
  ): SymbolicExpression = {
    val op = OperatorNames.stringToStringExpressionOp(
      CommonOperations.getStringField(jsObject, ExpFields.operatorField))
    val args: List[SymbolicExpression] =
      CommonOperations.getArrayField(jsObject, ExpFields.argsField).map(symbolicToExp).toList
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    op match {
      case op: StringRelationalOperator =>
        assert(
          args.length == 2,
          s"Expected args.length to be 2, but was ${args.length}. Args = $args")
        RelationalExpression(args.head, op, args(1), identifier)
      case _ =>
        op match {

          case StringAppend =>
            assert(args.length == 2)
            StringOperationProducesStringExpression(op, args, identifier)
          case StringReplace =>
            assert(args.length == 3)
            StringOperationProducesStringExpression(op, args, identifier)
          case StringAt =>
            assert(args.length == 2)
            assert(args(1).isInstanceOf[SymbolicInt])
            StringOperationProducesStringExpression(op, args, identifier)
          case StringLength =>
            assert(args.length == 1)
            StringOperationProducesIntExpression(op, args, identifier)
          case StringIndexOf =>
            assert(args.length == 2 || args.length == 3)
            StringOperationProducesIntExpression(op, args, identifier)
          case StringGetSubstring =>
            assert(args.length == 3)
            assert(args(1).isInstanceOf[SymbolicInt])
            assert(args(2).isInstanceOf[SymbolicInt])
            StringOperationProducesStringExpression(op, args, identifier)
          case _ => throw UnexpectedInputValue(op)
        }
    }
  }
  protected def convertJsObjectToSymbolicLogicalUnaryExp(
    op: LogicalUnaryOperator,
    jsObject: JsObject
  ): LogicalUnaryExpression = {
    val argument = symbolicToBooleanExp(
      symbolicToBooleanExp(symbolicToExp(jsObject.fields(ExpFields.argumentField))))
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    LogicalUnaryExpression(op, argument, identifier)
  }
  protected def convertJsObjectToArithmeticalSymbolicUnaryExp(
    op: ArithmeticalUnaryOperator,
    jsObject: JsObject
  ): ArithmeticalVariadicOperationExpression = {
    val argument = symbolicToExp(jsObject.fields(ExpFields.argumentField))
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    ArithmeticalVariadicOperationExpression(op, List(argument), identifier)
  }
  protected def convertJsObjectToSymbolicUnaryExp(jsObject: JsObject): SymbolicExpression = {
    val op = OperatorNames.stringToLogicalUnaryOp(
      CommonOperations.getStringField(jsObject, ExpFields.operatorField))
    op match {
      case LogicalNot => convertJsObjectToSymbolicLogicalUnaryExp(LogicalNot, jsObject)
      case IntInverse => convertJsObjectToArithmeticalSymbolicUnaryExp(IntInverse, jsObject)
    }
  }
  protected def convertJsObjectToSymbolicLogicalBinRelationExp(
    jsObject: JsObject
  ): LogicalBinaryExpression = {
    val op = OperatorNames.stringToLogicalBinaryOp(
      CommonOperations.getStringField(jsObject, ExpFields.operatorField))
    val left = symbolicToBooleanExp(symbolicToExp(jsObject.fields(ExpFields.leftField)))
    val right = symbolicToBooleanExp(symbolicToExp(jsObject.fields(ExpFields.rightField)))
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    LogicalBinaryExpression(left, op, right, identifier)
  }
  protected def convertJsObjectToMessageInput(jsObject: JsObject): SymbolicMessageInput = {
    val messageType = CommonOperations.getStringField(jsObject, ExpFields.messageTypeField)
    val id = CommonOperations.getIntField(jsObject, ExpFields.idField)
    val messageInputType =
      CommonOperations.getStringField(jsObject, ExpFields.messageInputTypeField)
    val identifier = JsonParseIdentifier.parseIdentifier(jsObject)
    val createSymbolicMessageInput = messageInputType match {
      case name if TypeNames.isBool(name) => SymbolicMessageInputBool
      case name if TypeNames.isFloat(name) => SymbolicMessageInputFloat
      case name if TypeNames.isInt(name) => SymbolicMessageInputInt
      case name if TypeNames.isString(name) => SymbolicMessageInputString
    }
    createSymbolicMessageInput(messageType, id, identifier)
  }
  protected def convertJsObjectToEventChosen(jsObject: JsObject): EventChosen = {
    val id = CommonOperations.getIntField(jsObject, ExpFields.idField)
    val processId = CommonOperations.getIntField(jsObject, ExpFields.processIdChosenField)
    val targetId = CommonOperations.getIntField(jsObject, ExpFields.targetIdChosenField)
    EventChosen(id, processId, targetId)
  }
  case class ListJsonReader[T](elementReader: JsonReader[T]) extends JsonReader[List[T]] {
    override def read(jsValue: JsValue): List[T] = {
      val vector = CommonOperations.jsValueToJsArray(jsValue).elements
      vector.map(elementReader.read).toList
    }
  }

}
