package backend.solvers

import backend.InvalidPathException
import backend.coverage_info.BranchCoverageMap
import backend.expression.{BooleanExpression, SymbolicInput}
import backend.logging.Logger
import backend.solvers.Z3Solver._
import backend.solvers.solve_events.SolverWithEvents
import backend.tree.constraints._
import backend.tree.follow_path._
import backend.tree.search_strategy._
import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.EventConstraintWithExecutionState
import backend.tree.constraints.event_constraints.TargetChosen

import scala.annotation.tailrec

trait CanDoOneSolveIteration[C <: Constraint] {
  protected def doOneSolveIteration(
    constraints: List[C],
    processesInfo: List[Int]
  ): Option[Map[SymbolicInput, ComputedValue]]
}

trait Solver[C <: Constraint] extends CanDoOneSolveIteration[C] {

  def solve(): SolverResult
  def toBasicConstraints: ToBasic[C]

  protected def doOneSolveIteration(
    constraints: List[C],
    processesInfo: List[Int]
  ): Option[Map[SymbolicInput, ComputedValue]] = {
    val basicConstraints = toBasicConstraints.toBasicConstraints(constraints)
    val solutions = Z3Solver.Z3.solve(basicConstraints, processesInfo)
    Logger.n(s"Solver.doOneSolveIteration, solutions = $solutions")
    solutions match {
      case Satisfiable(solution) => Some(solution)
      case Unsatisfiable => None
      case _: SomeZ3Error => throw new Exception(s"SMT solving failed")
    }
  }

  protected def doOneSolveIteration(
    expression: BooleanExpression,
    processesInfo: List[Int]
  ): Option[Map[SymbolicInput, ComputedValue]] = {
    val solutions = Z3Solver.Z3.solve(expression, processesInfo)
    Logger.n(s"Solver.doOneSolveIteration(BooleanExpression), solutions = $solutions")
    solutions match {
      case Satisfiable(solution) => Some(solution)
      case Unsatisfiable => None
      case _: SomeZ3Error => throw new Exception(s"SMT solving failed")
    }
  }

  protected case class TCInfo(eventId: Int, processId: Int, targetId: Int)
  protected def findLastTidOfEachTC(targetConstraints:  List[(TCInfo, C)]): List[(TCInfo, C)] = {
    @tailrec
    def loop(
      list: List[(TCInfo, C)],
      optLastEntry: Option[(TCInfo, C)],
      acc: List[(TCInfo, C)]
    ): List[(TCInfo, C)] = list.headOption match {
      case None => optLastEntry match {
        case Some(lastEntry) => acc :+ lastEntry
        case None => acc
      }
      case Some(tuple) =>
        val (tcInfo, c) = tuple
        val (newAcc, newOptLastEntry) = optLastEntry match {
          case Some(prevTuple) =>
            assert(prevTuple._1.eventId <= tcInfo.eventId)
            val newAcc = if (prevTuple._1.eventId != tcInfo.eventId) {
              acc :+ prevTuple
            } else {
              acc
            }
            (newAcc, Some(tuple))
          case None =>
            (acc, Some(tuple))
        }
        loop(list.tail, newOptLastEntry, newAcc)
    }
    loop(targetConstraints, None, Nil)
  }

  protected def getUsableConstraints(treePath: TreePath[C]): List[C] = {
    val constraints = treePath.getObserved
    val targetConstraints: List[(TCInfo, C)] = constraints.flatMap({
      case tc: TargetChosen => List((TCInfo(tc.id, tc.processIdChosen, tc.targetIdChosen), tc.asInstanceOf[C]))
      case ec@EventConstraintWithExecutionState(tc: TargetChosen, _) =>
        List((TCInfo(tc.id, tc.processIdChosen, tc.targetIdChosen), ec.asInstanceOf[C]))
      case _ => Nil
    })
    val lastTidConstraints: List[(TCInfo, C)] = findLastTidOfEachTC(targetConstraints)
    constraints.filter({
      case tc: TargetChosen => lastTidConstraints.exists(_._2 == tc)
      case ec@EventConstraintWithExecutionState(tc: TargetChosen, _) =>lastTidConstraints.exists(_._2 == ec)
      case _ =>true
    })
  }

}

