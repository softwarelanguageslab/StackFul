package backend.modes

import backend.StartupConfiguration
import backend.communication._
import backend.logging.TreeLogger
import backend.metrics._
import backend.reporters.ConstraintESReporter
import backend.solvers.{ActionNotApplied, SolverResult}
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.merging.ConstraintESNodeMerger
import backend.util.timer

class ExploreTreeMerge(val treeOutputPath: String, config: StartupConfiguration) extends SMTSolveProcess[ConstraintES] {
  override type Input = ExploreMergeModeJSONInput
  override val allInOne: ConstraintAllInOne[ConstraintES] = ConstraintESAllInOne
  val exploreTreeMergeJsonParsing: ExploreTreeMergeJsonParsing = new ExploreTreeMergeJsonParsing
  implicit private val negater: ConstraintNegater[ConstraintES] = allInOne.constraintNegater
  private val reporter: ConstraintESReporter = ConstraintESReporter.newReporter(MergingMode)
  private[modes] val exploreTree: ExploreTree[ConstraintES, backend.PCElementWithStoreUpdate[ConstraintES]] = ExploreTree(
    true, treeOutputPath, allInOne, reporter, ExploreTreeMergeFactory(allInOne))
  private[modes] val writer = new SymbolicTreeDotWriter[ConstraintES]
  private val treeLogger: TreeLogger = TreeLogger(writer, treeOutputPath, config.treeLogging)
  private[modes] val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)(allInOne.toBooleanExpression)
  private val metricsWriter = new MergingMetricsWriter("/Users/mvdcamme/PhD/Projects/Concolic_Testing_Backend/output/metrics/merging.csv")

  override def handleJSONInput(input: Input): SolverResult = input match {
    case ExploreMergeModeInput(exploreModeInput) =>
      println(s"======== Iteration ${exploreModeInput.iteration} ========")
      val result = exploreTree.handleJSONInput(exploreModeInput)
      MergingMetricsKeeper.newIteration()
      metricsWriter.write(MergingMetricsKeeper.getAllMetrics)
      result
    case tryMerge@TryMerge(iteration, executionState, pathConstraint, prescribedEvents, i) =>
      exploreTree.reporter = ConstraintESReporter.withPrescribedEvents(exploreTree.reporter.asInstanceOf[ConstraintESReporter], prescribedEvents)
      val treePathAdded = exploreTree.addPathConstraint(pathConstraint, false)
      exploreTree.reporter.getRoot.foreach(writer.writeTree(_, treeOutputPath))
      val optEndNode = if (treePathAdded.length > 0) Some(treePathAdded.lastNode) else None
      val optResult = exploreTree.reporter.getRoot.map(root => {
        val (result, mergingOverhead) = timer(nodeMerger.mergeWorkListStates(root, treePathAdded.init, executionState, optEndNode))
        MergingMetricsKeeper.addMergingOverhead(mergingOverhead)
        result
      })
      exploreTree.reporter.getRoot.foreach(writer.writeTree(_, treeOutputPath))
      optResult.getOrElse(ActionNotApplied("No root created yet"))
  }
  protected def constraintHasStoreUpdates: HasStoreUpdates[ConstraintES] = ConstraintESHasStoreUpdates
  override protected def parseInput(parsedJSON: ParsedJSON): Input = {
    exploreTreeMergeJsonParsing.parse(parsedJSON)
  }
}
