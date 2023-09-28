package backend.modes

import backend._
import backend.communication._
import backend.coverage_info.BranchCoverageMap
import backend.logging.Logger
import backend.reporters.Reporter
import backend.solvers._
import backend.solvers.solve_events.SolverWithEvents
import backend.tree.constraints._
import backend.tree.constraints.event_constraints.EventConstraintAllInOne
import backend.tree.search_strategy._
import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, ConstraintESConstantChecker, ConstraintESOptimizer}

class ExploreTree[C <: Constraint, PCElementUsed <: PCElement[C]](
  val prescribeEvents: Boolean,
  val treeOutputPath: String,
  val createSearchStrategy: SymbolicNode[C] => SearchStrategyCached[C],
  var reporter: Reporter[PathConstraintWith[C, PCElementUsed], C],
  val allInOne: ConstraintAllInOne[C],
  val sanitizer: Sanitizer,
  val optimizer: Optimizer[PCElementUsed],
  val constantChecker: ConstantChecker[PCElementUsed]
)(
  implicit val constraintHasStoreUpdates: HasStoreUpdates[C]
)
  extends SMTSolveProcess[C] {

  import allInOne._

  override type Input = ExploreTree.Input
  val exploreTreeJsonParsing: ExploreTreeJsonParsing = new ExploreTreeJsonParsing
  def treeEmpty: Boolean = {
    reporter.getRoot.fold(true)({
      case _: BranchSymbolicNode[_] => false
      case _ => true
    })
  }
  def handleJSONInput(input: Input): SolverResult = input match {
    case ExploreModeInput(iteration, pc: PathConstraintWith[C, PCElementUsed]@unchecked, processesInfo, finishedTestRun, optBranchCoverage, optEventHandlersCoverage) =>
      addPathConstraint(pc, finishedTestRun)
      reporter.getRoot.foreach(treeDotWriter.writeTree(_, treeOutputPath))
      reporter.getRoot match {
        case Some(root) =>
          val solver = makeSolver(root, processesInfo, optBranchCoverage, optEventHandlersCoverage)
          val result = solver.solve()
          reporter.getRoot.foreach(treeDotWriter.writeTree(_, treeOutputPath))
          result
        case None =>
          reporter.getRoot.foreach(treeDotWriter.writeTree(_, treeOutputPath))
          SymbolicTreeFullyExplored
      }
  }
  def addPathConstraint(pc: PathConstraintWith[C, PCElementUsed], finishedTestRun: Boolean): TreePath[C] = {
    val sanitizedPathConstraint: PathConstraintWith[C, PCElementUsed] = sanitizer.sanitize(pc)(
      optimizer, constantChecker)
    Logger.v(s"Sanitized path constraint $sanitizedPathConstraint")
    val pathAdded = reporter.addExploredPath(sanitizedPathConstraint, finishedTestRun)
    Logger.v(s"Finished adding path: ${reporter.getRoot}")
    reporter.getRoot.foreach(treeDotWriter.writeTree(_, treeOutputPath))
    pathAdded
  }
  def makeSolver(root: SymbolicNode[C], processesInfo: List[Int], optBranchCoverage: Option[BranchCoverageMap], optEventHandlersCoverage: Option[List[String]]): Solver[C] = {
    val searchStrategy = createSearchStrategy(root)
    if (prescribeEvents) {
      new SolverWithEvents[C](root, searchStrategy, processesInfo, optBranchCoverage, optEventHandlersCoverage)
    } else {
      new BasicSolver[C](searchStrategy, processesInfo)
    }
  }
  override protected def parseInput(parsedJSON: ParsedJSON): Input = {
    exploreTreeJsonParsing.parse(parsedJSON)
  }

}

object ExploreTree {

  type Input = ExploreModeJSONInput

  def apply[C <: Constraint : ConstraintNegater : Optimizer, PCElementUsed <: PCElement[C]](
    prescribeEvents: Boolean,
    treeOutputPath: String,
    allInOne: ConstraintAllInOne[C],
    reporter: Reporter[PathConstraintWith[C, PCElementUsed], C],
    factory: ExplorationModeFactory[C, PCElementUsed]
  )(implicit constraintHasStoreUpdates: HasStoreUpdates[C]): ExploreTree[C, PCElementUsed] = {
    new ExploreTree[C, PCElementUsed](
      prescribeEvents,
      treeOutputPath,
      factory.makeSearchStrategy,
      reporter,
      allInOne,
      factory.makeSanitizer,
      factory.makeOptimizer,
      factory.makeConstantChecker)
  }
}

trait ExplorationModeFactory[C <: Constraint, PCElementUsed <: PCElement[C]] {
  def makeSanitizer: Sanitizer
  def makeOptimizer: Optimizer[PCElementUsed]
  def makeConstantChecker: ConstantChecker[PCElementUsed]
  def makeSearchStrategy(root: SymbolicNode[C]): SearchStrategyCached[C]
}

object ExploreTreeFactory extends ExplorationModeFactory[EventConstraint, RegularPCElement[EventConstraint]] {
  private val allInOne: ConstraintAllInOne[EventConstraint] = implicitly[ConstraintAllInOne[EventConstraint]]

  import allInOne._

  def makeSanitizer: Sanitizer = RemoveConstantsSanitizer
  def makeOptimizer: Optimizer[RegularPCElement[EventConstraint]] = RegularPCElementOptimizer()
  def makeConstantChecker: ConstantChecker[RegularPCElement[EventConstraint]] = RegularPCElementConstantChecker()
  def makeSearchStrategy(root: SymbolicNode[EventConstraint]): SearchStrategyCached[EventConstraint] = {
    new BreadthFirstSearchCached[EventConstraint](root)
  }
}

case class ExploreTreeMergeFactory(allInOne: ConstraintAllInOne[ConstraintES]) extends ExplorationModeFactory[ConstraintES, PCElementWithStoreUpdate[ConstraintES]] {

  import allInOne._

  def makeSanitizer: Sanitizer = RemoveConstantsSanitizer
  def makeOptimizer: Optimizer[PCElementWithStoreUpdate[ConstraintES]] = PCElementWithStoreUpdateOptimizer(
    ConstraintESOptimizer)
  def makeConstantChecker: ConstantChecker[PCElementWithStoreUpdate[ConstraintES]] = PCElementWithStoreUpdateConstantChecker(
    ConstraintESConstantChecker)
  def makeSearchStrategy(root: SymbolicNode[ConstraintES]): SearchStrategyCached[ConstraintES] = {
    new ConstraintESBreadthFirstSearchCached(root)
  }
}