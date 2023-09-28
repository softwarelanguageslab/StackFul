package backend.tree.merging.use_cases

import backend._
import backend.TestConfigs.treeLogger
import backend.execution_state.{SymbolicStore, TargetEndExecutionState}
import backend.execution_state.store._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints._
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase8 extends MergingUseCasesTest {

  ignore("Merging scoping 8") {

    val processesInfo = ProcessesInfo(List(0, 2), true)
    def makeTargetChosen(id: Int, tid: Int): EventConstraintWithExecutionState = {
      EventConstraintWithExecutionState(
        TargetChosen(id, 1, tid, Map(0 -> Set(), 1 -> 0.until(tid).toSet), processesInfo, startingProcess, Nil), 0)
    }

    /**
      * var z = 0;
      *
      * function f(x) {
      *   var y = x;
      *   function g() {
      *     z += 1;
      *     y += z;
      *     return 123 + y;
      *   }
      *   return g;
      * }
      *
      * var b0_f = f(10);
      * var b1_f = f(20);
      *
      * function b0() {
      *   if (Math.random() === b0_f()) {
      *     "DEBUGGING B0 THEN";
      *   } else {
      *     "DEBUGGING B0 ELSE";
      *   }
      *   "EVENT_HANDLER_END";
      * }
      *
      * function b1() {
      *   if (Math.random() === b1_f()) {
      *     "DEBUGGING B1 THEN";
      *   } else {
      *     "DEBUGGING B1 ELSE";
      *   }
      *   "EVENT_HANDLER_END";
      * }
      */

    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)

    val globalScopeId = 0
    val fScopeId = 1
    val gScopeId = 2


    val y = Some("y")
    val z = Some("z")

    val enterGlobal = EnterScopeUpdate(Set("z"), globalScopeId)
    val exitGlobal = ExitScopeUpdate(Set("z"), globalScopeId)
    val enterF = EnterScopeUpdate(Set("y"), fScopeId)
    val exitF = ExitScopeUpdate(Set("y"), fScopeId)
    val enterG = EnterScopeUpdate(Set("g"), gScopeId)
    val exitG = ExitScopeUpdate(Set("g"), gScopeId)

    def createBCondition(
      b0Times: Int,
      b1Times: Int,
      baseYValue: Int
    ): (BooleanExpression, List[StoreUpdatePCElement[ConstraintES]]) = {
      def createY(i: Int, zValues: Seq[ArithmeticalExpression]): ArithmeticalExpression = {
        if (i == 0) {
          SymbolicInt(baseYValue, y)
        } else {
          ArithmeticalVariadicOperationExpression(IntPlus, List(createY(i - 1, zValues.init), zValues.last), y)
        }
      }
      def createZ(i: Int): ArithmeticalExpression = {
        if (i == 0) {
          SymbolicInt(0, z)
        } else {
          ArithmeticalVariadicOperationExpression(IntPlus, List(createZ(i - 1), SymbolicInt(1)), z)
        }
      }
      val zValues = 0.to(b0Times + b1Times).map(createZ)
      val yValues = 0.to(b0Times).map(createY(_, zValues))
      val returnValue = ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(123), yValues.last), None)
      val preamble = List(enterF, enterG)
      val assigns = AssignmentsStoreUpdate(List("y" -> yValues.last, "z" -> zValues.last))
      val exit = List(exitG, exitF)
      val storeUpdates = preamble ++ List(assigns) ++ exit
      val condition = RelationalExpression(SymbolicInputInt(RegularId(b0Times + b1Times - 1, 1)), IntEqual, returnValue)
      (condition, storeUpdates.map(StoreUpdatePCElement(_)))
    }

    val tc0_0: ConstraintES = makeTargetChosen(0, 0)
    val tc0_1: ConstraintES = makeTargetChosen(1, 0)
    val tc1_0: ConstraintES = makeTargetChosen(0, 1)
    val tc1_1: ConstraintES = makeTargetChosen(1, 1)
    val tc0_0T = ConstraintWithStoreUpdate(tc0_0, true)
    val tc0_1T = ConstraintWithStoreUpdate(tc0_1, true)
    val tc1_0T = ConstraintWithStoreUpdate(tc1_0, true)

    val (b0ConditionExp_0, storesUpdates_0) = createBCondition(1, 0, 10)
    val (b0ConditionExp_1, storesUpdates_1) = createBCondition(2, 0, 10)
    val (b1ConditionExp_0, storesUpdates_2) = createBCondition(0, 1, 20)

    val b0ConditionConst_0: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(b0ConditionExp_0, Nil), 0)
    val b0Condition_0 = ConstraintWithStoreUpdate(b0ConditionConst_0, false)

    val b0ConditionConst_1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(b0ConditionExp_1, Nil), 0)
    val b0Condition_1 = ConstraintWithStoreUpdate(b0ConditionConst_1, false)

    val b1ConditionConst_0: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(b1ConditionExp_0, Nil), 0)
    val b1Condition_0 = ConstraintWithStoreUpdate(b1ConditionConst_0, false)

    val targetEnd0 = TargetEndExecutionState(0)

    val stt0_const: ConstraintES = EventConstraintWithExecutionState(
      StopTestingTargets(0, processesInfo.processesInfo, Nil, startingProcess), targetEnd0)
    val stt0 = ConstraintWithStoreUpdate(stt0_const, true)
    val pc1: PathConstraintWithStoreUpdates[ConstraintES] = List(tc0_0T) ++ storesUpdates_0 :+ b0Condition_0 :+ stt0
    reporter.addExploredPath(pc1, true)

    val pc2: PathConstraintWithStoreUpdates[ConstraintES] = List(tc1_0T) ++ storesUpdates_1 :+ b1Condition_0 :+ stt0
    val treePathAdded = reporter.addExploredPath(pc2, true)

    val store: SymbolicStore = Map[String, SymbolicExpression](
      "z" -> SymbolicInt(1, z),
      "y" -> ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInputInt(RegularId(0, 1)), SymbolicInt(20))))
    val root = reporter.getRoot.get
    nodeMerger.mergeWorkListStates(root, treePathAdded.init, targetEnd0)

    assert(root.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    root match {
      case rootBsn@BranchSymbolicNode(cond, left, right) =>
        assert(cond == tc0_0)
        left.to match {
          case BranchSymbolicNode(cond, thenBranch, elseBranch1) =>
            assert(cond == b0Condition_0.constraint)
            assertUnexploredNode(thenBranch)
            elseBranch1.to match {
              case BranchSymbolicNode(_, thenBranch, elseBranch) =>
                assertLeafNode(thenBranch)
                assertUnexploredNode(elseBranch)
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
        right.to match {
          case BranchSymbolicNode(cond, left, right) =>
            assert(cond == tc1_0)
            assertUnsatisfiableNode(right)
            left.to match {
              case BranchSymbolicNode(cond, left, should_be_merged2) =>
                assert(cond == b1Condition_0.constraint)
                assertUnexploredNode(left)
                assert(
                  should_be_merged2.to == rootBsn.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]].elseBranch.to)
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
    }
  }

}
