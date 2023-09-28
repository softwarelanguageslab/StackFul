package backend.tree.merging

import backend.Path
import backend.execution_state._
import backend.expression.Util._
import backend.expression._
import backend.solvers.Z3Solver.Z3SatisfiabilityAsserter
import backend.tree.SymbolicNode
import backend.tree.constraints.ToBooleanExpression
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path._
import backend.tree.merging.use_cases.automatically_generated.Store
import backend.tree.path.SymJSState

trait RandomlyGeneratedTests {

  protected val pathsToTraverse = 50
  protected val nrOfTreesToCreate = 20

  protected val dummyExecutionState: CodeLocationExecutionState = CodeLocationExecutionState.dummyExecutionState

  def printSeed(seed: Int): Unit = {
    println(Console.RED + s"======= Creating tree with seed $seed =======" + Console.RESET)
  }

  protected def printIteration(iteration: Int): Unit = {
    println(Console.BLUE + s"Running test iteration $iteration" + Console.RESET)
  }

  protected def checkValueOfIdentifier(
    pathFollower: PathFollower[ConstraintES],
    path: Path,
    value: SymbolicInt,
    resultVariable: SymbolicInputInt,
    root: SymbolicNode[ConstraintES],
    processesInfo: List[Int]
  ): Unit = {
    val pastMergedConstraint = pathFollower.followPathAndArriveAt(root, emptyStore, SymJSState(Nil, path :+ ThenDirection))
    assert(pastMergedConstraint.isDefined)
    val constraints = pastMergedConstraint.get.path.getObserved
    val variableCompared = BranchConstraint(RelationalExpression(resultVariable, IntEqual, value), Nil)
    val allConstraints: List[ConstraintES] = constraints :+ variableCompared
    val booleanExp = implicitly[ToBooleanExpression[ConstraintES]](ConstraintESAllInOne.toBooleanExpression).toBoolExp(allConstraints)
    val asBasicConstraints = ConstraintESAllInOne.toBasicConstraints.toBasicConstraints(allConstraints)
    Z3SatisfiabilityAsserter.assertSatisfiable(asBasicConstraints, processesInfo)
  }

  protected def checkValueOfIdentifier(
    pathFollower: PathFollower[ConstraintES],
    path: Path,
    value: SymbolicInt,
    resultVariable: SymbolicInputInt,
    root: SymbolicNode[ConstraintES]
  ): Unit = {
    checkValueOfIdentifier(pathFollower, path, value, resultVariable, root, Nil)
  }

  protected def createComparisonExp(
    store: Store[Int],
    identifier: String,
    inputId: Int
  ): (SymbolicInt, RelationalExpression) = {
    val tuple = store(identifier)
    val expectedValue = tuple._1
    val comparison = RelationalExpression(expectedValue.replaceIdentifier(Some(identifier)), IntEqual, input(inputId))
    (tuple._2, comparison)
  }

}
