package backend.tree.merging

import backend.execution_state.{ExecutionState, TargetEndExecutionState}
import backend.tree._
import backend.tree.constraints.{Constraint, ConstraintNegater}
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path._
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec
import scala.collection.mutable.{Set => MSet}



case class PathAndNode[C <: Constraint](
  pathToGlobalMergePoint: TreePath[C],
  lastGlobalMergePoint: SymbolicNodeWithConstraint[C],
  treePath: TreePath[C],
  lastNode: SymbolicNode[C]
)

/**
  *
  * @param toFind The ExecutionState to find
  */
abstract class BaseConstraintESWorkListFinder[T](val toFind: ExecutionState)(
  implicit val negater: ConstraintNegater[ConstraintES]
)
  extends WorkListFinder[ConstraintES] {
  type TPath = TreePath[ConstraintES]
  type N = SymbolicNode[ConstraintES]
  type NwC = SymbolicNodeWithConstraint[ConstraintES]

  type WorkListItem = (N, TPath, TPath => TPath)

  protected def handleConstraint(
    node: N,
    constraint: ConstraintES,
    path: TPath,
    finishPath: TPath => TPath
  ): Option[PathAndNode[ConstraintES]] = {
    constraint match {
      case EventConstraintWithExecutionState(_, executionState) if executionState == toFind =>
        val tuple = (finishPath(path.copy()), node)
//        workSet += tuple
        Some(PathAndNode(???, ???, tuple._1, tuple._2))
      case _ => None
    }
  }
}

abstract class ConstraintESWorkListFinder[T](toFind: ExecutionState)
  extends BaseConstraintESWorkListFinder[T](toFind) {

  /**
    * Should the algorithm keep descending into a subtree if the node at the root of this subtree features the required execution state?
    *
    * @return
    */
  def continueAfterNodeFound: Boolean

  def findWorkList(root: SymbolicNode[ConstraintES])
  : Set[PathAndNode[ConstraintES]] = {
    val workSet = MSet[PathAndNode[ConstraintES]]()

    @tailrec
    def exploreTree(nodeWorkList: List[WorkListItem]): Unit = {
      nodeWorkList.headOption match {
        case None =>
        case Some(info) =>
          info match {
            case (bsn@BranchSymbolicNode(constraint, trueBranch, falseBranch),
            path,
            finishPath) =>
              val nodeFound = handleConstraint(bsn, constraint, path, finishPath)
              nodeFound.foreach(workSet += _)
              if (!continueAfterNodeFound && nodeFound.isDefined) {
                exploreTree(nodeWorkList.tail)
              } else {
                val tbEnqueued = trueBranch.to match {
                  case trueBranchwC: NwC =>
                    val item: WorkListItem = (trueBranchwC,
                      path.addThenBranch(trueBranchwC, trueBranch),
                      _.finishedByDroppingLastConstraint)
                    item :: nodeWorkList.tail
                  case _ => nodeWorkList.tail
                }
                val fbEnqueued = falseBranch.to match {
                  case falseBranchwC: NwC =>
                    val item: WorkListItem = (falseBranchwC,
                      path.addElseBranch(falseBranchwC, falseBranch),
                      _.finishedByDroppingLastConstraint)
                    item :: tbEnqueued
                  case _ => tbEnqueued
                }
                exploreTree(fbEnqueued)
              }
            case _ =>
              exploreTree(nodeWorkList.tail)
          }
      }
    }

    val initialPath: TPath = TreePath.init(root)._1
    val initialItem: WorkListItem = (root, initialPath, _.finishedByDroppingLastConstraint)
    exploreTree(List(initialItem))
    workSet.toSet
  }
}

class ConstraintESWorkListFinderWithDescend(toFind: ExecutionState)
  extends ConstraintESWorkListFinder(toFind) {
  override def continueAfterNodeFound = true
}

class ConstraintESWorkListFinderWithoutDescend(toFind: ExecutionState)
  extends ConstraintESWorkListFinder(toFind) {
  override def continueAfterNodeFound = false
}

/**
  * @param pathAdded The (init of) the path that was added to the tree
  * @param toFind The ExecutionState to find
  * @param endNode The last node of the path that was added to the tree (different from the last node of `pathAdded`
  *                since the WorkListFinder only receives the init of the added path)
  * @tparam T Type of the extra information carried by a `SymbolicNode`
  */
