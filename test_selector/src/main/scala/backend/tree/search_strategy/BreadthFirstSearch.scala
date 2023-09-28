package backend.tree.search_strategy

import backend.coverage_info.BranchCoverageMap
import backend.execution_state.{CodeLocationExecutionState, CodePosition}

import scala.annotation.tailrec
import scala.collection.immutable._
import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, ConstraintESAllInOne, EventConstraintWithExecutionState}
import backend.tree.constraints.event_constraints.TargetChosen.negate
import backend.tree.constraints.event_constraints.TargetChosen
import backend.tree.constraints.{Constraint, ConstraintNegater}
import backend.tree.follow_path._

trait SearchStrategyCached[C <: Constraint] {
  def next(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]]
  def findUnexploredNodes(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]]
}

class ConstraintESBreadthFirstSearchCached(root: SymbolicNode[ConstraintES]) extends BreadthFirstSearchCached[ConstraintES](root)(ConstraintESAllInOne.constraintNegater) {

  protected var tryUncoveredNodesFirst: Boolean = true

  override def next(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[ConstraintES]] = {
    if (tryUncoveredNodesFirst) {
      lastQueue match {
        case None => throw new Exception("No cache exists")
        case Some(queue) =>
          (optBranchCoverage, optEventHandlersCoverage) match {
            case (Some(branchCoverage), Some(eventHandlersCoverage)) =>
              val optUnexploredBranchFound = loopWithCoverageInfo(queue, branchCoverage, eventHandlersCoverage)
              optUnexploredBranchFound match {
                case Some(unexploredBranchFound) =>
                  println("ConstraintESBreadthFirstSearchCached.next found an unexplored node, with a branch that has not yet been taken")
                  Some(unexploredBranchFound)
                case None =>
                  tryUncoveredNodesFirst = false
                  println("ConstraintESBreadthFirstSearchCached.next did not find an uncovered branch, reverting to BSF")
                  super.findUnexploredNodes()
              }
            case _ => loopNoCoverageInfo(queue)
          }
      }
    } else {
      super.next()
    }
  }

  override def findUnexploredNodes(
    optBranchCoverage: Option[BranchCoverageMap] = None,
    optEventHandlersCoverage: Option[List[String]] = None
  ): Option[TreePath[ConstraintES]] = {
    lastQueue = None
    tryUncoveredNodesFirst = true
    root match {
      case bsn: SymbolicNodeWithConstraint[ConstraintES] =>
        val (path, _) = TreePath.init(bsn)(ConstraintESAllInOne.constraintNegater)
        val initialQueue = Queue(path)
        (optBranchCoverage, optEventHandlersCoverage) match {
          case (Some(branchCoverage), Some(eventHandlersCoverage)) =>
            val optUnexploredBranchFound = loopWithCoverageInfo(initialQueue, branchCoverage, eventHandlersCoverage)
            optUnexploredBranchFound match {
              case Some(unexploredBranchFound) =>
                println("ConstraintESBreadthFirstSearchCached.findUnexploredNode found an unexplored node, with a branch that has not yet been taken")
                Some(unexploredBranchFound)
              case None =>
                tryUncoveredNodesFirst = false
                println("ConstraintESBreadthFirstSearchCached.next did not find an uncovered branch, reverting to BSF")
                loopNoCoverageInfo(initialQueue)
            }
          case _ => loopNoCoverageInfo(initialQueue)
        }
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
        None
    }
  }

