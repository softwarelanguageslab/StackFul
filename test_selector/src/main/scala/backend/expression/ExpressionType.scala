package backend.expression

sealed trait ExpressionType {
  def equalsToOperator: BinaryRelationalOperator
}
trait BooleanExpressionType extends ExpressionType {
  def equalsToOperator: BinaryRelationalOperator = BooleanEqual
}
trait FloatExpressionType extends ExpressionType {
  def equalsToOperator: BinaryRelationalOperator = IntEqual
}
trait IntExpressionType extends ExpressionType {
  def equalsToOperator: BinaryRelationalOperator = IntEqual
}
trait StringExpressionType extends ExpressionType {
  def equalsToOperator: BinaryRelationalOperator = StringEqual
}
