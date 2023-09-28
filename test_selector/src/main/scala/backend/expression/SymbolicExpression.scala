package backend.expression

import backend.Main

trait SymbolicExpression {
  def identifier: Option[String]
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicExpression
}

//case class SymbolicFunction(scopes: List[List[String]], serial: Int, identifier: Option[String]) extends SymbolicExpression {
case class SymbolicFunction(serial: Int, identifier: Option[String]) extends SymbolicExpression {
  override def toString: String = s"SymbolicFunction($serial)"
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicFunction = this.copy(identifier = newIdentifier)
}

trait ArithmeticalExpression extends SymbolicExpression
trait BooleanExpression extends SymbolicExpression with ConvertibleToBooleanExpression {
  def negate: BooleanExpression
  def toBool: BooleanExpression = this
}
trait StringExpression extends SymbolicExpression
trait EventExpression extends SymbolicExpression

trait ConvertibleToBooleanExpression {
  def toBool: BooleanExpression
}

case object SymbolicNothingExpression
  extends SymbolicExpression {
  override def toString: String = "nothing"
  def identifier: Option[String] = None
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicNothingExpression.type = SymbolicNothingExpression
}

case class SymbolicEventChosenExpression(
  eventInput: SymbolicInputEvent,
  op: EventOperator,
  tid: Int,
  identifier: Option[String] = None
)
  extends BooleanExpression {
  override def toString: String = s"($eventInput $op $tid)"

  override def negate: BooleanExpression = copy(op = op.negate)
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicEventChosenExpression = {
    this.copy(identifier = newIdentifier)
  }
}

case class StringOperationProducesStringExpression(
  op: StringOperator,
  exps: List[SymbolicExpression],
  identifier: Option[String] = None
) extends StringExpression
  with StringExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): StringOperationProducesStringExpression = {
    this.copy(identifier = newIdentifier)
  }
  override def toString: String = {
    s"($op ${exps.map(_.toString).mkString(" ")})"
  }

  def toBool: BooleanExpression =
    LogicalUnaryExpression(
      LogicalNot,
      RelationalExpression(
        this,
        StringEqual,
        SymbolicString("")))
}

case class StringOperationProducesIntExpression(
  op: StringOperator,
  exps: List[SymbolicExpression],
  identifier: Option[String] = None
) extends ArithmeticalExpression
  with IntExpressionType {
  def replaceIdentifier(newIdentifier: Option[String]): StringOperationProducesIntExpression = {
    this.copy(identifier = newIdentifier)
  }
  override def toString: String = {
    s"($op ${exps.map(_.toString).mkString(" ")})"
  }
}

case class ArithmeticalVariadicOperationExpression(
  op: IntegerArithmeticalOperator,
  exps: List[SymbolicExpression],
  identifier: Option[String] = None
) extends ArithmeticalExpression
  with ConvertibleToBooleanExpression {
  def replaceIdentifier(newIdentifier: Option[String]): ArithmeticalVariadicOperationExpression = {
    this.copy(identifier = newIdentifier)
  }
  override def toString: String = {
    if (Main.useDebug) {
      exps match {
        case Nil => s"( $op )"
        case List(arg) => s"( $op$arg )"
        case _ => exps.map("(" + _.toString + ")").mkString(s" $op ")
      }
    } else {
      "ArithVarExp(...)"
    }
  }

  def toBool: BooleanExpression =
    RelationalExpression(this, IntNonEqual, SymbolicInt(0), identifier)
}

trait HasOperator {
  type OperatorType
  def op: OperatorType
}
trait BinaryExpression extends HasOperator {
  type OperatorType = BinaryOperator
  def left: SymbolicExpression
  def right: SymbolicExpression
}
trait UnaryExpression {
  type OperatorType = UnaryOperator
  def arg: SymbolicExpression
}

case class ArithmeticalUnaryOperationExpression(
  op: ArithmeticalUnaryOperator,
  arg: SymbolicExpression,
  identifier: Option[String] = None
) extends ArithmeticalExpression
  with UnaryExpression
  with ConvertibleToBooleanExpression {
  def replaceIdentifier(newIdentifier: Option[String]): ArithmeticalUnaryOperationExpression = {
    this.copy(identifier = newIdentifier)
  }
  override def toString: String = {
    if (Main.useDebug) s"$op($arg)" else "ArithUnaryExp"
  }

  def toBool: BooleanExpression =
    RelationalExpression(this, IntNonEqual, SymbolicInt(0), identifier)
}

