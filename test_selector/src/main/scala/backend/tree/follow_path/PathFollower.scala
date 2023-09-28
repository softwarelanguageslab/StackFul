package backend.tree.follow_path

import backend.InvalidPathException
import backend.execution_state._
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, EventConstraintWithExecutionState}
import backend.tree.constraints.event_constraints._
import backend.tree.path.SymJSState
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec

trait Direction {
  def negate: Direction
  /**
    * Retrieves the target child-edge of `node` that matches this object's Direction type.
    * Throws an `InvalidPathException` if `node` does not have
    * a child-edge matching this object's Direction type.
    *
    * @param node The node to retrieve the target child-edge from.
    * @return The target child-edge of the node.
    */
  def getTarget[C <: Constraint](node: SymbolicNode[C]): Edge[C]
  def toEdgeWithoutTo[C <: Constraint]: EdgeWithoutTo[C]
}
trait BinaryDirection extends Direction
case object ElseDirection extends BinaryDirection {
  def negate: BinaryDirection = ThenDirection
  def getTarget[C <: Constraint](bsn: SymbolicNode[C]): ElseEdge[C] = {
    if(!bsn.isInstanceOf[BranchSymbolicNode[C]]) {
      throw InvalidPathException(List(ElseDirection))
    }
    bsn.asInstanceOf[BranchSymbolicNode[C]].elseBranch
  }
  def toEdgeWithoutTo[C <: Constraint]: EdgeWithoutTo[C] = ElseEdgeWithoutTo(Nil)
}
case object ThenDirection extends BinaryDirection {
  def negate: BinaryDirection = ElseDirection
  def getTarget[C <: Constraint](bsn: SymbolicNode[C]): ThenEdge[C] = {
    if(!bsn.isInstanceOf[BranchSymbolicNode[C]]) {
      throw InvalidPathException(List(ThenDirection))
    }
    bsn.asInstanceOf[BranchSymbolicNode[C]].thenBranch
  }
  def toEdgeWithoutTo[C <: Constraint]: EdgeWithoutTo[C] = ThenEdgeWithoutTo(Nil)
}

case class TargetTriggered(processId: Int, targetId: Int)

case class PathFollowed[C <: Constraint](
  endNode: SymbolicNode[C],
  store: SymbolicStore,
  path: TreePath[C]
)
case class PathFollowedWithAssignments[C <: Constraint](
  pathFollowed: PathFollowed[C],
  identifiersAssigned: Set[String]
)

trait PathFollower[C <: Constraint] {

