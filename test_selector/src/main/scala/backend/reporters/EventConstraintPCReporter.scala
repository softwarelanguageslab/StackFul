package backend.reporters

import backend._
import backend.execution_state.store.StoreUpdate
import backend.modes.Mode
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.event_constraints._
import backend.tree.constraints._
import backend.tree._
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec

class EventConstraintPCReporter(val mode: Mode)
  extends PCReporter[EventConstraint, RegularPCElement[EventConstraint]]()(
    EventConstraintNegater, EventConstraintOptimizer, EventConstraintHasStoreUpdates) {

  protected def internalAddExploredPath(
    constraints: PathConstraint[EventConstraint],
    terminationNode: SymbolicNode[EventConstraint],
    replaceLastNode: Boolean
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
      updateTreePath: UpdateTreePath[EventConstraint],
      treePath: TreePath[EventConstraint],
      lastStoreConstraints: List[StoreUpdate]
    ): TreePath[EventConstraint] = currentConstraints.headOption match {
      case Some(pcElement) => pcElement match {
        case RegularPCElement(constraint: StopTestingTargets, isTrue) =>
          val constraintAdded = addConstraint(constraint, setChild, updateTreePath, currentConstraints, isTrue, treePath, lastStoreConstraints)
          val (newPathConstraint, newSetChild, newUpdateTreePath, newTreePath) = constraintAdded
          loop(newPathConstraint, newSetChild, newUpdateTreePath, newTreePath, Nil)
        case RegularPCElement(tc: TargetChosen, currentConstraintIsTrue) =>
          assert(currentConstraintIsTrue) // By default, this condition was always set to true
          val childNode = setChild.existingChild
          childNode match {
            case BranchSymbolicNode(previous: TargetChosen, _, _) if previous.id != tc.id =>
              throw new Exception(s"Should not happen: IDs should be the same: ${previous.id} and ${tc.id}")
            case bsn@BranchSymbolicNode(previous: TargetChosen, _, _)
              if previous.processIdChosen == tc.processIdChosen && previous.targetIdChosen == tc.targetIdChosen =>
              // Don't actually have to set the current TC constraint in the tree here, since it is already present.
              val newSetChild = nodeToSetChild(bsn, true)
              val newUpdateTreePath = AddThenBranchToTreePath[EventConstraint](lastStoreConstraints)
              val newTreePath = updateTreePath.update(treePath, bsn)
              loop(currentConstraints.tail, newSetChild, newUpdateTreePath, newTreePath, lastStoreConstraints)
            case bsn@BranchSymbolicNode(previous: TargetChosen, _, _) =>
              val newSetChild = nodeToSetChild(bsn, false)
              val newUpdateTreePath = AddElseBranchToTreePath[EventConstraint](lastStoreConstraints)
              val newTreePath = updateTreePath.update(treePath, bsn)
              val updatedConstraintsHead = RegularPCElement[EventConstraint](
                tc.addTargetChosen(previous.processIdChosen, previous.targetIdChosen),
                currentConstraintIsTrue)
              loop(updatedConstraintsHead :: currentConstraints.tail, newSetChild, newUpdateTreePath, newTreePath, lastStoreConstraints)
            case _: BranchSymbolicNode[EventConstraint] =>
              throw new Exception(s"Should not happen: node should not be a $childNode")
            case UnexploredNode() =>
              val newNode = constraintToNode(tc)
              val newTreePath = updateTreePath.update(treePath, newNode)
              invokeSetChild(setChild, newNode, currentConstraints, lastStoreConstraints)
              val newSetChild = nodeToSetChild(newNode, currentConstraintIsTrue)
              val newUpdateTreePath: UpdateTreePath[EventConstraint] = if (currentConstraintIsTrue) {
                AddThenBranchToTreePath(lastStoreConstraints)
              } else {
                AddElseBranchToTreePath(lastStoreConstraints)
              }
              if (tc.lastPossibleTargetForThisId(mode)) {
                nodeToSetChild(newNode, false).setChild(UnsatisfiableNode(), Nil)
              }
              loop(currentConstraints.tail, newSetChild, newUpdateTreePath, newTreePath, Nil)
            case MergedNode() | RegularLeafNode() | SafeNode(_) | UnsatisfiableNode() =>
              throw new Exception(
                s"Should not happen: node should not be a $childNode, currentConstraints = $currentConstraints")
          }

        case RegularPCElement(constraint: BranchConstraint, currentConstraintIsTrue) =>
          val constraintAdded = addConstraint(constraint, setChild, updateTreePath, currentConstraints, currentConstraintIsTrue, treePath, lastStoreConstraints)
          val (newPathConstraint, newSetChild, newUpdateTreePath, newTreePath) = constraintAdded
          loop(newPathConstraint, newSetChild, newUpdateTreePath, newTreePath, Nil)
      }
      case None =>
        if (replaceLastNode) {
          invokeSetChild(setChild, terminationNode, Nil, lastStoreConstraints)
          val newTreePath = updateTreePath.update(treePath, terminationNode)
          newTreePath
        } else {
          val existingChild = setChild.existingChild
          updateTreePath.update(treePath, existingChild)
        }
    }

    /*
     * Constraints contains at least one element, now find the first BranchConstraint in the list of constraints.
     * Will throw an error if there are no BranchConstraints in the list of constraints.
     */
    val (headConstraint, headIsTrue, remainder) = findFirstUsableConstraint(constraints).get
    val treePathInitialiser = InitTreePath[EventConstraint]()
    val initialTreePath = TreePath.empty[EventConstraint]
    loop(RegularPCElement(headConstraint, headIsTrue) :: remainder, SetRoot, treePathInitialiser, initialTreePath, Nil)
  }
  /**
    * Finds the first BranchConstraint in the list of constraints, if there is one, and returns a triple of
    * all non-BranchConstraints before the BranchConstraints, the BranchConstraint itself, and all constraints
    * that follows this BranchConstraint.
    *
    * @param constraints
    * @return
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

  type ConstraintAdded = (PathConstraintWith[EventConstraint, RegularPCElement[EventConstraint]],
      SetChild[EventConstraint],
      UpdateTreePath[EventConstraint],
      TreePath[EventConstraint])

  protected def addConstraint(
    constraint: EventConstraint,
    setChild: SetChild[EventConstraint],
    updateTreePath: UpdateTreePath[EventConstraint],
    currentConstraints: PathConstraint[EventConstraint],
    currentConstraintIsTrue: Boolean,
    treePath: TreePath[EventConstraint],
    lastStoreConstraints: List[StoreUpdate]
  ): ConstraintAdded = {
    val childNode = setChild.existingChild
    lazy val newNode = constraintToNode(constraint)
    val node = if (childNode == UnexploredNode()) newNode else childNode
    invokeSetChild(setChild, node, currentConstraints, lastStoreConstraints)
    val newTreePath = updateTreePath.update(treePath, node)
    val newUpdateTreePath: UpdateTreePath[EventConstraint] = if (currentConstraintIsTrue) {
      AddThenBranchToTreePath(lastStoreConstraints)
    } else {
      AddElseBranchToTreePath(lastStoreConstraints)
    }
    node match {
      case _: SymbolicNodeWithConstraint[EventConstraint] =>
        val setChild = nodeToSetChild(node, currentConstraintIsTrue)
        (currentConstraints.tail, setChild, newUpdateTreePath, newTreePath)
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
        throw new Exception(s"Should not happen: node should not be a $node")
    }
  }
  protected def constraintToNode(
    constraint: EventConstraint
  ): SymbolicNodeWithConstraint[EventConstraint] = constraint match {
    case _: BranchConstraint | _: TargetChosen | _: StopTestingTargets =>
      new BranchSymbolicNode(constraint, ThenEdge.to(UnexploredNode()), ElseEdge.to(UnexploredNode()))
  }
  protected def invokeSetChild(
    setChild: SetChild[EventConstraint],
    child: SymbolicNode[EventConstraint],
    constraints: PathConstraint[EventConstraint],
    lastStoreConstraints: List[StoreUpdate]
  ): Unit = {
    setChild.setChild(child, lastStoreConstraints)
  }
}