case class LogicalUnaryExpression(
  op: LogicalUnaryOperator,
  arg: BooleanExpression,
  identifier: Option[String] = None
) extends BooleanExpression
  with UnaryExpression {
  def replaceIdentifier(newIdentifier: Option[String]): LogicalUnaryExpression = this.copy(identifier = newIdentifier)
  override def toString: String = {
    s"$op $arg"
  }

  def negate: BooleanExpression = op match {
    case LogicalNot =>
      // ! (! a) == a
      arg
  }
}

case class LogicalBinaryExpression(
  left: BooleanExpression,
  op: LogicalBinaryOperator,
  right: BooleanExpression,
  identifier: Option[String] = None
) extends BooleanExpression {
  def replaceIdentifier(newIdentifier: Option[String]): LogicalBinaryExpression = this.copy(identifier = newIdentifier)
  override def toString: String = {
    if (Main.useDebug) {
      op match {
        case LogicalAnd => s"{$left $op $right}"
        case LogicalOr => s"[$left $op $right]"
        case _ => s"($left $op $right)"
      }
    } else {
      "LogicalBinaryExpression"
    }
  }

  def negate: BooleanExpression = op match {
    case LogicalAnd =>
      // ! (a && b) == (! a || ! b)
      LogicalBinaryExpression(
        LogicalUnaryExpression(LogicalNot, left),
        LogicalOr,
        LogicalUnaryExpression(LogicalNot, right),
        identifier)
    case LogicalOr =>
      // ! (a || b) == (! a && ! b)
      LogicalBinaryExpression(
        LogicalUnaryExpression(LogicalNot, left),
        LogicalAnd,
        LogicalUnaryExpression(LogicalNot, right),
        identifier)
  }
}

case class SymbolicIntVariable(variable: SymbolicInputInt)
  extends ArithmeticalExpression {
  def identifier: Option[String] = variable.identifier
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicIntVariable = {
    this.copy(variable = variable.replaceIdentifier(newIdentifier).asInstanceOf[SymbolicInputInt])
  }

  override def toString: String = s"(var:$variable)"
}

case class RelationalExpression(
  left: SymbolicExpression,
  op: BinaryRelationalOperator,
  right: SymbolicExpression,
  identifier: Option[String] = None
) extends BooleanExpression
  with BinaryExpression {
  def replaceIdentifier(newIdentifier: Option[String]): RelationalExpression = this.copy(identifier = newIdentifier)
  override def toString: String = {
    if (Main.useDebug) s"($left $op $right)" else "RelationalExp"
  }

  def negate: BooleanExpression = op match {
    case IntLessThan => RelationalExpression(left, IntGreaterThanEqual, right, identifier)
    case IntLessThanEqual => RelationalExpression(left, IntGreaterThan, right, identifier)
    case IntGreaterThan => RelationalExpression(left, IntLessThanEqual, right, identifier)
    case IntGreaterThanEqual => RelationalExpression(left, IntLessThan, right, identifier)
    case IntEqual => RelationalExpression(left, IntNonEqual, right, identifier)
    case IntNonEqual => RelationalExpression(left, IntEqual, right, identifier)
    case _ => LogicalUnaryExpression(LogicalNot, this, identifier)
  }
}

case class EventChosen(id: Int, processId: Int, targetId: Int)
  extends BooleanExpression {
  def negate: BooleanExpression = this

  def identifier: Option[String] = None
  def replaceIdentifier(newIdentifier: Option[String]): EventChosen = this
}

sealed trait ConcreteValue extends SymbolicExpression {
  type T
  def value: T
  override def toString: String = identifier match {
    case Some(identifier) => s"$value${identifier}"
    case None => value.toString
  }
}