  //  protected val constraintHasStoreUpdates: HasStoreUpdates[C] = implicitly
  protected val elseEdgeHasStoreUpdates: HasStoreUpdates[ElseEdge[C]] = new ElseEdgeHasStoreUpdates
  protected val thenEdgeHasStoreUpdates: HasStoreUpdates[ThenEdge[C]] = new ThenEdgeHasStoreUpdates
  def followPathAndArriveAt(
    symbolicNode: SymbolicNode[C],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[C],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowed[C]] = {
    followPathWithAssignments(symbolicNode, symbolicStore, selectedState, addLastIdentifiersAssigned).map(_.pathFollowed)
  }
  def followPath(
    symbolicNode: SymbolicNode[C],
    selectedState: SymJSState[C],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[TreePath[C]] = {
    followPathWithAssignments(symbolicNode, emptyStore, selectedState, addLastIdentifiersAssigned).map(_.pathFollowed.path)
  }
  def followPathWithStore(
    symbolicNode: SymbolicNode[C],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[C],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[TreePath[C]] = {
    followPathWithAssignments(symbolicNode, symbolicStore, selectedState, addLastIdentifiersAssigned).map(_.pathFollowed.path)
  }
  def followPathWithAssignments(
    symbolicNode: SymbolicNode[C],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[C],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowedWithAssignments[C]]
  trait WhatToDo {
    protected type Node = SymbolicNodeWithConstraint[C]
    protected type TP = TreePath[C]
    def finished(treePath: TP): TP
    def add(optTreePath: Option[TP], node: Node)(implicit negater: ConstraintNegater[C]): TP
  }
  case class SetElse(elseEdge: ElseEdge[C]) extends WhatToDo {
    def finished(treePath: TP): TP = treePath.finishedViaElse
    def add(optTreePath: Option[TP], node: Node)
      (implicit negater: ConstraintNegater[C]): TP = optTreePath.get.addElseBranch(node, elseEdge)
  }
  case class SetThen(thenEdge: ThenEdge[C]) extends WhatToDo {
    def finished(treePath: TP): TP = treePath.finishedViaThen
    def add(optTreePath: Option[TP], node: Node)
      (implicit negater: ConstraintNegater[C]): TP = optTreePath.get.addThenBranch(node, thenEdge)
  }
  case object StartFrom extends WhatToDo {
    def finished(treePath: TP): TP = treePath.finished
    def add(optTreePath: Option[TP], node: Node)(implicit negater: ConstraintNegater[C]): TP = optTreePath match {
      case None => TreePath.init[C](node)._1
      case Some(_) => throw new Exception("Should not happen")
    }
  }

}

class ConstraintESNoEventsPathFollower[C <: Constraint : HasStoreUpdates]
  (implicit val negater: ConstraintNegater[C])
  extends PathFollower[C] {

  private val hasStoreUpdates: HasStoreUpdates[C] = implicitly[HasStoreUpdates[C]]

  def followPathWithAssignments(
    symbolicNode: SymbolicNode[C],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[C],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowedWithAssignments[C]] = {
    @tailrec
    def loop(
      symbolicNode: SymbolicNode[C],
      symbolicStore: SymbolicStore,
      currentState: SymJSState[C],
      optTreePath: Option[TreePath[C]],
      identifiersAssigned: Set[String],
      skipFirstNode: Boolean,
      toDo: WhatToDo
    ): Option[PathFollowedWithAssignments[C]] = {
      if (currentState.isEmpty) {
        val treePath = optTreePath.getOrElse(TreePath.empty[C])
        val updatedStore = symbolicNode match {
          case node: SymbolicNodeWithConstraint[C] => hasStoreUpdates.applyToStore(
            node.constraint, symbolicStore)
          case _ => symbolicStore
        }
        val updatedIdentifiersAssigned = if (addLastIdentifiersAssigned) {
          symbolicNode match {
            case node: SymbolicNodeWithConstraint[C] =>
              if (skipFirstNode) {
                identifiersAssigned
              } else {
                identifiersAssigned ++ hasStoreUpdates.assignsIdentifiers(node.constraint)
              }
            case _ => identifiersAssigned
          }
        } else {
          identifiersAssigned
        }
        Some(PathFollowedWithAssignments(
          PathFollowed(symbolicNode, updatedStore, toDo.finished(treePath)), updatedIdentifiersAssigned))
      } else {
        symbolicNode match {
          //          case bsn @ BranchSymbolicNode(EventConstraintWithExecutionState(_: BranchConstraint, _, _), thenBranch, elseBranch, _) =>
          case bsn@BranchSymbolicNode(c, thenBranch, elseBranch) =>
            val updatedStore = hasStoreUpdates.applyToStore(c, symbolicStore)
            val identifiersAssignedWithConstraint = if (skipFirstNode) identifiersAssigned else identifiersAssigned ++ hasStoreUpdates.assignsIdentifiers(
              c)
            currentState.headBranch.map(_.toDirection) match {
              case Some(ElseDirection) =>
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                  elseBranch)
                val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(elseBranch, updatedStore)
                loop(
                  elseBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                  identifiersAssignedWithEdge, false, SetElse(elseBranch))
              case Some(ThenDirection) =>
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                  thenBranch)
                val storeUpdatedWithEdge = thenEdgeHasStoreUpdates.applyToStore(thenBranch, updatedStore)
                loop(
                  thenBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                  identifiersAssignedWithEdge, false, SetThen(thenBranch))
              case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
            }
          case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
            if (currentState.branchSequenceEmpty) {
              Some(PathFollowedWithAssignments(
                PathFollowed(symbolicNode, symbolicStore, toDo.finished(optTreePath.get)), identifiersAssigned))
            } else {
              None
            }
        }
      }
    }
    loop(symbolicNode, symbolicStore, selectedState, None, Set(), true, StartFrom)
  }
}

