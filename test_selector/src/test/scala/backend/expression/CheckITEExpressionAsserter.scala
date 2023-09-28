package backend.expression

import backend.solvers.Z3Solver.Z3SatisfiabilityAsserter
import backend.tree.constraints.basic_constraints.BranchConstraint

object CheckITEExpressionAsserter {

  import backend.expression.Util._

  def checkITEExpression[T: CanDoEqualityCheck](
    input: SymbolicInputInt,
    valueForInput: Int,
    iteExp: SymbolicExpression,
    expectedValue: T
  ): Unit = {
    checkITEExpression(List((input, valueForInput)), iteExp, expectedValue)
  }
  def checkITEExpression[T: CanDoEqualityCheck](
    inputsWithValues: List[(SymbolicInput, Int)], iteExp: SymbolicExpression,
    expectedValue: T
  ): Unit = {
    checkITEExpression(inputsWithValues, iteExp, expectedValue, Nil)
  }
  def checkITEExpression[T: CanDoEqualityCheck](
    inputsWithValues: List[(SymbolicInput, Int)], iteExp: SymbolicExpression,
    expectedValue: T, processesInfo: List[Int]
  ): Unit = {
    val equateInputExps = inputsWithValues.map({
      case (intInput: SymbolicInputInt, value) => RelationalExpression(intInput, IntEqual, value)
      case (eventInput: SymbolicInputEvent, value) => SymbolicEventChosenExpression(
        eventInput, EventChosenOperator, value)
    })
    val equateInputConstraints = equateInputExps.map(BranchConstraint(_, Nil))
    val expectedExp = implicitly[CanDoEqualityCheck[T]].toSymValue(expectedValue)
    val iteExpCompared = RelationalExpression(iteExp, implicitly[CanDoEqualityCheck[T]].operator, expectedExp)
    val iteComparedConstraint = BranchConstraint(iteExpCompared, Nil)
    Z3SatisfiabilityAsserter.assertSatisfiable(equateInputConstraints :+ iteComparedConstraint, processesInfo)
  }
}
