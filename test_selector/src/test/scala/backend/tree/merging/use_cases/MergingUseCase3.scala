package backend.tree.merging.use_cases

import backend.TestConfigs.treeLogger
import backend.execution_state.TargetEndExecutionState
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.BranchSymbolicNode
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state.{BasicConstraintES, ConstraintES}
import backend.tree.constraints.event_constraints.{ProcessesInfo, StopTestingTargets}
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase3 extends MergingUseCasesTest {

  test("Merging use_case 3") {
    /**
      * var x;
      * function eventHandler1() {
      *   x = 1;
      * }
      * function eventHandler2() {
      *   x = 10;
      * }
      * || generate event ||
      * if (x == 10) {} else {}
      */
    val x = Some("x")
    val reporter = new ConstraintESReporter(MergingMode, None)
    val processesInfo = ProcessesInfo(List(3), true)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)

    val (_, path1Before, path1After) = makeEventPath1(x, processesInfo)
    val path1 = path1Before ++ path1After
    reporter.addExploredPath(path1, true)
    val (_, path2Before, path2After) = makeEventPath2(x, processesInfo)
    val completePath2 = path2Before ++ path2After
    val treePathAdded = reporter.addExploredPath(path2Before, false)

    val root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeMerger.mergeWorkListStates(root, treePathAdded.init, TargetEndExecutionState(0))

    reporter.addExploredPath(completePath2, true)

    assert(reporter.getRoot.get.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(root.thenBranch.to.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(root.elseBranch.to.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(root.thenBranch.to == root.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]].thenBranch.to)
    val castedT = root.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    assert(castedT.constraint.asInstanceOf[BasicConstraintES].ec.isInstanceOf[StopTestingTargets])
    castedT.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]].constraint match {
      case c: BasicConstraintES => c.ec match {
        case BranchConstraint(exp, _) => exp match {
          case RelationalExpression(left, IntEqual, SymbolicInt(10, _), _) => left match {
            case actualITE: SymbolicITEExpression[SymbolicInt@unchecked, SymbolicInt@unchecked] =>
              val inputEvent = SymbolicInputEvent(RegularId(0, 0))
              val event0Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 0)
              val event1Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 1)
              val ite1 = ite(event0Chosen, SymbolicInt(1, x), SymbolicInt(10, x), x)
              val ite2 = ite(event1Chosen, SymbolicInt(10, x), SymbolicInt(1, x), x)
              assert(iteEquals(actualITE, ite1) || iteEquals(actualITE, ite2))
            case other => assert(false, s"Found a $other")
          }
          case other => assert(false, s"Found a $other")
        }
        case other => assert(false, s"Found a $other")
      }
      case other => assert(false, s"Found a $other")
    }
  }

}