// TODO refactor!
class ConstraintESNoEventsPathFollowerSkipStopTestingTargets[T](
  val skipLastEdge: Boolean = false
)(implicit val negater: ConstraintNegater[ConstraintES])
  extends PathFollower[ConstraintES] {

  private val constraintESHasStoreUpdates: HasStoreUpdates[ConstraintES] = implicitly[HasStoreUpdates[ConstraintES]]

  def followPathWithAssignments(
    symbolicNode: SymbolicNode[ConstraintES],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[ConstraintES],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowedWithAssignments[ConstraintES]] = {
    @tailrec
    def loop(
      symbolicNode: SymbolicNode[ConstraintES],
      symbolicStore: SymbolicStore,
      currentState: SymJSState[ConstraintES],
      optTreePath: Option[TreePath[ConstraintES]],
      identifiersAssigned: Set[String],
      skipFirstNode: Boolean,
      toDo: WhatToDo
    ): Option[PathFollowedWithAssignments[ConstraintES]] = {
      if (currentState.isEmpty) {
        val treePath = optTreePath.getOrElse(TreePath.empty[ConstraintES])
        val updatedStore = if (skipLastEdge) {
          symbolicStore
        } else {
          symbolicNode match {
            case node: SymbolicNodeWithConstraint[ConstraintES] => constraintESHasStoreUpdates.applyToStore(
              node.constraint, symbolicStore)
            case _ => symbolicStore
          }
        }
        val updatedIdentifiersAssigned = if (skipLastEdge) {
          identifiersAssigned
        } else {
          symbolicNode match {
            case node: SymbolicNodeWithConstraint[ConstraintES] => if (skipFirstNode) identifiersAssigned else identifiersAssigned ++ constraintESHasStoreUpdates.assignsIdentifiers(
              node.constraint)
            case _ => identifiersAssigned
          }
        }
        Some(PathFollowedWithAssignments(
          PathFollowed(symbolicNode, updatedStore, toDo.finished(treePath)), updatedIdentifiersAssigned))
      } else {
        symbolicNode match {
          //          case bsn @ BranchSymbolicNode(EventConstraintWithExecutionState(_: BranchConstraint, _, _), thenBranch, elseBranch, _) =>
          case bsn@BranchSymbolicNode(c, thenBranch, elseBranch) =>
            val updatedStore = constraintESHasStoreUpdates.applyToStore(c, symbolicStore)
            val identifiersAssignedWithConstraint = if (skipFirstNode) identifiersAssigned else identifiersAssigned ++ constraintESHasStoreUpdates.assignsIdentifiers(
              c)
            val newTreePath: Option[TreePath[ConstraintES]] = c match {
              case EventConstraintWithExecutionState(_: StopTestingTargets, _) => optTreePath
              case _ => Some(toDo.add(optTreePath, bsn)(negater)) // do not add new StopTestingTarget constraints to the tree path
            }
            currentState.headBranch.map(_.toDirection) match {
              case Some(ElseDirection) =>
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                  elseBranch)
                val storeUpdatedWithEdge = if (skipLastEdge && currentState.dropBranch.branchSequenceEmpty) {
                  updatedStore
                } else elseEdgeHasStoreUpdates.applyToStore(elseBranch, updatedStore)
                val newToDo = c match {
                  case EventConstraintWithExecutionState(_: StopTestingTargets, _) => toDo
                  case _ => SetElse(elseBranch)
                }
                loop(
                  elseBranch.to, storeUpdatedWithEdge, currentState.dropBranch, newTreePath,
                  identifiersAssignedWithEdge, false, newToDo)
              case Some(ThenDirection) =>
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                  thenBranch)
                val storeUpdatedWithEdge = if (skipLastEdge && currentState.dropBranch.branchSequenceEmpty) {
                  updatedStore
                } else thenEdgeHasStoreUpdates.applyToStore(thenBranch, updatedStore)
                val newToDo = c match {
                  case EventConstraintWithExecutionState(_: StopTestingTargets, _) => toDo
                  case _ => SetThen(thenBranch)
                }
                loop(
                  thenBranch.to, storeUpdatedWithEdge, currentState.dropBranch, newTreePath,
                  identifiersAssignedWithEdge, false, newToDo)
              case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
            }
          case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
            if (currentState.branchSequenceEmpty) {
              Some(PathFollowedWithAssignments(
                PathFollowed(symbolicNode, symbolicStore, toDo.finished(optTreePath.get)), identifiersAssigned))
            } else {
              None
            }
        }
      }
    }
    loop(symbolicNode, symbolicStore, selectedState, None, Set(), true, StartFrom)
  }
}

