package backend.expression

sealed abstract class SymbolicOperator(private val name: String) {
  override def toString: String = name
}
sealed trait RelationalOperator extends SymbolicOperator
sealed trait BinaryRelationalOperator extends RelationalOperator with BinaryOperator

case object StringToInt extends SymbolicOperator("string_to_int") // StringToInt not supported by Z3

/* Integer operations */
/* Integer arithmetical operations */
sealed abstract class IntegerArithmeticalOperator(val name: String) extends SymbolicOperator(name)

sealed trait BinaryOperator
sealed trait UnaryOperator

case object IntPlus extends IntegerArithmeticalOperator("+")
case object IntMinus extends IntegerArithmeticalOperator("-")
case object IntTimes extends IntegerArithmeticalOperator("*")
case object IntDiv extends IntegerArithmeticalOperator("/")
case object IntModulo extends IntegerArithmeticalOperator("%")

sealed abstract class ArithmeticalUnaryOperator(override val name: String)
  extends IntegerArithmeticalOperator(name)
    with UnaryOperator
case object IntInverse extends ArithmeticalUnaryOperator("-")

/* Boolean relational operations */
sealed abstract class BooleanRelationalOperator(val name: String)
  extends SymbolicOperator(name)
    with BinaryRelationalOperator
case object BooleanEqual extends BooleanRelationalOperator("=_b")

/* Integer relational operations */
sealed abstract class IntegerRelationalOperator(val name: String)
  extends SymbolicOperator(name)
    with BinaryRelationalOperator

case object IntLessThan extends IntegerRelationalOperator("<")
case object IntLessThanEqual extends IntegerRelationalOperator("<=")
case object IntGreaterThan extends IntegerRelationalOperator(">")
case object IntGreaterThanEqual extends IntegerRelationalOperator(">=")
case object IntEqual extends IntegerRelationalOperator("=")
case object IntNonEqual extends IntegerRelationalOperator("!=")

sealed abstract class LogicalUnaryOperator(val name: String)
  extends SymbolicOperator(name)
    with UnaryOperator
case object LogicalNot extends LogicalUnaryOperator("!")

sealed abstract class LogicalBinaryOperator(val name: String) extends SymbolicOperator(name)
case object LogicalAnd extends LogicalBinaryOperator("&")
case object LogicalOr extends LogicalBinaryOperator("|")

/* String operations */
sealed abstract class StringOperator(val name: String) extends SymbolicOperator(name)

case object IntToString extends StringOperator("int_to_string") // IntToString not supported by Z3
case object StringAt extends StringOperator("string_at")
case object StringAppend extends StringOperator("string_append")
case object StringIndexOf extends StringOperator("string_index_of")
case object StringLength extends StringOperator("string_length")
case object StringReplace extends StringOperator("string_replace")
case object StringGetSubstring extends StringOperator("string_get_substring")

/* String relational operations */
sealed abstract class StringRelationalOperator(override val name: String)
  extends StringOperator(name)
    with BinaryRelationalOperator

case object StringEqual extends StringRelationalOperator("=_s")
case object StringPrefixOf extends StringRelationalOperator("string_prefix")
case object StringIsSubstring extends StringRelationalOperator("string_is_substring")
case object StringSuffixOf extends StringRelationalOperator("string_suffix")

sealed abstract class EventOperator(val name: String)
  extends SymbolicOperator(name)
    with BinaryRelationalOperator {
  def negate: EventOperator
}
case object EventChosenOperator extends EventOperator("=e") {
  override def negate: EventOperator = EventNotChosenOperator
}
case object EventNotChosenOperator extends EventOperator("!=e") {
  override def negate: EventOperator = EventChosenOperator
}
