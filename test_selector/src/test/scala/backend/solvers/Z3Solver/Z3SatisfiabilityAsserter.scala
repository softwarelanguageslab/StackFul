package backend.solvers.Z3Solver

import backend.expression.{BooleanExpression, SymbolicInput}
import backend.solvers.ComputedValue
import backend.tree.constraints.BasicConstraint
import backend.tree.constraints.basic_constraints.BranchConstraint

object Z3SatisfiabilityAsserter {

  def assertSatisfiable(exp: BooleanExpression): Map[SymbolicInput, ComputedValue] = {
    val bc = BranchConstraint(exp, Nil)
    assertSatisfiable(List(bc))
  }
  def assertSatisfiable(constraints: List[BasicConstraint]): Map[SymbolicInput, ComputedValue] = {
    assertSatisfiable(constraints, Nil)
  }
  def assertSatisfiable(
    constraints: List[BasicConstraint],
    processesInfo: List[Int]
  ): Map[SymbolicInput, ComputedValue] = {
    Z3.solve(constraints, processesInfo) match {
      case Satisfiable(solution) => solution
      case Unsatisfiable =>
        assert(false, "Expected a solution, constraints were found to be unsatisfiable")
        throw new AssertionError()
      case exception: SomeZ3Error =>
        exception.ex.printStackTrace()
        assert(false, "Expected a solution, Z3 threw an error")
        throw exception.ex
    }
  }
  def assertUnsatisfiable(exp: BooleanExpression): Unit = {
    val bc = BranchConstraint(exp, Nil)
    assertUnsatisfiable(List(bc))
  }
  def assertUnsatisfiable(constraints: List[BasicConstraint]): Unit = {
    assertUnsatisfiable(constraints, Nil)
  }
  def assertUnsatisfiable(constraints: List[BasicConstraint], processesInfo: List[Int]): Unit = {
    Z3.solve(constraints, processesInfo) match {
      case Satisfiable(solution) =>
        assert(false, s"Expected constraints to be unsatisfiable, but found a solution: $solution")
        throw new AssertionError()
      case Unsatisfiable =>
        assert(true)
      case exception: SomeZ3Error =>
        exception.ex.printStackTrace()
        assert(false, "Expected a solution, Z3 threw an error")
        throw exception.ex
    }
  }

}