  @tailrec
  protected final def loopWithCoverageInfo(
    queue: Queue[TreePath[ConstraintES]],
    branchCoverage: BranchCoverageMap,
    eventHandlersCoverage: List[String]
  ): Option[TreePath[ConstraintES]] = {

    def hasBranchBeenExplored(optCodePosition: Option[CodePosition], direction: Direction) = {
      optCodePosition.map(codePosition => branchCoverage.map.get(codePosition).map(
        directions => directions.contains(direction)).getOrElse(false)).getOrElse(true)
    }

    def targetToString(targetChosen: TargetChosen): String = {
      s"{pid:${targetChosen.processIdChosen};tid:${targetChosen.targetIdChosen}}"
    }

    def nodeElseBranchInteresting(branchSymbolicNode: BranchSymbolicNode[ConstraintES]): Boolean = branchSymbolicNode.constraint match {
      case ec: EventConstraintWithExecutionState => ec.ec match {
        case tc: TargetChosen =>
          val alternative = negate(tc)
          !eventHandlersCoverage.contains(targetToString(alternative))
        case _ =>
          val es = ec.executionState
          val optCodePosition = es match {
            case es: CodeLocationExecutionState => Some(es.codePosition)
            case _ => None
          }
          !hasBranchBeenExplored(optCodePosition, ElseDirection)
      }
    }

    def nodeThenBranchInteresting(branchSymbolicNode: BranchSymbolicNode[ConstraintES]): Boolean = branchSymbolicNode.constraint match {
      case ec: EventConstraintWithExecutionState => ec.ec match {
        case tc: TargetChosen =>
          !eventHandlersCoverage.contains(targetToString(tc))
        case _ =>
          val es = ec.executionState
          val optCodePosition = es match {
            case es: CodeLocationExecutionState => Some(es.codePosition)
            case _ => None
          }
          !hasBranchBeenExplored(optCodePosition, ThenDirection)
      }
    }
    queue.headOption match {
      case None => None
      case Some(path) =>
        val tailQueue = queue.tail
        val latestNode = path.lastNode
        latestNode match {
          case b: BranchSymbolicNode[ConstraintES] =>
            val es = b.constraint.executionState
            val optCodePosition = es match {
              case es: CodeLocationExecutionState => Some(es.codePosition)
              case _ => None
            }
            // If branch has not shown up yet in branch coverage, the branch definitely has not been explored yet
            // TODO only works for code positions at the moment, considers any non-code-position branch to already have been explored
            val elseBranchInteresting: Boolean = nodeElseBranchInteresting(b)
            val thenBranchInteresting: Boolean = nodeThenBranchInteresting(b)

            // Both branches have already been explored, so continue looking through both branches to find an unexplored node
            // Although both branches have been explored, it could be that they don't actually have any successors, e.g., because the branch ends
            // Only add child-branches if they are a BranchSymbolicNode
            val newQueue: Queue[TreePath[ConstraintES]] = (b.thenBranch.to, b.elseBranch.to) match {
              // Negate branches
              case (thenNode: SymbolicNodeWithConstraint[ConstraintES],
              elseNode: SymbolicNodeWithConstraint[ConstraintES]) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch) :+ path.addElseBranch(elseNode, b.elseBranch)
              case (thenNode: SymbolicNodeWithConstraint[ConstraintES], _) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch)
              case (_, elseNode: SymbolicNodeWithConstraint[ConstraintES]) =>
                tailQueue :+ path.addElseBranch(elseNode, b.elseBranch)
              case (_, _) => tailQueue
            }

            lastQueue = Some(newQueue)
            if ((!b.thenBranchTaken && thenBranchInteresting) || (!b.elseBranchTaken && elseBranchInteresting)) {
              val finishedPath = path.finished
              Some(finishedPath)
            } else {
              loopWithCoverageInfo(newQueue, branchCoverage, eventHandlersCoverage)
            }
        }
    }
  }
}

class BreadthFirstSearchCached[C <: Constraint : ConstraintNegater](root: SymbolicNode[C]) extends SearchStrategyCached[C] {

  protected var pathsFound: Set[TreePath[C]] = Set()

  protected var lastQueue: Option[Queue[TreePath[C]]] = None

