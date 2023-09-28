package backend.reporters

import backend.execution_state.store.StoreUpdate
import backend.expression.IdGenerator
import backend.modes._
import backend.tree._
import backend.tree.constraints.{EventConstraint, _}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state.EventConstraintWithExecutionState
import backend.tree.constraints.event_constraints._
import backend.tree.follow_path._
import backend.tree.search_strategy.TreePath
import backend.{PCElement, PathConstraint, PathConstraintWith, RegularPCElement}

import scala.annotation.tailrec

trait Reporter[PathConstraintUsed, C <: Constraint] {

  def getRoot: Option[SymbolicNode[C]]
  def deleteSymbolicTree(): Unit
  def clear(): Unit
  def addExploredPath(constraints: PathConstraintUsed, finishedTestRun: Boolean): TreePath[C]
  def mergePath(currentConstraints: PathConstraintUsed): Unit

  def writeSymbolicTree(path: String): Unit
  def setRoot(newRoot: SymbolicNode[C]): Unit
}

sealed trait SetChild[C <: Constraint] {
  def existingEdge: Option[Edge[C]]
  def existingChild: SymbolicNode[C]
  def setChild(child: SymbolicNode[C]): Unit
  def setChild(child: SymbolicNode[C], storeUpdates: Iterable[StoreUpdate]): Unit
}

object SetChild {
  def apply[C <: Constraint](
    parent: SymbolicNode[C],
    direction: Direction
  ): SetChild[C] = parent match {
    case p: BranchSymbolicNode[C] => direction match {
      case ElseDirection => SetChildElseBranch(p)
      case ThenDirection => SetChildThenBranch(p)
      case _ => throw new Exception(s"Invalid direction $direction for parent $p")
    }
    case _ => throw new Exception(s"Invalid parent $parent")
  }
}

case class SetChildElseBranch[C <: Constraint](parent: BranchSymbolicNode[C])
  extends SetChild[C] {
  def existingEdge: Option[Edge[C]] = Some(parent.elseBranch)
  def setChild(child: SymbolicNode[C]): Unit = {
    setChild(child, parent.elseBranch.storeUpdates)
  }
  def setChild(child: SymbolicNode[C], storeUpdates: Iterable[StoreUpdate]): Unit = {
    if (existingChild != child)
      parent.setElseBranch(ElseEdge(storeUpdates, child))
  }
  def existingChild: SymbolicNode[C] = parent.elseBranch.to
}

case class SetChildThenBranch[C <: Constraint](parent: BranchSymbolicNode[C])
  extends SetChild[C] {
  def existingEdge: Option[Edge[C]] = Some(parent.thenBranch)
  def setChild(child: SymbolicNode[C]): Unit = {
    setChild(child, parent.thenBranch.storeUpdates)
  }
  def setChild(child: SymbolicNode[C], storeUpdates: Iterable[StoreUpdate]): Unit = {
    if (existingChild != child)
      parent.setThenBranch(ThenEdge(storeUpdates, child))
  }
  def existingChild: SymbolicNode[C] = parent.thenBranch.to
}

abstract class BaseReporter[
  PathConstraintUsed,
  C <: Constraint : ConstraintNegater : Optimizer : HasStoreUpdates]
  extends Reporter[PathConstraintUsed, C] {

  def clear(): Unit = {
    IdGenerator.resetId()
  }

  def writeSymbolicTree(path: String): Unit = getRoot match {
    case None =>
    case Some(root) => new SymbolicTreeDotWriter()(implicitly[HasStoreUpdates[C]]).writeTree(root, path)
  }

  def nodeToSetChild(
    node: SymbolicNode[C],
    constraintIsTrue: Boolean
  ): SetChild[C] = node match {
    case bsn: BranchSymbolicNode[C] =>
      if (constraintIsTrue) SetChildThenBranch[C](bsn)
      else SetChildElseBranch[C](bsn)
    case _ => throw new Exception(s"Unexpected child node: $node")
  }
}

