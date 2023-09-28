package backend.expression

import backend.execution_state.ExecutionState

trait CanDoEqualityCheck[T] {
  def operator: BinaryRelationalOperator
  def toSymValue(value: T): SymbolicExpression
}

object Util {

  import scala.language.implicitConversions

  def input(id: Int, processId: Int): SymbolicInputInt = SymbolicInputInt(RegularId(id, processId), None)
  def input(id: Int, processId: Int, identifier: String): SymbolicInputInt = SymbolicInputInt(RegularId(id, processId), Some(identifier))
  def input(id: Int, identifier: String): SymbolicInputInt = input(id, 0, identifier)
  def input(id: Int): SymbolicInputInt = SymbolicInputInt(RegularId(id, 0), None)
  def input(es: ExecutionState, id: Int, processId: Int): SymbolicInputInt = SymbolicInputInt(ExecutionStateId(es, processId, id), None)

  def i(i: Int, identifier: String): SymbolicInt = SymbolicInt(i, Some(identifier))
  implicit def i(i: Int): SymbolicInt = SymbolicInt(i, None)

  def b(b: Boolean, identifier: String): SymbolicBool = SymbolicBool(b, Some(identifier))
  implicit def b(b: Boolean): SymbolicBool = SymbolicBool(b, None)

  def s(s: String, identifier: String): SymbolicString = SymbolicString(s, Some(identifier))
  implicit def s(s: String): SymbolicString = SymbolicString(s, None)


  implicit object BooleanCanDoEqualityCheck extends CanDoEqualityCheck[Boolean] {
    override def operator: BinaryRelationalOperator = BooleanEqual
    override def toSymValue(value: Boolean): SymbolicExpression = SymbolicBool(value)
  }
  implicit object IntCanDoEqualityCheck$ extends CanDoEqualityCheck[Int] {
    def operator: BinaryRelationalOperator = IntEqual
    def toSymValue(value: Int): SymbolicExpression = SymbolicInt(value)
  }
  implicit object StringCanDoEqualityCheck$ extends CanDoEqualityCheck[String] {
    def operator: BinaryRelationalOperator = StringEqual
    def toSymValue(value: String): SymbolicExpression = SymbolicString(value)
  }

}
