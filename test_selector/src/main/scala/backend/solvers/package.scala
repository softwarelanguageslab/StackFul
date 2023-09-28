package backend

import backend.expression.SymbolicExpression

package object solvers {

  case class SolveError(originalException: Throwable) extends Throwable
  case class UnexpectedExpressionType(exp: SymbolicExpression, expectedType: String) extends Throwable {
    override def toString: String = s"Expression $exp was expected to have type $expectedType"
  }

}