class ConstraintESPathFollower[T]
  (implicit val negater: ConstraintNegater[ConstraintES]) extends PathFollower[ConstraintES] {
  val basicConstraintPathFollower = new ConstraintESNoEventsPathFollower[ConstraintES]
  private val constraintESHasStoreUpdates: HasStoreUpdates[ConstraintES] = implicitly[HasStoreUpdates[ConstraintES]]

  def followPathWithAssignments(
    symbolicNode: SymbolicNode[ConstraintES],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[ConstraintES],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowedWithAssignments[ConstraintES]] = {
    @tailrec
    def loop(
      symbolicNode: SymbolicNode[ConstraintES],
      symbolicStore: SymbolicStore,
      currentState: SymJSState[ConstraintES],
      optTreePath: Option[TreePath[ConstraintES]],
      identifiersAssigned: Set[String],
      toDo: WhatToDo
    ): Option[PathFollowedWithAssignments[ConstraintES]] = {
      if (currentState.isEmpty) {
        val treePath = optTreePath.getOrElse(TreePath.empty[ConstraintES])
        val updatedStore = symbolicNode match {
          case node: SymbolicNodeWithConstraint[ConstraintES] => constraintESHasStoreUpdates.applyToStore(
            node.constraint, symbolicStore)
          case _ => symbolicStore
        }
        Some(PathFollowedWithAssignments(
          PathFollowed(symbolicNode, updatedStore, toDo.finished(treePath)), identifiersAssigned))
      } else {
        symbolicNode match {
          case bsn: BranchSymbolicNode[ConstraintES] =>
            val updatedStore = constraintESHasStoreUpdates.applyToStore(bsn.constraint, symbolicStore)
            val identifiersAssignedWithConstraint = identifiersAssigned ++ constraintESHasStoreUpdates.assignsIdentifiers(
              bsn.constraint)
            bsn match {
              case BranchSymbolicNode(
              EventConstraintWithExecutionState(_: BranchConstraint, _), thenBranch, elseBranch) =>
                currentState.headBranch.map(_.toDirection) match {
                  case Some(ElseDirection) =>
                    val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                      elseBranch)
                    val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(elseBranch, updatedStore)
                    loop(
                      elseBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                      identifiersAssignedWithEdge, SetElse(elseBranch))
                  case Some(ThenDirection) =>
                    val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                      thenBranch)
                    val storeUpdatedWithEdge = thenEdgeHasStoreUpdates.applyToStore(thenBranch, updatedStore)
                    loop(
                      thenBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                      identifiersAssignedWithEdge, SetThen(thenBranch))
                  case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
                }
              case BranchSymbolicNode(EventConstraintWithExecutionState(_: TargetChosen, _), _, _) =>
                currentState.headEvent match {
                  case Some(targetTriggered) =>
                    // Pass identifiersAssigned instead of identifiersAssignedWithConstraint here because findTargetTriggeredNode
                    // itself also adds the identifiers in the constraint's store updates if the node is a BSN
                    val (nextNode, nextStore, nextIdentifiersAssigned) = findTargetTriggeredNode(
                      bsn, updatedStore, targetTriggered, identifiersAssigned)
                    loop(nextNode, nextStore, currentState.dropEvent, optTreePath, nextIdentifiersAssigned, toDo)
                  case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
                }

              case BranchSymbolicNode(EventConstraintWithExecutionState(_: StopTestingTargets, _), _, elseBranch) =>
                val nextNode = elseBranch // if (selectedState.eventSequenceEmpty) thenBranch else elseBranch
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                  elseBranch)
                val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(nextNode, updatedStore)
                loop(nextNode.to, storeUpdatedWithEdge, currentState, optTreePath, identifiersAssignedWithEdge, toDo)
            }
          case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
            if (currentState.branchSequenceEmpty) {
              Some(PathFollowedWithAssignments(
                PathFollowed(symbolicNode, symbolicStore, toDo.finished(optTreePath.get)), identifiersAssigned))
            } else {
              None
            }
        }
      }
    }
    loop(symbolicNode, symbolicStore, selectedState, None, Set(), StartFrom)
  }

  @tailrec
  private def findTargetTriggeredNode(
    node: SymbolicNode[ConstraintES],
    symbolicStore: SymbolicStore,
    target: TargetTriggered,
    identifiersAssigned: Set[String]
  ): (SymbolicNode[ConstraintES], SymbolicStore, Set[String]) =
    node match {
      case bsn: BranchSymbolicNode[ConstraintES] =>
        val identifiersAssignedWithConstraint = constraintESHasStoreUpdates.assignsIdentifiers(bsn.constraint)
        bsn.constraint match {
          case _: StopTestingTargets | _: BranchConstraint =>
            throw new Exception(s"Unexpected constraint found: ${bsn.constraint}")
          case tc: TargetChosen =>
            if (tc.processIdChosen == target.processId && tc.targetIdChosen == target.targetId) {
              val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                bsn.thenBranch)
              val storeUpdatedWithEdge = thenEdgeHasStoreUpdates.applyToStore(bsn.thenBranch, symbolicStore)
              (bsn.thenBranch.to, storeUpdatedWithEdge, identifiersAssignedWithEdge)
            } else {
              val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                bsn.elseBranch)
              val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(bsn.elseBranch, symbolicStore)
              findTargetTriggeredNode(bsn.elseBranch.to, storeUpdatedWithEdge, target, identifiersAssignedWithEdge)
            }
        }
      case UnexploredNode() => (node, symbolicStore, identifiersAssigned)
      case _ =>
        throw new Exception(s"End of path reached before appropriate TargetChosen node was found: $node, $target")
    }
}