case class SymbolicBool(b: Boolean, identifier: Option[String] = None)
  extends BooleanExpression
  with ConcreteValue {
  type T = Boolean
  def negate: BooleanExpression = SymbolicBool(!b, identifier)
  def value: Boolean = b
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicBool = this.copy(identifier = newIdentifier)
}

object SymbolicBool {
  def apply(b: Boolean, identifier: Option[String] = None): SymbolicBool = {
    SymExpFlyweight.makeSymBool(b, identifier)
  }
}

case class SymbolicInt(
  i: Int,
  identifier: Option[String] = None
) extends ArithmeticalExpression
  with ConvertibleToBooleanExpression
  with ConcreteValue {
  type T = Int
  def value: Int = i
  def toBool: SymbolicBool = SymbolicBool(i != 0, identifier)
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicInt = this.copy(identifier = newIdentifier)
}

object SymbolicInt {
  def apply(i: Int, identifier: Option[String] = None): SymbolicInt = {
    SymExpFlyweight.makeSymInt(i, identifier)
  }
}

case class SymbolicFloat(
  f: Float,
  identifier: Option[String] = None
) extends ArithmeticalExpression
  with ConvertibleToBooleanExpression
  with ConcreteValue {
  type T = Float
  def value: Float = f
  def toBool: SymbolicBool = SymbolicBool(f != 0, identifier)
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicFloat = this.copy(identifier = newIdentifier)
}

object SymbolicFloat {
  def apply(f: Float, identifier: Option[String]): SymbolicFloat = {
    SymExpFlyweight.makeSymFloat(f, identifier)
  }
}

case class SymbolicString(
  s: String,
  identifier: Option[String] = None
) extends StringExpression
  with ConvertibleToBooleanExpression
  with ConcreteValue {
  type T = String
  def value: String = s
  def toBool: SymbolicBool = SymbolicBool(s != "", identifier)
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicString = this.copy(identifier = newIdentifier)
}

object SymbolicString {
  def apply(s: String, identifier: Option[String] = None): SymbolicString = {
    SymExpFlyweight.makeSymString(s, identifier)
  }
}

case class SymbolicAddress(id: Int, identifier: Option[String] = None)
  extends SymbolicExpression {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicAddress = this.copy(identifier = newIdentifier)
  override def toString: String = s"@$id"
}

case class SymbolicObject(
  constructorName: String,
  fields: Map[String, SymbolicExpression],
  identifier: Option[String] = None
) extends SymbolicExpression {
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicObject = this.copy(identifier = newIdentifier)
  override def toString: String = constructorName
}

case class SymbolicITEExpression[T <: SymbolicExpression, U <: SymbolicExpression](
  predExp: BooleanExpression,
  thenExp: T,
  elseExp: U,
  identifier: Option[String] = None
) extends SymbolicExpression
  with BooleanExpression
  with ArithmeticalExpression
  with StringExpression {
  def negate: BooleanExpression = {
    // TODO only a valid operation if T and U are BooleanExpressions, because the ITE's negation would then be SymbolicITEExpression(predExp, thenExp.negate, elseExp.negate)
    assert(thenExp.isInstanceOf[BooleanExpression])
    assert(elseExp.isInstanceOf[BooleanExpression])
    SymbolicITEExpression(
      predExp, thenExp.asInstanceOf[BooleanExpression].negate, elseExp.asInstanceOf[BooleanExpression].negate)
  }
  def replaceIdentifier(newIdentifier: Option[String]): SymbolicITEExpression[T, U] = this.copy(identifier = newIdentifier)

  private val toStringDepthLimit = 2
  private def limitedToString(depth: Int): String = {
    if (depth >= toStringDepthLimit) {
      "ITE(...)"
    } else {
      val predString = predExp.toString
      val thenString = thenExp match {
        case thenITE: SymbolicITEExpression[_, _] => thenITE.limitedToString(depth + 1)
        case _ => thenExp.toString
      }
      val elseString = elseExp match {
        case elseITE: SymbolicITEExpression[_, _] => elseITE.limitedToString(depth + 1)
        case _ => elseExp.toString
      }
      val identifierString = if (identifier.nonEmpty) identifier.get else ""
      s"ITE($predString, $thenString, $elseString)_$identifierString"
    }
  }

  override def toString: String = limitedToString(0)
}