trait UpdateTreePath[C <: Constraint] {
  def update(treePath: TreePath[C], nodeToAdd: SymbolicNode[C]): TreePath[C]
}
case class AddElseBranchToTreePath[C <: Constraint](storeUpdates: Iterable[StoreUpdate]) extends UpdateTreePath[C] {
  override def update(
    treePath: TreePath[C],
    nodeToAdd: SymbolicNode[C],
  ): TreePath[C] = nodeToAdd match {
    case nwc: SymbolicNodeWithConstraint[C] =>
      treePath.addElseBranch(nwc, ElseEdge(storeUpdates, nodeToAdd))
    case _ =>
      treePath.finishedViaElse
  }
}
case class AddThenBranchToTreePath[C <: Constraint](storeUpdates: Iterable[StoreUpdate]) extends UpdateTreePath[C] {
  override def update(
    treePath: TreePath[C],
    nodeToAdd: SymbolicNode[C],
  ): TreePath[C] = nodeToAdd match {
    case nwc: SymbolicNodeWithConstraint[C] =>
      treePath.addThenBranch(nwc, ThenEdge(storeUpdates, nodeToAdd))
    case _ =>
      treePath.finishedViaThen
  }
}
case class InitTreePath[C <: Constraint]()(implicit negater: ConstraintNegater[C]) extends UpdateTreePath[C] {
  override def update(
    treePath: TreePath[C],
    nodeToAdd: SymbolicNode[C]
  ): TreePath[C] = nodeToAdd match {
    case nwc: SymbolicNodeWithConstraint[C] => TreePath.init(nwc)(negater)._1
    case _ => TreePath.empty[C]
  }
}

