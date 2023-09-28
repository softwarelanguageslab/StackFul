package backend.tree.merging.use_cases

import backend.PathConstraintWithStoreUpdates
import backend.TestConfigs.treeLogger
import backend.execution_state.{SymbolicStore, TargetEndExecutionState}
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.BranchSymbolicNode
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{ProcessesInfo, StopTestingTargets}
import backend.tree.merging.ConstraintESNodeMerger
import org.scalatest.Ignore

class MergingUseCase4 extends MergingUseCasesTest {

  import Util._

  protected val x: Option[String] = Some("x")
  protected val processesInfo: ProcessesInfo = ProcessesInfo(List(3), true)
  protected def setupUseCase: UseCaseSetup = {
    val path1: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = makeEventPath1(x, processesInfo)
    val path2: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = makeEventPath2(x, processesInfo)
    val path3: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = makeEventPath3(x, processesInfo)
    val reporter: ConstraintESReporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger: ConstraintESNodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    UseCaseSetup(path1, path2, path3, reporter, nodeMerger)
  }
  protected def checkUseCase(reporter: ConstraintESReporter, processesInfo: ProcessesInfo): Unit = {
    val root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    val nodeF = root.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]] // TargetChosen for tid 1
    val nodeFF = nodeF.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]] // TargetChosen for tid 2

    assert(root.thenBranch.to == nodeF.thenBranch.to)
    assert(nodeF.thenBranch.to == nodeFF.thenBranch.to)
    val stopTestingNode = root.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    assert(stopTestingNode.constraint.asInstanceOf[BasicConstraintES].ec.isInstanceOf[StopTestingTargets])
    stopTestingNode.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]].constraint match {
      case c: EventConstraintWithExecutionState => c.ec match {
        case BranchConstraint(exp, _) => exp match {
          case RelationalExpression(left, IntEqual, SymbolicInt(10, _), _) => left match {
            case actualITE: SymbolicITEExpression[SymbolicInt, SymbolicITEExpression[SymbolicInt, SymbolicInt]] =>
              CheckITEExpressionAsserter.checkITEExpression(
                List((SymbolicInputEvent(RegularId(0, 0)), 0)), actualITE, 1, List(3))
              CheckITEExpressionAsserter.checkITEExpression(
                List((SymbolicInputEvent(RegularId(0, 0)), 1)), actualITE, 10, List(3))
              CheckITEExpressionAsserter.checkITEExpression(
                List((SymbolicInputEvent(RegularId(0, 0)), 2)), actualITE, 100, List(3))
            case other => assert(false, s"Found a $other")
          }
          case other => assert(false, s"Found a $other")
        }
        case other => assert(false, s"Found a $other")
      }
      case other => assert(false, s"Found a $other")
    }

    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "TT", "x", 1, processesInfo.processesInfo)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "ETT", "x", 10, processesInfo.processesInfo)
    CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, "EETT", "x", 100, processesInfo.processesInfo)
  }
  case class UseCaseSetup(
    path1: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]),
    path2: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]),
    path3: (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]),
    reporter: ConstraintESReporter,
    nodeMerger: ConstraintESNodeMerger
  )

  test("Merging use_case 4: add P1, add P2, merge, add P3, merge") {
    val UseCaseSetup((_, path1Before, path1After), (_, path2Before, path2After), (_, path3Before, path3After), reporter, nodeMerger) = setupUseCase
    val path1 = path1Before ++ path1After
    val path2 = path2Before ++ path2After
    val path3 = path3Before ++ path3After

    // add path1, add path2Before, merge, add path2
    reporter.addExploredPath(path1, true)
    val treePathAdded1 = reporter.addExploredPath(path2Before, false)
    var root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path2, true)

    // add path3Before, merge, add path3
    root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    val treePathAdded2 = reporter.addExploredPath(path3Before, false)
    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path3, true)

    checkUseCase(reporter, processesInfo)
  }

  // Ignored, because when adding a TargetChosen constraint of tid = n to the tree,
  // all TargetChosen nodes with constraints < n should have already been added
  ignore("Merging use_case 4: add P2, add P1, merge, add P3, merge") {
    val UseCaseSetup((_, path1Before, path1After), (_, path2Before, path2After), (_, path3Before, path3After), reporter, nodeMerger) = setupUseCase
    val path1 = path1Before ++ path1After
    val path2 = path2Before ++ path2After
    val path3 = path3Before ++ path3After

    // add path2, add path1Before, merge, add path1
    reporter.addExploredPath(path2, true)
    val treePathAdded1 = reporter.addExploredPath(path1Before, false)
    var root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeMerger.mergeWorkListStates(root, treePathAdded1.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path1, true)

    // add path3Before, merge, add path3
    val treePathAdded2 = reporter.addExploredPath(path3Before, false)
    root = reporter.getRoot.get.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    nodeMerger.mergeWorkListStates(root, treePathAdded2.init, TargetEndExecutionState(0))
    reporter.addExploredPath(path3, true)

    checkUseCase(reporter, processesInfo)
  }

}