  def next(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]] = {
    lastQueue match {
      case None => throw new Exception("No cache exists")
      case Some(queue) => loopNoCoverageInfo(queue)
    }
  }

  def findUnexploredNodes(ptBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]] = {
    lastQueue = None
    root match {
      case bsn: SymbolicNodeWithConstraint[C] =>
        val (path, _) = TreePath.init(bsn)
        loopNoCoverageInfo(Queue(path))
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
        None
    }
  }

  @tailrec
  protected final def loopNoCoverageInfo(queue: Queue[TreePath[C]]): Option[TreePath[C]] = {
    queue.headOption match {
      case None => None
      case Some(path) =>
        val tailQueue = queue.tail
        val latestNode = path.lastNode
        latestNode match {
          case b: BranchSymbolicNode[C] =>
            // Both branches have already been explored, so continue looking through both branches to find an unexplored node
            // Although both branches have been explored, it could be that they don't actually have any successors, e.g., because the branch ends
            // Only add child-branches if they are a BranchSymbolicNode
            val newQueue: Queue[TreePath[C]] = (b.thenBranch.to, b.elseBranch.to) match {
              // Negate branches
              case (thenNode: SymbolicNodeWithConstraint[C],
                    elseNode: SymbolicNodeWithConstraint[C]) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch) :+ path.addElseBranch(elseNode, b.elseBranch)
              case (thenNode: SymbolicNodeWithConstraint[C], _) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch)
              case (_, elseNode: SymbolicNodeWithConstraint[C]) =>
                tailQueue :+ path.addElseBranch(elseNode, b.elseBranch)
              case (_, _) => tailQueue
            }

            lastQueue = Some(newQueue)
            if (!b.thenBranchTaken || !b.elseBranchTaken) {
              val finishedPath = path.finished
              if (pathsFound.contains(finishedPath)) {
                loopNoCoverageInfo(newQueue)
              } else {
//                pathsFound += finishedPath
                Some(finishedPath)
              }
            } else {
              loopNoCoverageInfo(newQueue)
            }
        }
    }
  }
}

class BreadthFirstSearch[C <: Constraint : ConstraintNegater] extends SearchStrategy[C] {

  def findUnexploredNodes(
    symbolicNode: SymbolicNode[C]
  ): LinearSeq[TreePath[C]] =
    symbolicNode match {
      case bsn: SymbolicNodeWithConstraint[C] =>
        val (path, _) = TreePath.init(bsn)
        //        if (isUnexplored) {
        //          // Very first node in the tree is unexplored, so just return that one
        //          Some(path.finished)
        //        } else {
        val queue: Queue[TreePath[C]] = Queue(path)
        loop(queue, List())
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() |
           UnsatisfiableNode() =>
        List()
    }

  @tailrec
  private def loop(
    queue: Queue[TreePath[C]],
    result: LinearSeq[TreePath[C]]
  ): LinearSeq[TreePath[C]] =
    queue.headOption match {
      case None => result
      case Some(path) =>
        val tailQueue = queue.tail
        val latestNode = path.lastNode
        latestNode match {
          case b: BranchSymbolicNode[C] =>
            val collection1 = if (!b.thenBranchTaken || !b.elseBranchTaken) List(path.finished) else List()
            // Both branches have already been explored, so continue looking through both branches to find an unexplored node
            // Although both branches have been explored, it could be that they don't actually have any successors, e.g., because the branch ends

            // Only add child-branches if they are a BranchSymbolicNode
            val newQueue: Queue[TreePath[C]] = (b.thenBranch.to, b.elseBranch.to) match {
              // Negate branches
              case (thenNode: SymbolicNodeWithConstraint[C],
              elseNode: SymbolicNodeWithConstraint[C]) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch) :+ path.addElseBranch(elseNode, b.elseBranch)
              case (thenNode: SymbolicNodeWithConstraint[C], _) =>
                tailQueue :+ path.addThenBranch(thenNode, b.thenBranch)
              case (_, elseNode: SymbolicNodeWithConstraint[C]) =>
                tailQueue :+ path.addElseBranch(elseNode, b.elseBranch)
              case (_, _) => tailQueue
            }
            loop(newQueue, result ++ collection1)
        }
    }
}