class ConstraintESWorkListFinderFollowPath[T](
  val pathAdded: TreePath[ConstraintES],
  toFind: ExecutionState,
  optEndNode: Option[SymbolicNode[ConstraintES]]
)
  extends BaseConstraintESWorkListFinder[T](toFind) {

  protected def isGlobalMergePoint(symbolicNode: SymbolicNodeWithConstraint[ConstraintES]): Boolean = {
    symbolicNode.constraint.executionState match {
      case _: TargetEndExecutionState => true
      case _ => false
    }
  }

  protected def mergeAllowed(candidate: SymbolicNode[ConstraintES]): Boolean = {
    ! pathAdded.original.contains(candidate)
  }


  protected def findSiblingNodesAlongPath(
    root: SymbolicNode[ConstraintES],
    pathToFollow: TreePath[ConstraintES]
  ): List[PathAndNode[ConstraintES]] = {
    @tailrec
    def loop(
      lastGlobalMergePoint: SymbolicNodeWithConstraint[ConstraintES],
      currentNode: SymbolicNode[ConstraintES],
      pathToFollow: TreePath[ConstraintES],
      currentPath: TreePath[ConstraintES],
      completeCurrentPath: TreePath[ConstraintES],
      siblings: List[PathAndNode[ConstraintES]]
    ): List[PathAndNode[ConstraintES]] = pathToFollow.getPath.headOption match {
      case None => siblings
      case Some(dir) => currentNode match {
        case bsn: BranchSymbolicNode[ConstraintES] =>
          val bsnIsGlobalMergePoint = bsn.constraint.executionState.stackLength == 0 && (bsn.hasBeenMerged || bsn == root)
          val newGlobalMergePoint = if (bsnIsGlobalMergePoint) bsn else lastGlobalMergePoint
          val newTreePath = if (bsnIsGlobalMergePoint) TreePath.init(bsn)._1 else currentPath

          val optSibling: Option[PathAndNode[ConstraintES]] = dir match {
            case ElseDirection => bsn.thenBranch.to match {
              case thenSibling: SymbolicNodeWithConstraint[ConstraintES] =>
                Some(PathAndNode(
                  completeCurrentPath.finished, lastGlobalMergePoint, newTreePath.addThenBranch(thenSibling, bsn.thenBranch),
                  thenSibling))
              case _ => None
            }
            case ThenDirection => bsn.elseBranch.to match {
              case elseSibling: SymbolicNodeWithConstraint[ConstraintES] =>
                Some(PathAndNode(
                  completeCurrentPath.finished, lastGlobalMergePoint, newTreePath.addElseBranch(elseSibling, bsn.elseBranch),
                  elseSibling))
              case _ => None
            }
          }
          val newSiblings = optSibling match {
            case Some(sibling) => sibling :: siblings
            case None => siblings
          }

          val (newCurrentPath, newCompleteCurrentPath): (TreePath[ConstraintES], TreePath[ConstraintES]) = dir match {
            case ElseDirection =>
              bsn.elseBranch.to match {
                case nwc: SymbolicNodeWithConstraint[ConstraintES] =>
                  (newTreePath.addElseBranch(nwc, bsn.elseBranch), completeCurrentPath.addElseBranch(
                    nwc, bsn.elseBranch))
                case _ =>
                  (newTreePath.finishedViaElse, completeCurrentPath.finishedViaElse)
              }
            case ThenDirection =>
              bsn.thenBranch.to match {
                case nwc: SymbolicNodeWithConstraint[ConstraintES] =>
                  (newTreePath.addThenBranch(nwc, bsn.thenBranch), completeCurrentPath.addThenBranch(
                    nwc, bsn.thenBranch))
                case _ =>
                  (newTreePath.finishedViaThen, completeCurrentPath.finishedViaThen)
              }
          }

          val newEdge = dir.getTarget(bsn)
          loop(newGlobalMergePoint, newEdge.to, pathToFollow.tail, newCurrentPath, newCompleteCurrentPath, newSiblings)
        case other => throw new Exception(s"Should not happen: got the non-branch node $other while traversing path")
      }
    }
    pathToFollow.original.headOption match {
      case None => Nil
      case Some(headPath) =>
        loop(headPath, root, pathToFollow, TreePath.init(root)._1, TreePath.init(root)._1, Nil)
    }
  }

  protected def pathIsRegistered(path: TreePath[ConstraintES]): Boolean = {
    CentralMergedPathRegistry.containsPath(toFind, path.getPath)
  }

  type TreePathAndNode = (SymbolicNode[ConstraintES], TreePath[ConstraintES])

  private var bsnsEncountered: Set[SymbolicNode[ConstraintES]] = Set()

  /**
    * Starts from a sibling node of one of the nodes that has just been added and tries to find a node with
    * an execution state equal to `toFind`
    * @param pathToParent
    * @param parentNode
    * @param workList
    * @return
    */
  @tailrec
  private def tryOnlyRegisteredPaths(
    pathToParent: TreePath[ConstraintES],
    parentNode: SymbolicNodeWithConstraint[ConstraintES],
    workList: List[TreePathAndNode]
  ): Option[PathAndNode[ConstraintES]] = workList.headOption match {
    case None => None
    case Some((node, pathViaSibling)) =>
      if (bsnsEncountered.contains(node)) {
        tryOnlyRegisteredPaths(pathToParent, parentNode, workList.tail)
      } else {
        bsnsEncountered += node
        node match {
          case nwc: NwC
            if nwc.constraint.executionState == toFind && (!optEndNode.contains(nwc)) &&
               pathIsRegistered(pathViaSibling) && mergeAllowed(node) =>
            val finishedTreePath = pathViaSibling.finished
            Some(PathAndNode(pathToParent, parentNode, finishedTreePath, node))
          case bsn: BranchSymbolicNode[ConstraintES] =>
            val thenChild: List[TreePathAndNode] = bsn.thenBranch.to match {
              case nwC: NwC => List((nwC, pathViaSibling.addThenBranch(nwC, bsn.thenBranch)))
              case _ => Nil
            }
            val elseChild: List[TreePathAndNode] = bsn.elseBranch.to match {
              case nwC: NwC => List((nwC, pathViaSibling.addElseBranch(nwC, bsn.elseBranch)))
              case _ => Nil
            }
            val newWorklist = thenChild ++ elseChild ++ workList.tail
            tryOnlyRegisteredPaths(pathToParent, parentNode, newWorklist)
        }
      }
  }

  @tailrec
  private def tryOneUnregisteredSibling(
    pathToLastGlobalMergePoint: TreePath[ConstraintES],
    lastGlobalMergePoint: SymbolicNodeWithConstraint[ConstraintES],
    workList: List[TreePathAndNode]
  ): Option[PathAndNode[ConstraintES]] = workList.headOption match {
    case None => None
    case Some((sibling, pathViaSibling)) =>
      if (bsnsEncountered.contains(sibling)) {
        tryOneUnregisteredSibling(pathToLastGlobalMergePoint, lastGlobalMergePoint, workList.tail)
      } else {
        bsnsEncountered += sibling
        sibling match {
          case node: NwC
            if node.constraint.executionState == toFind && (!optEndNode.contains(node)) && mergeAllowed(node) =>
            val finishedTreePath = pathViaSibling.finished
            Some(PathAndNode(pathToLastGlobalMergePoint, lastGlobalMergePoint, finishedTreePath, sibling))
          case bsn: BranchSymbolicNode[ConstraintES] =>
            val thenChild: List[TreePathAndNode] = bsn.thenBranch.to match {
              case nwC: NwC =>
                List((nwC, pathViaSibling.addThenBranch(nwC, bsn.thenBranch)))
              case _ => Nil
            }
            val elseChild: List[TreePathAndNode] = bsn.elseBranch.to match {
              case nwC: NwC =>
                List((nwC, pathViaSibling.addElseBranch(nwC, bsn.elseBranch)))
              case _ => Nil
            }
            val newWorkList = thenChild ++ elseChild ++ workList.tail
            tryOneUnregisteredSibling(pathToLastGlobalMergePoint, lastGlobalMergePoint, newWorkList)
          case _ => None
        }
      }
  }

  override def findWorkList(
    root: SymbolicNode[ConstraintES]
  ): Set[PathAndNode[ConstraintES]] = {
    val siblingsAlongPath = findSiblingNodesAlongPath(root, pathAdded)
    @tailrec
    def loopRegisteredPaths(siblings: List[PathAndNode[ConstraintES]]): Option[PathAndNode[ConstraintES]] =
      siblings.headOption match {
        case None => None
        case Some(sibling) =>
          bsnsEncountered = Set()
          val initialWorklist = List((sibling.lastNode, sibling.treePath))
          val optRegisteredPathAndNode = tryOnlyRegisteredPaths(
            sibling.pathToGlobalMergePoint, sibling.lastGlobalMergePoint, initialWorklist)
          bsnsEncountered = Set()
          optRegisteredPathAndNode match {
            case Some(pathAndNode) => Some(pathAndNode)
            case None => loopRegisteredPaths(siblings.tail)
          }
      }
    @tailrec
    def loopUnregisteredPaths(siblings: List[PathAndNode[ConstraintES]]): Option[PathAndNode[ConstraintES]] =
      siblings.headOption match {
        case None => None
        case Some(sibling) =>
          bsnsEncountered = Set()
          val initialWorklist = List((sibling.lastNode, sibling.treePath))
          val optPathAndNode = tryOneUnregisteredSibling(
            sibling.pathToGlobalMergePoint, sibling.lastGlobalMergePoint, initialWorklist)
          bsnsEncountered = Set()
          optPathAndNode match {
            case Some(pathAndNode) => Some(pathAndNode)
            case None => loopUnregisteredPaths(siblings.tail)
          }
      }
    val registeredPaths = loopRegisteredPaths(siblingsAlongPath)
    if (registeredPaths.nonEmpty) {
      registeredPaths.toSet
    } else {
      val result = loopUnregisteredPaths(siblingsAlongPath).toSet
      result
    }
  }

}