abstract class PCReporter[C <: Constraint : ConstraintNegater : Optimizer : HasStoreUpdates, PCElementUsed <: PCElement[C]]
  extends BaseReporter[PathConstraintWith[C, PCElementUsed], C]() {

  var optRoot: Option[SymbolicNode[C]] = None

  def getRoot: Option[SymbolicNode[C]] = optRoot
  def setRoot(newRoot: SymbolicNode[C]): Unit = {
    optRoot = Some(newRoot)
  }

  /**
    * Completely removes the symbolic tree that was already explored.
    */
  def deleteSymbolicTree(): Unit = {
    optRoot = None
  }
  def addExploredPath(constraints: PathConstraintWith[C, PCElementUsed], finishedTestRun: Boolean): TreePath[C] = {
    internalAddExploredPath(
      constraints, if (finishedTestRun) RegularLeafNode[C]() else UnexploredNode[C](), finishedTestRun)
  }
  def mergePath(currentConstraints: PathConstraintWith[C, PCElementUsed]): Unit = {
    internalAddExploredPath(currentConstraints, MergedNode[C](), true)
  }
  protected def internalAddExploredPath(
    constraints: PathConstraintWith[C, PCElementUsed],
    terminationNode: SymbolicNode[C],
    replaceLastNode: Boolean
  ): TreePath[C]
  protected def existOtherTCConstraints(constraints: PathConstraint[C]): Boolean =
    constraints.exists({
      case RegularPCElement(_: TargetChosen, _) => true
      case RegularPCElement(EventConstraintWithExecutionState(_: TargetChosen, _), _) => true
      case _ => false
    })

  protected def invokeSetChild(
    setChild: SetChild[C],
    child: SymbolicNode[C],
    constraints: PathConstraintWith[C, PCElementUsed],
    lastStoreConstraints: List[StoreUpdate]
  ): Unit

  case object SetRoot extends SetChild[C] {
    def existingEdge: Option[Edge[C]] = None
    override def existingChild: SymbolicNode[C] = optRoot.getOrElse(UnexploredNode())
    def setChild(child: SymbolicNode[C]): Unit = {
      setChild(child, Nil)
    }
    def setChild(child: SymbolicNode[C], storeUpdates: Iterable[StoreUpdate]): Unit = {
      optRoot = Some(child)
    }
  }

  /*
  protected def internalAddExploredPath(constraints: PathConstraint[EventConstraint],
    terminationNode: SymbolicNode[EventConstraint]): Unit = {

if (constraints.isEmpty) {
return
} else if (optRoot.exists({ case SafeNode(_) => true; case _ => false })) {
return
}

@tailrec
    def loop(currentConstraints: PathConstraint[C], setChild: SetChild[Unit]): Unit =
currentConstraints.headOption match {
case Some(RegularPCElement(tc: TargetChosen, currentConstraintIsTrue)) =>
assert(currentConstraintIsTrue) // By default, this condition was always set to true
val childNode = setChild.existingChild
childNode match {
case BranchSymbolicNode(previous: TargetChosen, _, _, _) if previous.id != tc.id =>
throw new Exception(
s"Should not happen: IDs should be the same: ${previous.id} and ${tc.id}")
case bsn @ BranchSymbolicNode(previous: TargetChosen, _, _, _)
                if previous.processIdChosen == tc.processIdChosen && previous.targetIdChosen == tc.targetIdChosen =>
// Don't actually have to set the current TC constraint in the tree here, since it is already present.
val newSetChild = nodeToSetChild(bsn, true)
loop(currentConstraints.tail, newSetChild)
case bsn @ BranchSymbolicNode(previous: TargetChosen, _, _, _) =>
val newSetChild = nodeToSetChild(bsn, false)
              val updatedConstraintsHead = RegularPCElement[C](
tc.addTargetChosen(previous.processIdChosen, previous.targetIdChosen),
currentConstraintIsTrue)
loop(updatedConstraintsHead :: currentConstraints.tail, newSetChild)
case bsn @ BranchSymbolicNode(StopTestingTargets(id, _), _, _, _) =>
assert(id < tc.id)
/* Encountered a StopTestingTargets-node. However, since we are adding a new TargetChosen-constraint in the tree,
 * at least one more target has already been chosen, so we need to descend into the else-branch. */
val newSetChild = nodeToSetChild(bsn, false)
loop(currentConstraints, newSetChild)
            case _: BranchSymbolicNode[C] =>
throw new Exception(s"Should not happen: node should not be a $childNode")
case UnexploredNode() =>
val newNode = constraintToNode(tc)
invokeSetChild(setChild, newNode, currentConstraints)
val newSetChild = nodeToSetChild(newNode, currentConstraintIsTrue)
              if (tc.lastPossibleTargetForThisId) {
nodeToSetChild(newNode, false).setChild(UnsatisfiableNode())
}
loop(currentConstraints.tail, newSetChild)
case SafeNode(_)                                                                      => /* Stop expansion of the symbolic tree */
            case MergedNode() | RegularLeafNode() | UnsatisfiableNode() if mode == SolvePathsMode =>
//            throw new Exception(s"Should not happen: node should not be a $childNode, currentConstraints = $currentConstraints")
              val newNode = constraintToNode(tc)
              invokeSetChild(setChild, newNode, currentConstraints)
              val newSetChild = nodeToSetChild(newNode, currentConstraintIsTrue)
              if (tc.lastPossibleTargetForThisId) {
                nodeToSetChild(newNode, false).setChild(UnsatisfiableNode())
              }
              loop(currentConstraints.tail, newSetChild)
            case MergedNode() | RegularLeafNode() | UnsatisfiableNode()
                if mode == ExploreTreeMode || mode == VerifyIntraMode =>
throw new Exception(
s"Should not happen: node should not be a $childNode, currentConstraints = $currentConstraints")
}

