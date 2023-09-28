package backend.tree.merging.use_cases

import backend.ConstraintWithStoreUpdate
import backend.execution_state.SymbolicStore
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._

class MergingUseCase2 extends MergingUseCasesTest {

  import Util._

  test("Merging use_case 2") {
    val x = Some("x")
    val reporter = new ConstraintESReporter(MergingMode, None)
    val store1: SymbolicStore = Map[String, SymbolicExpression](x.get -> ite(true, SymbolicInt(1), SymbolicInt(2)))
    val c1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(SymbolicBool(true), Nil), 0)
    reporter.addExploredPath(List(ConstraintWithStoreUpdate(c1, true)), true)
    assert(reporter.getRoot.isDefined)
    reporter.getRoot.get match {
      case BranchSymbolicNode(_, thenBranch, elseBranch) =>
        assertLeafNode(thenBranch)
        assertUnexploredNode(elseBranch)
      case _ => assert(false)
    }
    val exp2 = RelationalExpression(SymbolicInt(10, x), IntLessThan, 20)
    val c2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(exp2, Nil), 1)
    reporter.addExploredPath(List(ConstraintWithStoreUpdate(c1, false), ConstraintWithStoreUpdate(c2, true)), true)
    reporter.getRoot.get match {
      case BranchSymbolicNode(_, thenBranch, elseBranch) =>
        assertLeafNode(thenBranch)
        elseBranch.to match {
          case BranchSymbolicNode(constraint, thenBranch, elseBranch) =>
            assertLeafNode(thenBranch)
            assertUnexploredNode(elseBranch)
            constraint.asInstanceOf[EventConstraintWithExecutionState].ec.asInstanceOf[BranchConstraint].exp match {
              case RelationalExpression(_: SymbolicInt, _, _, _) => assert(true)
              case _ => assert(false)
            }
          case _ => assert(false)
        }
      case _ => assert(false)
    }
  }

}
