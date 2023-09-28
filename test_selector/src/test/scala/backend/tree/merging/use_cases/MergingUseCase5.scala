package backend.tree.merging.use_cases

import backend.TestConfigs.treeLogger
import backend.execution_state.TargetEndExecutionState
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.BranchSymbolicNode
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{ProcessesInfo, StopTestingTargets}
import backend.tree.merging.ConstraintESNodeMerger

class MergingUseCase5 extends MergingUseCasesTest {

  test("Merging use_case 5") {
    val x = Some("x")
    val y = Some("y")
    val z = Some("z")
    val reporter = new ConstraintESReporter(MergingMode, None)
    val processesInfo = ProcessesInfo(List(3), true)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)

    val (_, path1Before, path1After) = makeGenericEventPath(processesInfo, 0, (1, 2, 4))
    val path1 = path1Before ++ path1After
    reporter.addExploredPath(path1, true)
    val (_, path2Before, path2After) = makeGenericEventPath(processesInfo, 1, (10, 20, 40))
    val path2 = path2Before ++ path2After
    val treePathAdded1 = reporter.addExploredPath(path2Before, false)

    var root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path2, true)

    var nodeF = root.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]] // TargetChosen for tid 1

    assert(root.thenBranch.to == nodeF.thenBranch.to)
    val stopTestingNode = root.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    assert(stopTestingNode.constraint.asInstanceOf[BasicConstraintES].ec.isInstanceOf[StopTestingTargets])
    stopTestingNode.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]].constraint match {
      case c: EventConstraintWithExecutionState => c.ec match {
        case BranchConstraint(exp, _) => exp match {
          case RelationalExpression(ArithmeticalVariadicOperationExpression(IntPlus, args, _), IntEqual, zExp, _) =>
            assert(args.length == 2)
            val xExp = args.head
            val yExp = args(1)
            xExp match {
              case actualITE: SymbolicITEExpression[SymbolicInt, SymbolicInt] =>
                val inputEvent = SymbolicInputEvent(RegularId(0, 0))
                val event0Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 0)
                val event1Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 1)
                val ite1 = ite(event0Chosen, SymbolicInt(1, x), SymbolicInt(10, x), x)
                val ite2 = ite(event1Chosen, SymbolicInt(10, x), SymbolicInt(1, x), x)
                assert(iteEquals(actualITE, ite1) || iteEquals(actualITE, ite2))
              case other => assert(false, s"Found a $other")
            }
            yExp match {
              case actualITE: SymbolicITEExpression[SymbolicInt, SymbolicInt] =>
                val inputEvent = SymbolicInputEvent(RegularId(0, 0))
                val event0Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 0)
                val event1Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 1)
                val ite1 = ite(event0Chosen, SymbolicInt(2, y), SymbolicInt(20, y), y)
                val ite2 = ite(event1Chosen, SymbolicInt(20, y), SymbolicInt(2, y), y)
                assert(iteEquals(actualITE, ite1) || iteEquals(actualITE, ite2))
              case other => assert(false, s"Found a $other")
            }
            zExp match {
              case actualITE: SymbolicITEExpression[SymbolicInt, SymbolicInt] =>
                val inputEvent = SymbolicInputEvent(RegularId(0, 0))
                val event0Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 0)
                val event1Chosen = SymbolicEventChosenExpression(inputEvent, EventChosenOperator, 1)
                val ite1 = ite(event0Chosen, SymbolicInt(4, z), SymbolicInt(40, z), z)
                val ite2 = ite(event1Chosen, SymbolicInt(40, z), SymbolicInt(4, z), z)
                assert(iteEquals(actualITE, ite1) || iteEquals(actualITE, ite2))
              case other => assert(false, s"Found a $other")
            }
          case other => assert(false, s"Found a $other")
        }
        case other => assert(false, s"Found a $other")
      }
      case other => assert(false, s"Found a $other")
    }

    val (_, path3Before, path3After) = makeGenericEventPath(processesInfo, 2, (100, 200, 400))
    val path3 = path3Before ++ path3After
    val treePathAdded2 = reporter.addExploredPath(path3Before, false)
    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path3, true)

    root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeF = root.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]] // TargetChosen for tid 1
    val nodeFF = nodeF.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]] // TargetChosen for tid 2
    assert(nodeF.thenBranch.to == nodeFF.thenBranch.to)
  }

}
