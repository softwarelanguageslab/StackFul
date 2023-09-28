package backend.expression

import backend.execution_state.ExecutionState

sealed trait SymbolicInputId
case class RegularId(id: Int, processId: Int) extends SymbolicInputId {
  override def toString: String = {
    s"pid:$processId, i:$id"
  }
}
case class FunctionId(functionId: Int, id: Int, processId: Int) extends SymbolicInputId {
  override def toString: String = {
    s"fid:$functionId, pid:$processId, i:$id"
  }
}
case class FunctionInputId(functionId: Int, processId: Int, id: Int, timesCalled: Int)
  extends SymbolicInputId {
  override def toString: String = {
    s"Input(fid:$functionId, pid:$processId, id:$id, tc:$timesCalled)"
  }
}
case class FunctionReturnId(functionId: Int, timesCalled: Int, processId: Int)
  extends SymbolicInputId {
  override def toString: String = {
    s"Return(fid:$functionId, pid:$processId, tc:$timesCalled)"
  }
}

case class ExecutionStateId(executionStateId: ExecutionState, processId: Int, id: Int)
  extends AnyRef
  with SymbolicInputId

sealed trait SymbolicMessageInput extends SymbolicExpression {
  this: ExpressionType =>
  def messageType: String
  def id: Int
  def equalsToOperator: BinaryRelationalOperator
}
case class SymbolicMessageInputBool(messageType: String, id: Int, identifier: Option[String] = None)
  extends SymbolicMessageInput
  with BooleanExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicMessageInputBool = {
    this.copy(identifier = newIdentifier)
  }
}
case class SymbolicMessageInputInt(messageType: String, id: Int, identifier: Option[String] = None)
  extends SymbolicMessageInput
  with FloatExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicMessageInputInt = {
    this.copy(identifier = newIdentifier)
  }
}
case class SymbolicMessageInputFloat(messageType: String, id: Int, identifier: Option[String] = None)
  extends SymbolicMessageInput
  with IntExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicMessageInputFloat = {
    this.copy(identifier = newIdentifier)
  }
}
case class SymbolicMessageInputString(messageType: String, id: Int, identifier: Option[String] = None)
  extends SymbolicMessageInput
  with StringExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicMessageInputString = {
    this.copy(identifier = newIdentifier)
  }
}

sealed abstract class SymbolicInput extends SymbolicExpression with ConvertibleToBooleanExpression {
  this: ExpressionType =>
  def id: SymbolicInputId
  def toBool: BooleanExpression =
    RelationalExpression(this, IntNonEqual, SymbolicInt(0), identifier)

  override def toString: String = {
    s"$inputTypeName(${id.toString})"
  }

  protected def inputTypeName: String = "input"

  def equalsToOperator: BinaryRelationalOperator
}

case class SymbolicInputBool(id: SymbolicInputId, identifier: Option[String] = None)
  extends SymbolicInput
    with BooleanExpression
    with BooleanExpressionType {
  def dropIdentifier: SymbolicInputBool = this.copy(identifier = None)
  override def toBool: BooleanExpression = this
  override def negate: BooleanExpression = LogicalUnaryExpression(LogicalNot, this)
  override protected def inputTypeName: String = "bool_input"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInputBool = {
    this.copy(identifier = newIdentifier)
  }
}
object SymbolicInputBool {
  def apply(id: SymbolicInputId, identifier: Option[String] = None): SymbolicInputBool = {
    SymExpFlyweight.makeSymInputBool(id, identifier)
  }
}

case class SymbolicInputFloat(id: SymbolicInputId, identifier: Option[String] = None)
  extends SymbolicInput
    with FloatExpressionType
    with ArithmeticalExpression {
  def dropIdentifier: SymbolicInputFloat = this.copy(identifier = None)
  override protected def inputTypeName: String = "float_input"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInputFloat = {
    this.copy(identifier = newIdentifier)
  }
}
object SymbolicInputFloat {
  def apply(id: SymbolicInputId, identifier: Option[String] = None): SymbolicInputFloat = {
    SymExpFlyweight.makeSymInputFloat(id, identifier)
  }
}

case class SymbolicInputInt(id: SymbolicInputId, identifier: Option[String] = None)
  extends SymbolicInput
    with IntExpressionType
    with ArithmeticalExpression {
  def dropIdentifier: SymbolicInputInt = this.copy(identifier = None)
  override protected def inputTypeName: String = "int_input"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInputInt = {
    this.copy(identifier = newIdentifier)
  }
}
object SymbolicInputInt {
  def apply(id: SymbolicInputId, identifier: Option[String] = None): SymbolicInputInt = {
    SymExpFlyweight.makeSymInputInt(id, identifier)
  }
}

case class SymbolicInputString(id: SymbolicInputId, identifier: Option[String] = None)
  extends SymbolicInput
    with StringExpression
    with StringExpressionType {
  override def toBool: BooleanExpression =
    LogicalUnaryExpression(
      LogicalNot,
      RelationalExpression(
        this,
        StringEqual,
        SymbolicString("")),
      identifier)
  def dropIdentifier: SymbolicInputString = this.copy(identifier = None)
  override protected def inputTypeName: String = "string_input"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInputString = {
    this.copy(identifier = newIdentifier)
  }
}
object SymbolicInputString {
  def apply(id: SymbolicInputId, identifier: Option[String] = None): SymbolicInputString = {
    SymExpFlyweight.makeSymInputString(id, identifier)
  }
}

case class SymbolicInputEvent(id: SymbolicInputId, identifier: Option[String] = None)
  extends SymbolicInput
    with IntExpressionType {
  override def equalsToOperator: BinaryRelationalOperator = IntEqual
  def eventId: Int = id match {
    case rid: RegularId => rid.id
    case fid: FunctionId => fid.id
  }
  def dropIdentifier: SymbolicInputEvent = this.copy(identifier = None)
  override protected def inputTypeName: String = "event_input"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInputEvent = {
    this.copy(identifier = newIdentifier)
  }

}
