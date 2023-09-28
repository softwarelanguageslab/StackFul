package backend.tree.merging.use_cases

import backend._
import backend.TestConfigs.treeLogger
import backend.execution_state.SymbolicStore
import backend.execution_state.store._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.constraints.HasStoreUpdates
import backend.tree.BranchSymbolicNode
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path._
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase7 extends MergingUseCasesTest {

  import Util._

  /**
    * <img src="doc/tests/symbolic_execution_trees/merging_use_case_7.pdf"/>
    */
  test("Merging use_case 7") {
    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    val pathFollower = new ConstraintESPathFollower[Unit]

    /**
      * var x;
      * if (i0 == 222) {
      *   if (i1 == 2) {
      *     x = 1
      *   } else {  i1 != 2
      *     x = 2
      *   }
      * } else { i0 != 222
      *   x = 3
      * }
      * if (x == 42) {} else {}
      */


    val x = Some("x")
    val enterX = StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(Set("x"), 0))
    val assigns_0 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(0))))
    val assigns_1 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(1))))
    val assigns_2 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(2))))
    val assigns_3 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(3))))
    val assigns_4 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(4))))

    val i0 = SymbolicInputInt(RegularId(0, 0))
    val i1 = SymbolicInputInt(RegularId(1, 0))
    val c2_exp = RelationalExpression(i0, IntEqual, SymbolicInt(222))
    val c2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c2_exp, Nil), 1)

    val c3_exp = RelationalExpression(i1, IntEqual, SymbolicInt(2))
    val c3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c3_exp, Nil), 2)

    val c4_1_exp = RelationalExpression(SymbolicInt(1, x), IntEqual, SymbolicInt(42))
    val c4_2_exp = RelationalExpression(SymbolicInt(2, x), IntEqual, SymbolicInt(42))
    val c4_3_exp = RelationalExpression(SymbolicInt(3, x), IntEqual, SymbolicInt(42))
    val c4_4_exp = RelationalExpression(SymbolicInt(4, x), IntEqual, SymbolicInt(42))
    val c4_1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_1_exp, Nil), 3)
    val c4_2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_2_exp, Nil), 3)
    val c4_3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_3_exp, Nil), 3)
    val c4_4: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_4_exp, Nil), 3)



    // Removing store annotations alongside the edges would make it easier to merge a previously-merged subtree
    // into a larger tree. However, this has the disadvantage that *all* updates would have to be removed in the
    // entire subtree. What happens to those edges in the paths that don't lead back to a common node?
    // Is this even possible?

    // On the other hand, leaving the edges as is without doing anything to them might lead to
    // inconsistent store updates: a store is formed by following a first set of (outdated)
    // store updates, which are later overridden by the newly created store updates inside the common node.
    // These two updates basically represent the same store changes, but only the ones inside
    // the common node should be used.
    // Can't try to remove the store updates alongside the taken path either, because there might be other
    // paths as well which might not have been followed but are here nonetheless.

    // first step: create a unit test where two merges take place. One path has an intermediate node right before
    // the common node. This intermediate node is never

    val c2_T = ConstraintWithStoreUpdate[ConstraintES](c2, true)
    val c2_F = ConstraintWithStoreUpdate[ConstraintES](c2, false)
    val c3_T = ConstraintWithStoreUpdate[ConstraintES](c3, true)
    val c3_F = ConstraintWithStoreUpdate[ConstraintES](c3, false)
    val c4_1F = ConstraintWithStoreUpdate[ConstraintES](c4_1, false)
    val c4_2F = ConstraintWithStoreUpdate[ConstraintES](c4_2, false)
    val c4_3F = ConstraintWithStoreUpdate[ConstraintES](c4_3, false)
    val c4_4F = ConstraintWithStoreUpdate[ConstraintES](c4_4, false)

    val pc1: PathConstraintWithStoreUpdates[ConstraintES] = List(assigns_0, c2_T, c3_T, assigns_1, c4_1F)
    val pc2: PathConstraintWithStoreUpdates[ConstraintES] = List(assigns_0, c2_T, c3_F, assigns_2, c4_2F)
    val pc3: PathConstraintWithStoreUpdates[ConstraintES] = List(assigns_0, c2_F, assigns_3, c4_3F)
    val pc4: PathConstraintWithStoreUpdates[ConstraintES] = List(assigns_4, c4_4F)

    val store1: SymbolicStore = Map("x" -> SymbolicInt(1))
    val store2: SymbolicStore = Map("x" -> SymbolicInt(2))
    val store3: SymbolicStore = Map("x" -> SymbolicInt(3))
    val store4: SymbolicStore = Map("x" -> SymbolicInt(4))

    reporter.addExploredPath(pc1, true)
    var root = reporter.getRoot.get
    treeLogger.e(root)
    val treePathAdded1 = reporter.addExploredPath(pc3, true)

    root = reporter.getRoot.get
    treeLogger.e(root)
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, 3)
    root = reporter.getRoot.get

    val expectedITEValue1 = ite(
      RelationalExpression(i0, IntEqual, SymbolicInt(222)), SymbolicInt(1), SymbolicInt(3), x)
    root match {
      case BranchSymbolicNode(condT, nodeTT, nodeTF) =>
        assert(
          condT == implicitly[HasStoreUpdates[ConstraintES]].addStoreUpdatesBefore(c2, List(assigns_0.storeUpdate)))
        assert(nodeTT.to != nodeTF.to)
        nodeTT.to match {
          case BranchSymbolicNode(condTT, nodeTTT, nodeTTF) =>
            assertUnexploredNode(nodeTTF)
            assert(nodeTTT.to == nodeTF.to)
            assert(condTT == c3)
            nodeTTT.to match {
              case BranchSymbolicNode(condTTT, nodeTTTT, nodeTTTF) =>
                assertUnexploredNode(nodeTTTT)
                assertLeafNode(nodeTTTF)
                condTTT match {
                  case EventConstraintWithExecutionState(BranchConstraint(re: RelationalExpression, _), _) =>
                    assert(re.identifier.isEmpty)
                    assert(re.right == SymbolicInt(42))
                    assert(checkITEValue(expectedITEValue1, re.left), s"Actual value of re.left: ${re.left}")
                }
            }
        }

        CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TT", "x", 1)
        CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "E", "x", 3)
    }

    val treePathAdded2 = reporter.addExploredPath(pc2, true)
    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, 3)
    root = reporter.getRoot.get

    root match {
      case BranchSymbolicNode(condT, nodeTT, nodeTF) =>
        assert(
          condT == implicitly[HasStoreUpdates[ConstraintES]].addStoreUpdatesBefore(c2, List(assigns_0.storeUpdate)))
        assert(nodeTT.to != nodeTF.to)
        nodeTT.to match {
          case BranchSymbolicNode(condTT, nodeTTT, nodeTTF) =>
            assert(nodeTTT.to == nodeTTF.to)
            assert(nodeTTT.to == nodeTF.to)
            assert(condTT == c3)
            nodeTTT.to match {
              case BranchSymbolicNode(condTTT, nodeTTTT, nodeTTTF) =>
                assertUnexploredNode(nodeTTTT)
                assertLeafNode(nodeTTTF)
                condTTT match {
                  case EventConstraintWithExecutionState(BranchConstraint(re: RelationalExpression, _), _) =>
                    assert(re.identifier.isEmpty)
                    assert(re.right == SymbolicInt(42))
                    CheckITEExpressionAsserter.checkITEExpression(i0, 223, re.left, 3)
                    CheckITEExpressionAsserter.checkITEExpression(List((i0, 222), (i1, 2)), re.left, 1)
                    CheckITEExpressionAsserter.checkITEExpression(List((i0, 222), (i1, 10)), re.left, 2)
                }
            }
        }
    }

    val pathsAndValues = Set[(Path, Int)](("TT", 1), ("TE", 2), ("E", 3))
    CheckExpressionAsserter.assertExpressionEqualsViaMultiplePaths(pathFollower, root, "x", pathsAndValues)
  }

}