case Some(RegularPCElement(constraint: BranchConstraint, currentConstraintIsTrue)) =>
  addConstraint(constraint, setChild, currentConstraints, true) match {
  case Some((newPathConstraint, newSetChild)) => loop(newPathConstraint, newSetChild)
  case None                                   => /* Stop recursion */
}
  case None =>
  invokeSetChild(setChild, terminationNode, Nil)
  //        setChild.setChild(terminationNode)
}

  /*
   * Constraints contains at least one element, now find the first BranchConstraint in the list of constraints.
   * Will throw an error if there are no BranchConstraints in the list of constraints.
   */
  val (headConstraint, headIsTrue, remainder) = findFirstUsableConstraint(constraints).get
  loop(RegularPCElement(headConstraint, headIsTrue) :: remainder, SetRoot)
}
   */
}

class SolveModePCReporter
  extends PCReporter[EventConstraint, RegularPCElement[EventConstraint]]()(
    EventConstraintNegater, EventConstraintOptimizer, EventConstraintHasStoreUpdates) {

  protected def constraintToNode(
    constraint: EventConstraint
  ): SymbolicNodeWithConstraint[EventConstraint] = constraint match {
    case _: BranchConstraint | _: TargetChosen | _: StopTestingTargets =>
      new BranchSymbolicNode(constraint, ThenEdge.to(UnexploredNode()), ElseEdge.to(UnexploredNode()))
  }

  /**
    * Finds the first BranchConstraint in the list of constraints, if there is one, and returns a triple of
    * all non-BranchConstraints before the BranchConstraints, the BranchConstraint itself, and all constraints
    * that follows this BranchConstraint.
    *
    */
  protected def findFirstUsableConstraint(
    constraints: PathConstraint[EventConstraint]
  ): Option[(EventConstraint, Boolean, PathConstraint[EventConstraint])] = {
    constraints match {
      case Nil => None
      /* If remainder is not empty, its first element should be a tuple where the first field is a BranchConstraint */
      case RegularPCElement(headConstraint: EventConstraint, headConstraintTrue) :: rest =>
        Some((headConstraint, headConstraintTrue, rest))
    }
  }

  protected def invokeSetChild(
    setChild: SetChild[EventConstraint],
    child: SymbolicNode[EventConstraint],
    constraints: PathConstraint[EventConstraint],
    lastStoreUpdates: List[StoreUpdate]
  ): Unit = {
    def nodeIsAStopTestingConstraint(node: SymbolicNode[EventConstraint]): Boolean = node match {
      case BranchSymbolicNode(_: StopTestingTargets, _, _) => true
      case _ => false
    }
    setChild match {
      case SetChildThenBranch(BranchSymbolicNode(tc1: TargetChosen, _, _))
        if !nodeIsAStopTestingConstraint(child) =>
        val stopTestingOtherTargetsNode =
          if (nodeIsAStopTestingConstraint(setChild.existingChild)) {
            setChild.existingChild
          } else {
            constraintToNode(StopTestingTargets(tc1.id, tc1.info.processesInfo, Nil, tc1.startingProcess))
          }
        setChild.setChild(stopTestingOtherTargetsNode, lastStoreUpdates)
        // In solve-mode, events are prescribed by the front-end, so the symbolic tree shouldn't place restrictions on which events are allowed
        val noOtherTCsInConstraints = false
        val newSetChild = nodeToSetChild(stopTestingOtherTargetsNode, noOtherTCsInConstraints)
        newSetChild.setChild(child, lastStoreUpdates)
      case _ =>
        setChild.setChild(child, lastStoreUpdates)
    }
  }

  protected def addConstraint(
    constraint: EventConstraint,
    setChild: SetChild[EventConstraint],
    currentConstraints: PathConstraint[EventConstraint],
    currentConstraintIsTrue: Boolean,
    lastStoreConstraints: List[StoreUpdate]
  ): Option[(PathConstraint[EventConstraint], SetChild[EventConstraint])] = {
    val childNode = setChild.existingChild
    lazy val newNode = constraintToNode(constraint)
    val node = if (childNode == UnexploredNode[EventConstraint]()) newNode else childNode
    invokeSetChild(setChild, node, currentConstraints, lastStoreConstraints)
    node match {
      case bsn@BranchSymbolicNode(_: StopTestingTargets, _, _) =>
        val otherTargetsChosenAvailable = true
        val newSetChild = nodeToSetChild(bsn, !otherTargetsChosenAvailable)
        Some((currentConstraints, newSetChild))
      case node: BranchSymbolicNode[EventConstraint] =>
        val setChild = nodeToSetChild(node, currentConstraintIsTrue)
        Some((currentConstraints.tail, setChild))
      case SafeNode(_) => None /* Stop expansion of the symbolic tree */
      case MergedNode() | RegularLeafNode() | UnexploredNode() | UnsatisfiableNode() =>
        throw new Exception(s"Should not happen: node should not be a $node")
    }
  }

  protected def internalAddExploredPath(
    constraints: PathConstraint[EventConstraint],
    terminationNode: SymbolicNode[EventConstraint], replaceLastNode: Boolean
  ): TreePath[EventConstraint] = {

    if (constraints.isEmpty) {
      return TreePath.empty[EventConstraint]
    } else if (optRoot.exists({ case SafeNode(_) => true; case _ => false })) {
      return TreePath.empty[EventConstraint]
    }

    @tailrec
    def loop(
      currentConstraints: PathConstraint[EventConstraint],
      setChild: SetChild[EventConstraint],
      lastStoreConstraints: List[StoreUpdate]
    ): Unit =
      currentConstraints.headOption match {
        case Some(RegularPCElement(sc: StoreUpdate, _)) =>
          loop(currentConstraints.tail, setChild, lastStoreConstraints :+ sc)
        case Some(RegularPCElement(tc: TargetChosen, currentConstraintIsTrue)) =>
          assert(currentConstraintIsTrue) // By default, this condition was always set to true
          val childNode = setChild.existingChild
          childNode match {
            case BranchSymbolicNode(previous: TargetChosen, _, _) if previous.id != tc.id =>
              throw new Exception(
                s"Should not happen: IDs should be the same: ${previous.id} and ${tc.id}")
            case bsn@BranchSymbolicNode(previous: TargetChosen, _, _)
              if previous.processIdChosen == tc.processIdChosen && previous.targetIdChosen == tc.targetIdChosen =>
              // Don't actually have to set the current TC constraint in the tree here, since it is already present.
              val newSetChild = nodeToSetChild(bsn, true)
              loop(currentConstraints.tail, newSetChild, lastStoreConstraints)
            case bsn@BranchSymbolicNode(previous: TargetChosen, _, _) =>
              val newSetChild = nodeToSetChild(bsn, false)
              val updatedConstraintsHead = RegularPCElement[EventConstraint](
                tc.addTargetChosen(previous.processIdChosen, previous.targetIdChosen),
                currentConstraintIsTrue)
              loop(updatedConstraintsHead :: currentConstraints.tail, newSetChild, lastStoreConstraints)
            case bsn@BranchSymbolicNode(StopTestingTargets(id, _, _, _), _, _) =>
              assert(id < tc.id)
              /* Encountered a StopTestingTargets-node. However, since we are adding a new TargetChosen-constraint in the tree,
               * at least one more target has already been chosen, so we need to descend into the else-branch. */
              val newSetChild = nodeToSetChild(bsn, false)
              loop(currentConstraints, newSetChild, lastStoreConstraints)
            case _: BranchSymbolicNode[EventConstraint] =>
              throw new Exception(s"Should not happen: node should not be a $childNode")
            case UnexploredNode() =>
              val newNode = constraintToNode(tc)
              invokeSetChild(setChild, newNode, currentConstraints, lastStoreConstraints)
              val newSetChild = nodeToSetChild(newNode, currentConstraintIsTrue)
              if (tc.lastPossibleTargetForThisId(SolvePathsMode)) {
                nodeToSetChild(newNode, false).setChild(UnsatisfiableNode(), Nil)
              }
              loop(currentConstraints.tail, newSetChild, Nil)
            case SafeNode(_) => /* Stop expansion of the symbolic tree */
            case MergedNode() | RegularLeafNode() | UnsatisfiableNode() =>
              //            throw new Exception(s"Should not happen: node should not be a $childNode, currentConstraints = $currentConstraints")
              val newNode = constraintToNode(tc)
              invokeSetChild(setChild, newNode, currentConstraints, lastStoreConstraints)
              val newSetChild = nodeToSetChild(newNode, currentConstraintIsTrue)
              if (tc.lastPossibleTargetForThisId(SolvePathsMode)) {
                nodeToSetChild(newNode, false).setChild(UnsatisfiableNode(), Nil)
              }
              loop(currentConstraints.tail, newSetChild, lastStoreConstraints)
          }

        case Some(RegularPCElement(constraint: BranchConstraint, currentConstraintIsTrue)) =>
          addConstraint(constraint, setChild, currentConstraints, currentConstraintIsTrue, lastStoreConstraints) match {
            case Some((newPathConstraint, newSetChild)) => loop(newPathConstraint, newSetChild, Nil)
            case None => /* Stop recursion */
          }
        case None =>
          if (replaceLastNode) {
            invokeSetChild(setChild, terminationNode, Nil, lastStoreConstraints)
          }
        //        setChild.setChild(terminationNode)
      }

    /*
     * Constraints contains at least one element, now find the first BranchConstraint in the list of constraints.
     * Will throw an error if there are no BranchConstraints in the list of constraints.
     */
    val (headConstraint, headIsTrue, remainder) = findFirstUsableConstraint(constraints).get
    loop(RegularPCElement(headConstraint, headIsTrue) :: remainder, SetRoot, Nil)
    ???
  }
}

