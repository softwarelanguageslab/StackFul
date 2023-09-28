package backend.json

import backend.communication._
import backend.expression._
import spray.json._

case class SymbolicExpressionJsonWriter() extends JsonWriter[SymbolicExpression] {

  def write(exp: SymbolicExpression): JsValue = exp match {
    case ace: ArithmeticalVariadicOperationExpression => writeArithmeticalSymbolicExp(ace)
    case cec: EventChosen => writeSymbolicEventChosen(cec)
    case bce: LogicalBinaryExpression => writeSymbolicLogicalBinExp(bce)
    case rce: RelationalExpression => writeRelationalSymbolicExp(rce)
    case soe: StringOperationProducesStringExpression => writeStringOperationProducesStringExp(soe)
    case soe: StringOperationProducesIntExpression => writeStringOperationProducesIntExp(soe)
    case uce: UnaryExpression => writeUnarySymbolicExpression(uce)
    case inp: SymbolicInput => writeSymbolicInput(inp)
    case min: SymbolicMessageInput => writeSymbolicMessageInput(min)
    case bol: SymbolicBool => writeSymbolicBool(bol)
    case int: SymbolicInt => writeSymbolicInt(int)
    case flt: SymbolicFloat => writeSymbolicFloat(flt)
    case str: SymbolicString => writeSymbolicString(str)
    case ite@SymbolicITEExpression(_, _, _, _) => writeSymbolicITEExp(ite)
  }

  private def writeArithmeticalSymbolicExp(
    ace: ArithmeticalVariadicOperationExpression
  ): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.arithmeticExpType),
      ExpFields.operatorField -> JsString(OperatorNames.operatorToString(ace.op)),
      ExpFields.argsField -> JsArray(ace.exps.map(write).toVector)
    )
  }

  private def writeSymbolicEventChosen(cec: EventChosen): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.eventChosenType),
      ExpFields.idField -> JsNumber(cec.id),
      ExpFields.processIdField -> JsNumber(cec.processId),
      ExpFields.targetIdField -> JsNumber(cec.targetId)
    )
  }

  private def writeSymbolicLogicalBinExp(bce: LogicalBinaryExpression): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.logicanBinExpType),
      ExpFields.operatorField -> JsString(OperatorNames.operatorToString(bce.op)),
      ExpFields.leftField -> write(bce.left),
      ExpFields.rightField -> write(bce.right)
    )
  }

  private def writeRelationalSymbolicExp(rce: RelationalExpression): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.relationalExpType),
      ExpFields.operatorField -> JsString(OperatorNames.operatorToString(rce.op)),
      ExpFields.leftField -> write(rce.left),
      ExpFields.rightField -> write(rce.right)
    )
  }

  private def writeStringOperationProducesStringExp(soe: StringOperationProducesStringExpression): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.stringOperationExpType),
      ExpFields.operatorField -> JsString(OperatorNames.operatorToString(soe.op)),
      ExpFields.argsField -> JsArray(soe.exps.map(write).toVector)
    )
  }

  private def writeStringOperationProducesIntExp(soe: StringOperationProducesIntExpression): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.stringOperationExpType),
      ExpFields.operatorField -> JsString(OperatorNames.operatorToString(soe.op)),
      ExpFields.argsField -> JsArray(soe.exps.map(write).toVector)
    )
  }

  private def writeUnarySymbolicExpression(uce: UnaryExpression): JsObject = {
    val argjsValue = write(uce match {
      case auce: ArithmeticalUnaryOperationExpression => auce.arg
      case luce: LogicalUnaryExpression => luce.arg
    })
    val opString = OperatorNames.operatorToString(uce match {
      case auce: ArithmeticalUnaryOperationExpression => auce.op
      case luce: LogicalUnaryExpression => luce.op
    })
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.unaryExpType),
      ExpFields.operatorField -> JsString(opString),
      ExpFields.argumentField -> argjsValue)
  }

  private def writeId(id: SymbolicInputId): JsObject = id match {
    case rid: RegularId =>
      JsObject(
        IdFields.idField -> JsNumber(rid.id),
        IdFields.processIdField -> JsNumber(rid.processId))
    case fid: FunctionId =>
      JsObject(
        IdFields.functionIdField -> JsNumber(fid.functionId),
        IdFields.idField -> JsNumber(fid.id),
        IdFields.processIdField -> JsNumber(fid.processId))
    case fid: FunctionInputId =>
      JsObject(
        IdFields.functionIdField -> JsNumber(fid.functionId),
        IdFields.timesCalledField -> JsNumber(fid.timesCalled),
        IdFields.idField -> JsNumber(fid.id),
        IdFields.processIdField -> JsNumber(fid.processId)
      )
    case frd: FunctionReturnId =>
      JsObject(
        IdFields.functionIdField -> JsNumber(frd.functionId),
        IdFields.processIdField -> JsNumber(frd.processId),
        IdFields.timesCalledField -> JsNumber(frd.timesCalled)
      )
  }

  private def writeIdentifier(optIdentifier: Option[String]): JsObject = optIdentifier match {
    case None => JsObject(Map[String, JsValue]())
    case Some(identifier) => JsObject(Map[String, JsValue](ExpFields.identifierField -> JsString(identifier)))
  }

  private def writeSymbolicInput(inp: SymbolicInput): JsObject = {
    def writeInput(id: SymbolicInputId, optIdentifier: Option[String], typeName: String): JsObject = {
      val fields = Map[String, JsValue](ExpFields.typeField -> JsString(ExpTypes.inputBoolField)) ++
        writeId(id).fields ++
        writeIdentifier(optIdentifier).fields
      JsObject(fields)
    }
    inp match {
      case SymbolicInputBool(id, optIdentifier) => writeInput(id, optIdentifier, ExpTypes.inputBoolField)
      case SymbolicInputFloat(id, optIdentifier) => writeInput(id, optIdentifier, ExpTypes.inputFloatField)
      case SymbolicInputInt(id, optIdentifier) => writeInput(id, optIdentifier, ExpTypes.inputIntType)
      case SymbolicInputString(id, optIdentifier) => writeInput(id, optIdentifier, ExpTypes.inputStringType)
    }
  }

  private def writeSymbolicMessageInput(min: SymbolicMessageInput): JsObject = {
    val typeValue: JsString = JsString(min match {
      case _: SymbolicMessageInputBool => TypeNames.boolTypeName
      case _: SymbolicMessageInputFloat => TypeNames.floatTypeName
      case _: SymbolicMessageInputInt => TypeNames.intTypeName
      case _: SymbolicMessageInputString => TypeNames.stringTypeName
    })
    JsObject(
      ExpFields.messageInputTypeField -> typeValue,
      ExpFields.messageTypeField -> JsString(min.messageType),
      ExpFields.idField -> JsNumber(min.id))
  }

  private def writeSymbolicBool(bol: SymbolicBool): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.boolType),
      ExpFields.sField -> JsBoolean(bol.b))
  }

  private def writeSymbolicFloat(flt: SymbolicFloat): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.floatType),
      ExpFields.fField -> JsNumber(flt.f))
  }

  private def writeSymbolicInt(int: SymbolicInt): JsObject = {
    JsObject(ExpFields.typeField -> JsString(ExpTypes.intType), ExpFields.iField -> JsNumber(int.i))
  }

  private def writeSymbolicString(str: SymbolicString): JsObject = {
    JsObject(
      ExpFields.typeField -> JsString(ExpTypes.stringType),
      ExpFields.sField -> JsString(str.s))
  }

  private def writeSymbolicITEExp(ite: SymbolicITEExpression[_, _]): JsObject = {
    JsObject(ExpFields.typeField -> JsString(ExpTypes.iteType))
  }
}
