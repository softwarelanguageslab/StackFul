package backend.tree.merging.use_cases

import backend._
import backend.TestConfigs.treeLogger
import backend.execution_state.SymbolicStore
import backend.execution_state.store._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.BranchSymbolicNode
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase6 extends MergingUseCasesTest {

  import Util._

  test("Merging use_case 6") {

    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)

    /**
      * var x;
      * if (i0 == 999) {
      *   if (i1 == 222) {
      *     x = 1;
      *   } else {
      *     x = 2;
      *   }
      * } else {
      *   x = 3;
      * }
      * if (x == 42) {} else {}
      */

    val x = Some("x")
    val enterX = StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(Set("x"), 0))
    val assigns_1 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(1, x))))
    val assigns_2 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(2, x))))
    val assigns_3 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> SymbolicInt(3, x))))

    val i0 = SymbolicInputInt(RegularId(0, 0))
    val c1_exp = RelationalExpression(i0, IntEqual, SymbolicInt(999))
    val c1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c1_exp, List(enterX.storeUpdate)), 0)
    val i1 = SymbolicInputInt(RegularId(1, 0))
    val c2_exp = RelationalExpression(i1, IntEqual, SymbolicInt(222))
    val c2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c2_exp, Nil), 1)
    val c3_1_exp = RelationalExpression(SymbolicInt(1, x), IntEqual, SymbolicInt(42))
    val c3_2_exp = RelationalExpression(SymbolicInt(2, x), IntEqual, SymbolicInt(42))
    val c3_3_exp = RelationalExpression(SymbolicInt(3, x), IntEqual, SymbolicInt(42))
    val c3_1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c3_1_exp, Nil), 2)
    val c3_2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c3_2_exp, Nil), 2)
    val c3_3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c3_3_exp, Nil), 2)

    val c1_T = ConstraintWithStoreUpdate[ConstraintES](c1, true)
    val c1_F = ConstraintWithStoreUpdate[ConstraintES](c1, false)
    val c2_T = ConstraintWithStoreUpdate[ConstraintES](c2, true)
    val c2_F = ConstraintWithStoreUpdate[ConstraintES](c2, false)
    val c3_1F = ConstraintWithStoreUpdate[ConstraintES](c3_1, false)
    val c3_2F = ConstraintWithStoreUpdate[ConstraintES](c3_2, false)
    val c3_3F = ConstraintWithStoreUpdate[ConstraintES](c3_3, false)

    val pc1: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T, c2_T, assigns_1, c3_1F)
    val pc2: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T, c2_F, assigns_2, c3_2F)
    val pc3: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_F, assigns_3, c3_3F)

    val store1: SymbolicStore = Map("x" -> SymbolicInt(1, x))
    val store2: SymbolicStore = Map("x" -> SymbolicInt(2, x))
    val store3: SymbolicStore = Map("x" -> SymbolicInt(3, x))

    reporter.addExploredPath(pc1, true)
    val treePathAdded1 = reporter.addExploredPath(pc2, true)

    /**
      * Before merging:
      * true
      * ├─ [T] 111 == 222
      * │  ├─ [T] 1 == 42
      * │  │  ├─ [T] Unexplored
      * │  │  ├─ [F] Leaf
      * │  ├─ [F] 2 == 42
      * │  │  ├─ [T] Unexplored
      * |  |  ├─ [F] Leaf
      * ├─ [F] Unexplored
      *
      * After merging
      * true
      * ├─ [T] 111 == 222
      * │  ├─ [T+F] ITE(111 == 222, 1, 2) == 42
      * │  │  ├─ [T] Unexplored
      * │  │  ├─ [F] Leaf
      * ├─ [F] Unexplored
      */


    var root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, 2)
    root = reporter.getRoot.get

    root match {
      case BranchSymbolicNode(condR, nodeT, nodeF) =>
        assert(condR == c1) // "true"
        assert(nodeT.storeUpdates == Nil)
        assert(nodeF.storeUpdates == Nil)
        assertUnexploredNode(nodeF)

        nodeT.to match {
          case BranchSymbolicNode(condT, nodeTT, nodeTF) =>
            val expectedITEValue = ite(c2_exp, SymbolicInt(1, x), SymbolicInt(2, x), x)
            assert(condT == c2) // "111 == 222"
            assert(nodeTT.to == nodeTF.to)
            assertStoreContainsViaMultiplePaths(
              pathFollower, root, Set("TT", "TE"), List(("x", exp => {
                checkITEValue(expectedITEValue, exp)
              })))
            nodeTT.to match {
              case BranchSymbolicNode(condTT, nodeTTT, nodeTTF) =>
                assertUnexploredNode(nodeTTT)
                assertLeafNode(nodeTTF)
                condTT match { // "ite(111 == 222, 1, 2) == 42"
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
    val treePathAdded2 = reporter.addExploredPath(pc3, true)

    /**
      * Before merging:
      * true
      * ├─ [T] 111 == 222
      * │  ├─ [T+F] ITE(111 == 222, 1, 2) == 42
      * │  │  ├─ [T] Unexplored
      * │  │  ├─ [F] Leaf
      * ├─ [F] 3 == 42
      * │  ├─ [T] Unexplored
      * │  ├─ [F] Leaf
      *
      * After merging:
      * true
      * ├─ [T] 111 == 222
      * │  ├─ [T+F][1] ITE(true, ITE(111 == 222, 1, 2), 3) == 42
      * │  │  ├─ [T] Unexplored
      * │  │  ├─ [F] Leaf
      * ├─ [F][1]
      */

    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, 2)
    root = reporter.getRoot.get
    root match {
      case BranchSymbolicNode(condR, nodeT, nodeF) =>
        val expectedITEValue = ite(c1_exp, ite(c2_exp, SymbolicInt(1, x), SymbolicInt(2, x), x), SymbolicInt(3, x), x)
        assert(condR == c1) // "true"
        assert(nodeT.storeUpdates == Nil)
        //        assertExpressionEqualsViaMultiplePaths(pathFollower, root, "x", List((List(ElseDirection), 3)))
        CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "E", "x", 3)
        CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TT", "x", 1)
        CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TE", "x", 2)

        //        assertStoreContains(pathFollower, root, List(ElseDirection), List(("x", exp => {
        //          checkITEExpression(List((i0, 998)), exp, 3)
        //          checkITEExpression(List((i0, 998), (i1, 222)), exp, 1)
        //          checkITEExpression(List((i0, 998), (i1, 221)), exp, 2)
        //        })))

        nodeT.to match {
          case BranchSymbolicNode(condT, nodeTT, nodeTF) =>
            assert(condT == c2) // "111 == 222"
            assert(nodeTT.to == nodeTF.to)
            assert(nodeTT.to == nodeF.to)
            assert(nodeTF.to == nodeF.to)
            //            assertStoreContainsViaMultiplePaths(pathFollower, root, Set("TT", "TE"), List(("x", exp => {
            //              checkITEValue(expectedITEValue, exp)
            //            })))
            nodeTT.to match {
              case BranchSymbolicNode(condTT, nodeTTT, nodeTTF) =>
                assertUnexploredNode(nodeTTT)
                assertLeafNode(nodeTTF)
                condTT match { // "ite(111 == 222, 1, 2) == 42"
                  case EventConstraintWithExecutionState(BranchConstraint(re: RelationalExpression, storeUpdates), _) =>
                    assert(re.identifier.isEmpty)
                    assert(re.op == IntEqual)
                    assert(re.right == SymbolicInt(42))
                    re.left match {
                      case ite: SymbolicITEExpression[SymbolicITEExpression[SymbolicInt, SymbolicInt], SymbolicInt] =>
                        println(ite)
                        CheckITEExpressionAsserter.checkITEExpression(List((i0, 998), (i1, 221)), ite, 3)
                        CheckITEExpressionAsserter.checkITEExpression(List((i0, 999), (i1, 222)), ite, 1)
                        CheckITEExpressionAsserter.checkITEExpression(List((i0, 999), (i1, 221)), ite, 2)
                      //                        assert(iteEquals(ite, expectedITEValue))
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

}