class IncludeSingleTCPCReporter(val processId: Int, val nrOfEnabledEvents: Int, mode: Mode)
  extends EventConstraintPCReporter(mode) {

  protected val processesInfo: ProcessesInfo = ProcessesInfo(
    0.to(processId)
      .map((otherProcessId: Int) => if (processId == otherProcessId) nrOfEnabledEvents else 0)
      .toList,
    allowCreatingExtraTargets = false)

  override def addExploredPath(
    constraints: PathConstraintWith[EventConstraint, RegularPCElement[EventConstraint]],
    finishedTestRun: Boolean
  ): TreePath[EventConstraint] = {
    val finalNodes = makeFinalNodes(constraints)
    println(s"IncludeSingleTCPCReporter.addExploredPath, constraints = $constraints, finalNodes = $finalNodes")
    internalAddExploredPath(constraints, finalNodes, finishedTestRun)
    println(s"IncludeSingleTCPCReporter.completed internalAddExploredPath")
    ???
  }

  protected def makeFinalNodes(constraints: PathConstraint[EventConstraint]): SymbolicNode[EventConstraint] = {
    // We only want to insert 1 event in any given path, so if the path we just inserted already contains
    // a TargetChosen constraint, just return a RegularLeafNode.
    val containsTargetChosen = constraints.exists((element: RegularPCElement[EventConstraint]) =>
      element.constraint match {
        case _: TargetChosen => true
        case _ => false
      })

    if (nrOfEnabledEvents == 0 || containsTargetChosen) {
      RegularLeafNode[EventConstraint]()
    } else {
      val initialMap = 0
        .to(processId)
        .foldLeft(Map[Int, Set[Int]]())((map, pid) =>
          map + (pid -> (if (pid == processId) Set(0) else Set())))
      new BranchSymbolicNode[EventConstraint](
        event_constraints.TargetChosen(
          0, processId, 0, initialMap, processesInfo, 0, Nil), // TODO assumes startingProcess is 0
        ThenEdge.to[EventConstraint](UnexploredNode()),
        ElseEdge.to[EventConstraint](UnexploredNode()))
    }
  }

}