class EventConstraintPathFollower(implicit val negater: ConstraintNegater[EventConstraint])
  extends PathFollower[EventConstraint] {

  private val eventConstraintHasStoreUpdates: HasStoreUpdates[EventConstraint] = implicitly[HasStoreUpdates[EventConstraint]]

  def followPathWithAssignments(
    symbolicNode: SymbolicNode[EventConstraint],
    symbolicStore: SymbolicStore,
    selectedState: SymJSState[EventConstraint],
    addLastIdentifiersAssigned: Boolean = true
  ): Option[PathFollowedWithAssignments[EventConstraint]] = {
    @tailrec
    def loop(
      symbolicNode: SymbolicNode[EventConstraint],
      symbolicStore: SymbolicStore,
      currentState: SymJSState[EventConstraint],
      optTreePath: Option[TreePath[EventConstraint]],
      identifiersAssigned: Set[String],
      toDo: WhatToDo
    ): Option[PathFollowedWithAssignments[EventConstraint]] = {

      if (currentState.isEmpty) {
        val treePath = optTreePath.getOrElse(TreePath.empty[EventConstraint])
        val updatedStore = symbolicNode match {
          case node: SymbolicNodeWithConstraint[EventConstraint] => eventConstraintHasStoreUpdates.applyToStore(
            node.constraint, symbolicStore)
          case _ => symbolicStore
        }
        Some(PathFollowedWithAssignments(
          PathFollowed(symbolicNode, updatedStore, toDo.finished(treePath)), identifiersAssigned))
      } else {
        symbolicNode match {
          case bsn: BranchSymbolicNode[EventConstraint] =>
            val updatedStore = eventConstraintHasStoreUpdates.applyToStore(bsn.constraint, symbolicStore)
            val identifiersAssignedWithConstraint = identifiersAssigned ++ eventConstraintHasStoreUpdates.assignsIdentifiers(
              bsn.constraint)
            bsn match {
              case BranchSymbolicNode(_: BranchConstraint, thenBranch, elseBranch) =>
                currentState.headBranch.map(_.toDirection) match {
                  case Some(ElseDirection) =>
                    val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                      elseBranch)
                    val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(elseBranch, updatedStore)
                    loop(
                      elseBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                      identifiersAssignedWithEdge, SetElse(elseBranch))
                  case Some(ThenDirection) =>
                    val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                      thenBranch)
                    val storeUpdatedWithEdge = thenEdgeHasStoreUpdates.applyToStore(thenBranch, updatedStore)
                    loop(
                      thenBranch.to, storeUpdatedWithEdge, currentState.dropBranch, Some(toDo.add(optTreePath, bsn)),
                      identifiersAssignedWithEdge, SetThen(thenBranch))
                  case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
                }
              case BranchSymbolicNode(_: TargetChosen, _, _) =>
                currentState.headEvent match {
                  case Some(targetTriggered) =>
                    // Pass identifiersAssigned instead of identifiersAssignedWithConstraint here because findTargetTriggeredNode
                    // itself also adds the identifiers in the constraint's store updates if the node is a BSN
                    val (nextNode, nextStore, nextIdentifiersAssigned) = findTargetTriggeredNode(
                      bsn, updatedStore, targetTriggered, identifiersAssigned)
                    //              val nextNode = maybeStopTestingTargets match {
                    //                case BranchSymbolicNode(_: StopTestingTargets, _, elseBranch, _) => elseBranch
                    //                case _ => maybeStopTestingTargets
                    //              }
                    loop(nextNode, nextStore, currentState.dropEvent, optTreePath, nextIdentifiersAssigned, toDo)
                  case _ => throw new Exception(s"Unexpected end of path reached: $bsn")
                }
              case BranchSymbolicNode(StopTestingTargets(_, _, _, _), _, elseBranch) =>
                val nextNode = elseBranch // if (selectedState.eventSequenceEmpty) thenBranch else elseBranch
                val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(elseBranch, updatedStore)
                val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                  elseBranch)
                loop(nextNode.to, storeUpdatedWithEdge, currentState, optTreePath, identifiersAssignedWithEdge, toDo)
            }
          case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
            if (currentState.branchSequenceEmpty) {
              Some(PathFollowedWithAssignments(
                PathFollowed(symbolicNode, symbolicStore, toDo.finished(optTreePath.get)), identifiersAssigned))
            } else {
              None
            }
        }
      }
    }
    loop(symbolicNode, symbolicStore, selectedState, None, Set(), StartFrom)
  }

  @tailrec
  private def findTargetTriggeredNode(
    node: SymbolicNode[EventConstraint],
    symbolicStore: SymbolicStore,
    target: TargetTriggered,
    identifiersAssigned: Set[String]
  ): (SymbolicNode[EventConstraint], SymbolicStore, Set[String]) =
    node match {
      case bsn: BranchSymbolicNode[EventConstraint] =>
        val identifiersAssignedWithConstraint = eventConstraintHasStoreUpdates.assignsIdentifiers(bsn.constraint)
        bsn.constraint match {
          case _: StopTestingTargets | _: BranchConstraint =>
            throw new Exception(s"Unexpected constraint found: ${bsn.constraint}")
          case tc: TargetChosen =>
            if (tc.processIdChosen == target.processId && tc.targetIdChosen == target.targetId) {
              val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ thenEdgeHasStoreUpdates.assignsIdentifiers(
                bsn.thenBranch)
              val storeUpdatedWithEdge = thenEdgeHasStoreUpdates.applyToStore(bsn.thenBranch, symbolicStore)
              (bsn.thenBranch.to, storeUpdatedWithEdge, identifiersAssignedWithEdge)
            } else {
              val identifiersAssignedWithEdge = identifiersAssignedWithConstraint ++ elseEdgeHasStoreUpdates.assignsIdentifiers(
                bsn.elseBranch)
              val storeUpdatedWithEdge = elseEdgeHasStoreUpdates.applyToStore(bsn.elseBranch, symbolicStore)
              findTargetTriggeredNode(bsn.elseBranch.to, storeUpdatedWithEdge, target, identifiersAssignedWithEdge)
            }
        }
      case UnexploredNode() => (node, symbolicStore, identifiersAssigned)
      case _ =>
        throw new Exception(s"End of path reached before appropriate TargetChosen node was found: $node, $target")
    }
}
