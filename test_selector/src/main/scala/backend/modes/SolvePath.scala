package backend.modes

import backend.RegularPCElement
import backend.communication._
import backend.reporters.SolveModePCReporter
import backend.solvers._
import backend.tree.constraints.event_constraints.EventConstraintAllInOne
import backend.tree.constraints._
import backend.tree.follow_path.EventConstraintPathFollower
import backend.tree.path.SymJSState
import backend.tree.search_strategy.TreePath

class SolvePath(val treeOutputPath: String) extends SMTSolveProcess[EventConstraint] {
  type Input = SolveModeInput[EventConstraint]

  override protected implicit val constraintHasStoreUpdates: HasStoreUpdates[EventConstraint] = EventConstraintHasStoreUpdates
  override val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne

  import allInOne._
  val solvePathJsonParsing: SolvePathJsonParsing = new SolvePathJsonParsing
  private val reporter = new SolveModePCReporter()
  def handleJSONInput(input: SolveModeInput[EventConstraint]): SolverResult = {
    val pathConstraint = input.pathConstraint
    val eventSeq = input.eventSequence
    val branchSeq = input.branchSequence
    val optimizedPathConstraint = pathConstraint.map({
      case RegularPCElement(element, isTrue) =>
        //        val optimizedElement = implicitly[Optimizer[C]].optimize(element)
        RegularPCElement(element, isTrue)
      case element => element
    })
    //    println(s"Optimized path constraint $optimizedPathConstraint")
    if (reporter.getRoot.isEmpty && optimizedPathConstraint.isEmpty) {
      if (branchSeq.isEmpty) {
        NewInput(Map(), Some(TreePath.empty))
      } else {
        throw new Exception(
          "Cannot generate any solutions: both the tree-root and the pathConstraint are empty")
      }
    } else {
      reporter.addExploredPath(optimizedPathConstraint, finishedTestRun = true)
      reporter.getRoot.foreach(treeDotWriter.writeTree(_, treeOutputPath))
      reporter.getRoot match {
        case Some(root) =>
          val solver = new PathSolver[EventConstraint](
            root, SymJSState(eventSeq, branchSeq.map(_.toEdgeWithoutTo[EventConstraint])),
            new EventConstraintPathFollower)
          solver.solve()
        case None => SymbolicTreeFullyExplored
      }
    }
  }
  protected def parseInput(parsedJSON: ParsedJSON): SolveModeInput[EventConstraint] = {
    solvePathJsonParsing.parse(parsedJSON)
  }
}
