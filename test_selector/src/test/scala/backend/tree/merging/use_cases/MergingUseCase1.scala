package backend.tree.merging.use_cases

import backend._
import backend.TestConfigs.treeLogger
import backend.execution_state.SymbolicStore
import backend.execution_state.store.{AssignmentsStoreUpdate, EnterScopeUpdate}
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.{BranchSymbolicNode, RegularLeafNode, UnexploredNode}
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase1 extends MergingUseCasesTest {

  test("Merging use_case 1") {
    /**
      * var x;
      * if (true) {
      *   x = 1;
      * } else {
      *   x = 2;
      * }
      * if (x == 42) {} else {}
      */

    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)

    val x = Some("x")
    val enterX = StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(Set("x"), 0))
    val assigns_1 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(1, x))))
    val assigns_2 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(2, x))))

    val c1: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(SymbolicBool(true), List(enterX.storeUpdate)), 0)
    val c2_1: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(SymbolicInt(1, x), IntEqual, SymbolicInt(42)), Nil), 1)
    val c2_2: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(SymbolicInt(2, x), IntEqual, SymbolicInt(42)), Nil), 1)

    val c1_T = ConstraintWithStoreUpdate[ConstraintES](c1, true)
    val c1_F = ConstraintWithStoreUpdate[ConstraintES](c1, false)
    val c2_1F = ConstraintWithStoreUpdate[ConstraintES](c2_1, false)
    val c2_2F = ConstraintWithStoreUpdate[ConstraintES](c2_2, false)

    val pc1: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T) :+ assigns_1 :+ c2_1F
    val pc2: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_F) :+ assigns_2 :+ c2_2F

    val store1: SymbolicStore = Map("x" -> SymbolicInt(1, x))
    val store2: SymbolicStore = Map("x" -> SymbolicInt(2, x))

    reporter.addExploredPath(pc1, true)
    val treePathAdded = reporter.addExploredPath(pc2, true)
    val root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded.init, 1)

    root match {
      case BranchSymbolicNode(cond1, nodeT, nodeF) =>
        val expectedITEValue = ite(SymbolicBool(true), SymbolicInt(1, x), SymbolicInt(2, x), x)
        assert(cond1 == c1)
        assert(nodeT.to == nodeF.to)
        assertStoreContainsViaMultiplePaths(
          pathFollower, root, Set("T", "E"), List(("x", exp => {
            checkITEValue(expectedITEValue, exp)
          })))
        nodeT.to match {
          case BranchSymbolicNode(condT, nodeTT, nodeTF) =>
            assertUnexploredNode(nodeTT)
            assertLeafNode(nodeTF)
            condT match {
              case EventConstraintWithExecutionState(BranchConstraint(re: RelationalExpression, storeUpdates), _) =>
                assert(re.identifier.isEmpty)
                assert(re.op == IntEqual)
                assert(re.right == SymbolicInt(42))
                re.left match {
                  case ite: SymbolicITEExpression[SymbolicInt, SymbolicInt] =>
                    assert(iteEquals(ite, expectedITEValue))
                }
                assert(storeUpdates.size == 1)
                storeUpdates.head match {
                  case AssignmentsStoreUpdate(assignments) =>
                    assert(assignments.size == 1)
                    val (string, value) = assignments.head
                    assert(string == "x")
                    assert(checkITEValue(expectedITEValue, value))
                }
            }
        }
    }
  }

}