object PathNegater {

  def lastNodeWasTried[C <: Constraint : ConstraintNegater](treePath: TreePath[C]): Unit = {
    val (lastNode, _, lastDirection) = treePath.last
    (lastNode, lastDirection.toDirection) match {
      case (bsn: BranchSymbolicNode[C], direction: BinaryDirection) =>
        nodeWasTried(bsn, direction)
      case _ =>
        throw InvalidPathException(treePath.getPath)
    }
  }

  /**
    * To be called when someone negated the given node to create a new, unexplored path, but the resulting path
    * was considered unsatisfiable.
    *
    * @param node
    */
  def nodeWasTried[C <: Constraint : ConstraintNegater](
    node: BranchSymbolicNode[C],
    directionTried: BinaryDirection
  ): Unit = {
    directionTried match {
      case ElseDirection => node.setElseBranch(ElseEdge.to[C](UnsatisfiableNode()))
      case ThenDirection => node.setThenBranch(ThenEdge.to[C](UnsatisfiableNode()))
    }
  }

  def negatePath[C <: Constraint : ConstraintNegater](path: TreePath[C]): TreePath[C] = {
    //    val init = path.init
    val lastNode = path.lastNode
    assert(lastNode.isInstanceOf[BranchSymbolicNode[C]])
    val castedLastNode = lastNode.asInstanceOf[BranchSymbolicNode[C]]
    if (!castedLastNode.thenBranchTaken) {
      //      init :+ castedLastNode
      path.finishedViaThen
    } else {
      //      init !:+ castedLastNode
      path.finishedViaElse
    }
  }
}

class BasicSolver[C <: Constraint : ConstraintNegater : ToBasic](
  searchStrategy: SearchStrategyCached[C],
  processesInfo: List[Int]
)
  extends Solver[C] {

  def toBasicConstraints: ToBasic[C] = implicitly[ToBasic[C]]

  def solve(): SolverResult = {
    // unexploredPaths refer to those paths that end in a BranchNode with at least one branch
    // that has not yet been explored.
    var unexploredPaths: Option[TreePath[C]] = searchStrategy.findUnexploredNodes()
    @tailrec
    def loop: SolverResult = {
      unexploredPaths match {
        case Some(incompletelyExploredPath) =>
          unexploredPaths = searchStrategy.next()
          val unexploredPath: TreePath[C] = PathNegater.negatePath(incompletelyExploredPath)
          Logger.v(s"BasicSolver: path would be ${unexploredPath.getObserved}")
          val optSolution = doOneSolveIteration(getUsableConstraints(unexploredPath), processesInfo)
          optSolution match {
            case Some(solution) => NewInput(solution, Some(unexploredPath))
            case None => loop
          }
        case None => SymbolicTreeFullyExplored
      }
    }
    loop
  }
}

trait WithPrefix[C <: Constraint] extends CanDoOneSolveIteration[C] {
  def prefix: List[C]
  abstract override protected def doOneSolveIteration(
    constraints: List[C],
    processesInfo: List[Int]
  ): Option[Map[SymbolicInput, ComputedValue]] = {
    val allConstraints = prefix ++ constraints
    Logger.v(s"WithPrefix.doOneSolveIteration, allConstraints = $allConstraints")
    super.doOneSolveIteration(allConstraints, processesInfo)
  }
}

class SolverWithPrefix[C <: Constraint : ConstraintNegater : ToBasic](
  val prefix: List[C],
  searchStrategy: SearchStrategyCached[C],
  processesInfo: List[Int]
)
  extends BasicSolver[C](searchStrategy, processesInfo)
    with WithPrefix[C]

class SolverWithEventsAndPrefix[C <: Constraint : ConstraintNegater : ToBasic : UsesIdentifier : ToBooleanExpression](
  val prefix: List[C],
  root: SymbolicNode[C],
  searchStrategy: SearchStrategyCached[C],
  processesInfo: List[Int],
  optBranchCoverage: Option[BranchCoverageMap],
  optEventHandlersCoverage: Option[List[String]]
)
  extends SolverWithEvents[C](root, searchStrategy, processesInfo: List[Int], optBranchCoverage, optEventHandlersCoverage)
    with WithPrefix[C]
