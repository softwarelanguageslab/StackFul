package backend.expression

import backend.Path
import backend.execution_state.emptyStore
import backend.solvers.Z3Solver.Z3SatisfiabilityAsserter
import backend.tree.SymbolicNode
import backend.tree.constraints.ToBasic
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path.PathFollower
import backend.tree.path.SymJSState

object CheckExpressionAsserter {

  def assertExpressionEquals[T: CanDoEqualityCheck](
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    path: Path, identifier: String,
    expectedValue: T
  ): Unit = {
    assertExpressionEquals(pathFollower, root, path, identifier, expectedValue, Nil)
  }
  def assertExpressionEqualsViaMultiplePaths[T: CanDoEqualityCheck](
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    identifier: String,
    paths: Iterable[(Path, T)]
  ): Unit = {
    paths.foreach(tuple => assertExpressionEquals(pathFollower, root, tuple._1, identifier, tuple._2, Nil))
  }
  def assertExpressionEquals[T: CanDoEqualityCheck](
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    path: Path, identifier: String,
    expectedValue: T,
    processesInfo: List[Int]
  ): Unit = {
    val pathFollowed = pathFollower.followPathAndArriveAt(root, emptyStore, SymJSState(Nil, path))
    assert(pathFollowed.isDefined)
    val constraints = pathFollowed.get.path.getObserved

    val storeFound = pathFollowed.get.store
    assert(storeFound.map.contains(identifier))
    val expression = storeFound.map(identifier)
    val expectedExp = RelationalExpression(
      expression, implicitly[CanDoEqualityCheck[T]].operator,
      implicitly[CanDoEqualityCheck[T]].toSymValue(expectedValue))
    val expectedConstraint = BranchConstraint(expectedExp, Nil)
    val basicConstraints = implicitly[ToBasic[ConstraintES]].toBasicConstraints(constraints)
    Z3SatisfiabilityAsserter.assertSatisfiable(basicConstraints :+ expectedConstraint, processesInfo)
  }
  def assertExpressionEqualsViaMultiplePaths[T: CanDoEqualityCheck](
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    identifier: String,
    paths: Iterable[(Path, T)],
    processesInfo: List[Int]
  ): Unit = {
    paths.foreach(tuple => assertExpressionEquals(pathFollower, root, tuple._1, identifier, tuple._2, processesInfo))
  }

}
