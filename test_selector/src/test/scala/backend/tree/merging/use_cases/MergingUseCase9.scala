package backend.tree.merging.use_cases

import backend._
import backend.TestConfigs.treeLogger
import backend.execution_state.{ExecutionState, SymbolicStore}
import backend.execution_state.store._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path.ConstraintESPathFollower
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase9 extends MergingUseCasesTest {

  import backend.expression.Util._

  /**
    * <img src="doc/tests/symbolic_execution_trees/merging_use_case_9.pdf"/>
    */
  test("Merging use_case 9") {
    /**
      * var x;
      * if (i0 == 0) {
      *   if (i1 == 10) {
      *     if (i2 == 100) {
      *       x = 1;
      *     else {
      *       x = 2;
      *     }
      *   } else {
      *     x = 3;
      *   }
      * } else {
      *   x = 4;
      * }
      * if (x == 42) {...} else {...}
      */

    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    val pathFollower = new ConstraintESPathFollower[Unit]

    val toMerge: ExecutionState = 3

    val i0 = input(0)
    val i1 = input(1)
    val i2 = input(2)

    val enterX = StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(Set("x"), 0))
    val assigns_1 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> i(1, "x"))))
    val assigns_2 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> i(2, "x"))))
    val assigns_3 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> i(3, "x"))))
    val assigns_4 = StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(List("x" -> i(4, "x"))))

    val c1_exp = RelationalExpression(i0, IntEqual, 0)
    val c1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c1_exp, List(enterX.storeUpdate)), 0)

    val c2_exp = RelationalExpression(i1, IntEqual, 10)
    val c2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c2_exp, Nil), 1)

    val c3_exp = RelationalExpression(i2, IntEqual, 100)
    val c3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c3_exp, Nil), 2)

    val c4_1_exp = RelationalExpression(i(1, "x"), IntEqual, 42)
    val c4_2_exp = RelationalExpression(i(2, "x"), IntEqual, 42)
    val c4_3_exp = RelationalExpression(i(3, "x"), IntEqual, 42)
    val c4_4_exp = RelationalExpression(i(4, "x'"), IntEqual, 42)
    val c4_1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_1_exp, Nil), toMerge)
    val c4_2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_2_exp, Nil), toMerge)
    val c4_3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_3_exp, Nil), toMerge)
    val c4_4: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(c4_4_exp, Nil), toMerge)

    val c1_T = ConstraintWithStoreUpdate[ConstraintES](c1, true)
    val c1_F = ConstraintWithStoreUpdate[ConstraintES](c1, false)
    val c2_T = ConstraintWithStoreUpdate[ConstraintES](c2, true)
    val c2_F = ConstraintWithStoreUpdate[ConstraintES](c2, false)
    val c3_T = ConstraintWithStoreUpdate[ConstraintES](c3, true)
    val c3_F = ConstraintWithStoreUpdate[ConstraintES](c3, false)
    val c4_1F = ConstraintWithStoreUpdate[ConstraintES](c4_1, false)
    val c4_2F = ConstraintWithStoreUpdate[ConstraintES](c4_2, false)
    val c4_3F = ConstraintWithStoreUpdate[ConstraintES](c4_3, false)
    val c4_4F = ConstraintWithStoreUpdate[ConstraintES](c4_4, false)

    val pc1: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T, c2_T, c3_T, assigns_1, c4_1F)
    val pc2: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T, c2_T, c3_F, assigns_2, c4_2F)
    val pc3: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_T, c2_F, assigns_3, c4_3F)
    val pc4: PathConstraintWithStoreUpdates[ConstraintES] = List(c1_F, assigns_4, c4_4F)

    val store1: SymbolicStore = Map("x" -> i(1, "x"))
    val store2: SymbolicStore = Map("x" -> i(2, "x"))
    val store3: SymbolicStore = Map("x" -> i(3, "x"))
    val store4: SymbolicStore = Map("x" -> i(4, "x"))

    reporter.addExploredPath(pc1, true)
    val treePathAdded1 = reporter.addExploredPath(pc2, true)

    var root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, toMerge)
    root = reporter.getRoot.get

    root match {
      case BranchSymbolicNode(const1, t, f) =>
        assertUnexploredNode(f)
        assert(const1 == c1)
        t.to match {
          case BranchSymbolicNode(const2, tt, tf) =>
            assertUnexploredNode(tf)
            assert(const2 == c2)
            tt.to match {
              case BranchSymbolicNode(const3, ttt, ttf) =>
                assert(const3 == c3)
                assert(ttt.to == ttf.to)
                ttt.to match {
                  case BranchSymbolicNode(const4, tttt, tttf) =>
                    assertUnexploredNode(tttt)
                    assertLeafNode(tttf)
                    const4 match {
                      case EventConstraintWithExecutionState(ec, state) =>
                        assert(state == (toMerge: ExecutionState))
                        ec match {
                          case BranchConstraint(bConst4, bConst4Updates) =>
                            assert(bConst4Updates.nonEmpty)
                            bConst4Updates.head match {
                              case AssignmentsStoreUpdate(assignments) =>
                                assert(assignments.exists(_._1 == "x"))
                                val ite = assignments.find(_._1 == "x").get._2
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                              case other => assert(false, s"Found a $other")
                            }
                            bConst4 match {
                              case RelationalExpression(ite, IntEqual, SymbolicInt(42, None), None) =>
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                              case other => assert(false, s"Found a $other")
                            }
                          case other => assert(false, s"Found a $other")
                        }
                      case other => assert(false, s"Found a $other")
                    }
                  case other => assert(false, s"Found a $other")
                }
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
      case other => assert(false, s"Found a $other")
    }

    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTT", "x", 1)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTE", "x", 2)

    val treePathAdded2 = reporter.addExploredPath(pc3, true)
    root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, toMerge)
    root = reporter.getRoot.get

    root match {
      case BranchSymbolicNode(const1, t, f) =>
        assertUnexploredNode(f)
        assert(const1 == c1)
        t.to match {
          case BranchSymbolicNode(const2, tt, tf) =>
            assert(const2 == c2)
            tt.to match {
              case BranchSymbolicNode(const3, ttt, ttf) =>
                assert(const3 == c3)
                assert(ttt.to == ttf.to)
                assert(ttt.to == tf.to)
                assert(ttf.to == tf.to)
                ttt.to match {
                  case BranchSymbolicNode(const4, tttt, tttf) =>
                    assertUnexploredNode(tttt)
                    assertLeafNode(tttf)
                    const4 match {
                      case EventConstraintWithExecutionState(ec, state) =>
                        assert(state == (toMerge: ExecutionState))
                        ec match {
                          case BranchConstraint(bConst4, bConst4Updates) =>
                            assert(bConst4Updates.nonEmpty)
                            bConst4Updates.head match {
                              case AssignmentsStoreUpdate(assignments) =>
                                assert(assignments.exists(_._1 == "x"))
                                val ite = assignments.find(_._1 == "x").get._2
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite, 3)
                              case other => assert(false, s"Found a $other")
                            }
                            bConst4 match {
                              case RelationalExpression(ite, IntEqual, SymbolicInt(42, None), None) =>
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite, 3)
                              case other => assert(false, s"Found a $other")
                            }
                          case other => assert(false, s"Found a $other")
                        }
                      case other => assert(false, s"Found a $other")
                    }
                  case other => assert(false, s"Found a $other")
                }
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
      case other => assert(false, s"Found a $other")
    }

    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTT", "x", 1)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTE", "x", 2)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TE", "x", 3)

    val treePathAdded3 = reporter.addExploredPath(pc4, true)
    root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded3.init, toMerge)
    root = reporter.getRoot.get

    root match {
      case BranchSymbolicNode(const1, t, f) =>
        assert(const1 == c1)
        t.to match {
          case BranchSymbolicNode(const2, tt, tf) =>
            assert(const2 == c2)
            tt.to match {
              case BranchSymbolicNode(const3, ttt, ttf) =>
                assert(const3 == c3)
                assert(ttt.to == ttf.to)
                assert(ttt.to == tf.to)
                assert(ttf.to == tf.to)
                assert(ttt.to == f.to)
                assert(ttf.to == f.to)
                assert(tf.to == f.to)
                ttt.to match {
                  case BranchSymbolicNode(const4, tttt, tttf) =>
                    assertUnexploredNode(tttt)
                    assertLeafNode(tttf)
                    const4 match {
                      case EventConstraintWithExecutionState(ec, state) =>
                        assert(state == (toMerge: ExecutionState))
                        ec match {
                          case BranchConstraint(bConst4, bConst4Updates) =>
                            assert(bConst4Updates.nonEmpty)
                            bConst4Updates.head match {
                              case AssignmentsStoreUpdate(assignments) =>
                                assert(assignments.exists(_._1 == "x"))
                                val ite = assignments.find(_._1 == "x").get._2
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite, 3)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite, 4)
                              case other => assert(false, s"Found a $other")
                            }
                            bConst4 match {
                              case RelationalExpression(ite, IntEqual, SymbolicInt(42, None), None) =>
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 100)), ite, 1)
                                CheckITEExpressionAsserter.checkITEExpression(
                                  List((i0, 0), (i1, 10), (i2, 101)), ite, 2)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite, 3)
                                CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite, 4)
                              case other => assert(false, s"Found a $other")
                            }
                          case other => assert(false, s"Found a $other")
                        }
                      case other => assert(false, s"Found a $other")
                    }
                  case other => assert(false, s"Found a $other")
                }
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
      case other => assert(false, s"Found a $other")
    }

    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTT", "x", 1)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TTE", "x", 2)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TE", "x", 3)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "E", "x", 4)

  }
}
