package backend.solvers.solve_events

import backend.coverage_info.BranchCoverageMap
import backend.expression.SymbolicInput
import backend.logging.Logger
import backend.metrics.MergingMetricsKeeper
import backend.solvers._
import backend.tree._
import backend.tree.constraints._
import backend.tree.follow_path.{BinaryDirection, ElseDirection, ThenDirection}
import backend.tree.search_strategy._
import backend.util.timer

import scala.annotation.tailrec

object EventsComputer {

  def getEvents[C <: Constraint](
    constraints: List[C],
    solution: Map[SymbolicInput, ComputedValue]
  ): List[(Int, Int)] = {
    val computedEvents = findComputedEvents(solution)
    val naiveEvents = NaiveEventSolver.getEventSequence(constraints)
    naiveEvents.zipWithIndex.map(tuple => {
      computedEvents.getOrElse(tuple._2, tuple._1)
    })
  }
  private def findComputedEvents(solutions: Map[SymbolicInput, ComputedValue]): Map[Int, (Int, Int)] = {
    solutions.toList.flatMap(_._2 match {
      case ComputedEvent(eventId, processId, targetId) => List((eventId, processId, targetId))
      case _ => Nil
    }).sortBy(_._1).map(triple => (triple._1, (triple._2, triple._3))).toMap
  }

}


class SolverWithEvents[C <: Constraint : ConstraintNegater : ToBasic : UsesIdentifier : ToBooleanExpression](
  root: SymbolicNode[C],
  searchStrategy: SearchStrategyCached[C],
  processesInfo: List[Int],
  optBranchCoverage: Option[BranchCoverageMap],
  optEventHandlersCoverage: Option[List[String]]
)
  extends BasicSolver[C](searchStrategy, processesInfo) {


  override def solve(): SolverResult = {
    // unexploredPaths refer to those paths that end in a BranchNode with at least one branch
    // that has not yet been explored.
    val (unexploredPaths: Option[TreePath[C]], time) = timer(searchStrategy.findUnexploredNodes(optBranchCoverage, optEventHandlersCoverage))
    MergingMetricsKeeper.addFindingPathTime(time)
    @tailrec
    def loop(unexploredPaths: Option[TreePath[C]]): SolverResult = {
      unexploredPaths match {
        case Some(incompletelyExploredPath) =>
          val optUnexploredPath: Option[TreePath[C]] = try {
            val result = PathNegater.negatePath(incompletelyExploredPath)
            Some(result)
          } catch {
            case AllProcessesExplored => None
          }
          optUnexploredPath match {
            case Some(unexploredPath) =>
              //              println(s"SolverWithEvents: path would be ${unexploredPath.getObserved}")
              val constraints = getUsableConstraints(unexploredPath)
//              val optSolution = doOneSolveIteration(constraints, processesInfo)
              val lastNode = unexploredPath.lastNode
              val dir = lastNode match {
                case BranchSymbolicNode(_, thenEdge, elseEdge) =>
                  if (thenEdge.to == UnexploredNode[C]()) {
                    ThenDirection
                  } else if (elseEdge.to == UnexploredNode[C]()) {
                    ElseDirection
                  } else {
                    throw new Exception("Should not happen")
                  }
              }
              val completePathConstraint = new PathConstraintForUnexploredNodeFinder().createPathConstraintFromTo(root, lastNode, dir, getUsableConstraints)
              val optSolution = doOneSolveIteration(completePathConstraint, processesInfo)
              optSolution match {
                case Some(solution) =>
                  Logger.e(s"Computed a solution for path ${unexploredPath.getPath}")
                  val events = EventsComputer.getEvents(constraints, solution)
                  val solutions = InputAndEventSequence(NewInput(solution, Some(unexploredPath)), events, unexploredPath.getPath)
                  Logger.e(s"Solutions = $solution")
                  solutions
                case None =>
//                  lazy val lastNode = incompletelyExploredPath.last._1.asInstanceOf[BranchSymbolicNode[C]]
                  if (incompletelyExploredPath.length > 0) {
                    PathNegater.nodeWasTried(lastNode.asInstanceOf[BranchSymbolicNode[C]], unexploredPath.last._3.toDirection.asInstanceOf[BinaryDirection])
                  }
                  val (newUnexploredPaths, time) = timer(searchStrategy.next(optBranchCoverage, optEventHandlersCoverage))
                  MergingMetricsKeeper.addFindingPathTime(time)
                  loop(newUnexploredPaths)
              }
            case None => SymbolicTreeFullyExplored
          }
        case None => SymbolicTreeFullyExplored
      }
    }
    loop(unexploredPaths)
  }

}

class PathSolverDebugging[C <: Constraint : ConstraintNegater : ToBasic : UsesIdentifier](
  root: SymbolicNode[C],
  searchStrategy: SearchStrategyCached[C],
  processesInfo: List[Int],
  lastDirection: BinaryDirection
)
  extends BasicSolver[C](searchStrategy, processesInfo) {


  override def solve(): SolverResult = {
    // unexploredPaths refer to those paths that end in a BranchNode with at least one branch
    // that has not yet been explored.
    val optUnexploredPath: Option[TreePath[C]] = searchStrategy.findUnexploredNodes()
    @tailrec
    def loop: SolverResult = optUnexploredPath match {
        case Some(almostUnexploredNode) =>
          val castedLastNode = almostUnexploredNode.lastNode.asInstanceOf[BranchSymbolicNode[C]]
          val constraints: List[C] = getUsableConstraints(almostUnexploredNode.init) :+ (if (lastDirection == ThenDirection) castedLastNode.constraint else implicitly[ConstraintNegater[C]].negate(castedLastNode.constraint): C)
          val optSolution = doOneSolveIteration(constraints, processesInfo)
          optSolution match {
            case Some(solution) =>
              val events = EventsComputer.getEvents(constraints, solution)
              val solutions = InputAndEventSequence(
                NewInput(solution, Some(almostUnexploredNode)), events, almostUnexploredNode.getPath)
              solutions
            case None => loop
          }
        case None => SymbolicTreeFullyExplored
      }
      loop
    }

}