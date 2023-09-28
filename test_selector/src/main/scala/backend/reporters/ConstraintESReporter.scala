package backend.reporters

import backend._
import backend.execution_state._
import backend.execution_state.store._
import backend.expression.{BooleanExpression, ExpressionSubstituter}
import backend.modes.Mode
import backend.tree._
import backend.tree.constraints.{ConstraintESHasStoreUpdates, HasStoreUpdates}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state.{toConstraintES => _, _}
import backend.tree.constraints.event_constraints._
import backend.tree.merging.{SpecialConstraintESNegater, StopTestingTargetsCacher}
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec

class ConstraintESReporter(val mode: Mode, var prescribedEvents: Option[List[(Int, Int)]])
  extends PCReporter[ConstraintES, PCElementWithStoreUpdate[ConstraintES]]()(
    ConstraintESNegater, ConstraintESOptimizer, ConstraintESHasStoreUpdates) {

  private implicit val nodeHasStoreUpdates: HasStoreUpdates[SymbolicNode[ConstraintES]] =
    new SymbolicNodeHasStoreUpdates[ConstraintES]

  override protected def existOtherTCConstraints(constraints: PathConstraint[ConstraintES]): Boolean = {
    val existsTTInRemainingConstraints = constraints.exists({
      case RegularPCElement(_: TargetChosen, _) => true
      case RegularPCElement(EventConstraintWithExecutionState(_: TargetChosen, _), _) => true
      case _ => false
    })
    if (prescribedEvents.isEmpty) {
      existsTTInRemainingConstraints
    } else {
      existsTTInRemainingConstraints || prescribedEvents.get.nonEmpty
    }
  }
  override protected def internalAddExploredPath(
    constraints: PathConstraintWithStoreUpdates[ConstraintES],
    terminationNode: SymbolicNode[ConstraintES],
    replaceLastNode: Boolean
  ): TreePath[ConstraintES] = {
    if (constraints.isEmpty) {
      return TreePath.empty[ConstraintES]
    } else if (optRoot.exists({ case SafeNode(_) => true; case _ => false })) {
      return TreePath.empty[ConstraintES]
    }

    @tailrec
    def loop(
      currentConstraints: PathConstraintWithStoreUpdates[ConstraintES],
      setChild: SetChild[ConstraintES],
      updateTreePath: UpdateTreePath[ConstraintES],
      store: SymbolicStore,
      treePath: TreePath[ConstraintES],
      lastStoreConstraints: List[StoreUpdate]
    ): TreePath[ConstraintES] = currentConstraints.headOption match {
        case Some(pcElement) => pcElement match {
          case StoreUpdatePCElement(update) =>
            val (updatedUpdate, updatedStore) = update.replaceAndApply(store)
            loop(currentConstraints.tail, setChild, updateTreePath, updatedStore, treePath, lastStoreConstraints :+ updatedUpdate)
          case ConstraintWithStoreUpdate(constraint, isTrue) =>
            // Do not replace the store updates of this constraint, but apply the store updates to the current store
            val updatedStore = ConstraintESHasStoreUpdates.applyToStore(constraint, store)
            constraint match {
              case updatedConstraint@EventConstraintWithExecutionState(_: StopTestingTargets, _) =>
                val constraintAdded = addConstraint(updatedConstraint, setChild, updateTreePath, currentConstraints, isTrue, updatedStore, treePath, lastStoreConstraints)
                val (newPathConstraint, newSetChild, newUpdateTreePath, newStore, newTreePath) = constraintAdded
                loop(newPathConstraint, newSetChild, newUpdateTreePath, newStore, newTreePath, Nil)
              case constraint@EventConstraintWithExecutionState(tc: TargetChosen, _) =>
                assert(isTrue) // By default, this condition was always set to true
                val childNode = setChild.existingChild
                childNode match {
                  case BranchSymbolicNode(EventConstraintWithExecutionState(previous: TargetChosen, _), _, _) if previous.id != tc.id =>
                    throw new Exception(s"Should not happen: IDs should be the same: ${previous.id} and ${tc.id}")
                  case bsn@BranchSymbolicNode(EventConstraintWithExecutionState(previous: TargetChosen, _), _, _)
                    if previous.processIdChosen == tc.processIdChosen && previous.targetIdChosen == tc.targetIdChosen =>
                    // Don't actually have to set the current TC constraint in the tree here, since it is already present.
                    val newSetChild = nodeToSetChild(bsn, true)
                    val newUpdateTreePath = AddThenBranchToTreePath[ConstraintES](lastStoreConstraints)
                    val newTreePath = updateTreePath.update(treePath, bsn)
                    // Target has been set. If prescribedEvents were also set, drop head
                    prescribedEvents = prescribedEvents.map(_.tail)
                    loop(currentConstraints.tail, newSetChild, newUpdateTreePath, updatedStore, newTreePath, Nil)
                  case bsn@BranchSymbolicNode(EventConstraintWithExecutionState(previous: TargetChosen, es), _, _) =>
                    val newSetChild = nodeToSetChild(bsn, false)
                    val newUpdateTreePath = AddElseBranchToTreePath[ConstraintES](lastStoreConstraints)
                    val newTreePath = updateTreePath.update(treePath, bsn)
                    val targetConstraint = tc.addTargetChosen(previous.processIdChosen, previous.targetIdChosen)
                    val updatedConstraint: ConstraintES = EventConstraintWithExecutionState(targetConstraint, es)
                    val updatedConstraintsHead = ConstraintWithStoreUpdate[ConstraintES](updatedConstraint, isTrue)
                    loop(updatedConstraintsHead :: currentConstraints.tail, newSetChild, newUpdateTreePath, updatedStore, newTreePath, Nil)
                  case _: BranchSymbolicNode[ConstraintES] =>
                    throw new Exception(s"Should not happen: node should not be a $childNode")
                  case UnexploredNode() | RegularLeafNode() =>
                    val updatedConstraint = if (setChild == SetRoot) {
                      implicitly[HasStoreUpdates[ConstraintES]].addStoreUpdatesBefore(constraint, lastStoreConstraints)
                    } else {
                      constraint
                    }
                    val newNode = constraintToNode(updatedConstraint, updatedStore)
                    val newTreePath = updateTreePath.update(treePath, newNode)
                    // Target has been set. If prescribedEvents were also set, drop head
                    this.prescribedEvents = this.prescribedEvents.map(_.tail)
                    invokeSetChild(setChild, newNode, currentConstraints, lastStoreConstraints)
                    val newSetChild = nodeToSetChild(newNode, isTrue)
                    val newUpdateTreePath: UpdateTreePath[ConstraintES] = if (isTrue) {
                      AddThenBranchToTreePath(lastStoreConstraints)
                    } else {
                      AddElseBranchToTreePath(lastStoreConstraints)
                    }
                    if (tc.lastPossibleTargetForThisId(mode)) {
                      nodeToSetChild(newNode, false).setChild(UnsatisfiableNode(), Nil)
                    }
                    loop(currentConstraints.tail, newSetChild, newUpdateTreePath, updatedStore, newTreePath, Nil)
                  case MergedNode() | SafeNode(_) | UnsatisfiableNode() =>
                    throw new Exception(
                      s"Should not happen: node should not be a $childNode, currentConstraints = $currentConstraints")
                }
              case constraint@EventConstraintWithExecutionState(_: BranchConstraint, _) =>
//                val childNode = setChild.existingChild
//                assert(childNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
//                val bsn = childNode.asInstanceOf[BranchSymbolicNode[ConstraintES]]
//                val newTreePath = if (isTrue) treePath.addThenBranch(bsn, )
                val constraintAdded = addConstraint(
                  constraint, setChild, updateTreePath, currentConstraints, isTrue, updatedStore, treePath,
                  lastStoreConstraints)
                val (newPathConstraint, newSetChild, newUpdateTreePath, newStore, newTreePath) = constraintAdded
                loop(newPathConstraint, newSetChild, newUpdateTreePath, newStore, newTreePath, Nil)
            }
        }
        case None =>
          if (replaceLastNode) {
            val updatedTerminationNode = if (setChild == SetRoot) {
              updateChildWithUpdates(terminationNode, lastStoreConstraints)
            } else {
              terminationNode
            }
            invokeSetChild(setChild, updatedTerminationNode, Nil, lastStoreConstraints)
            val newTreePath = updateTreePath.update(treePath, updatedTerminationNode)
            newTreePath
          } else {
            val existingChild = setChild.existingChild
            updateTreePath.update(treePath, existingChild)
          }
      }

    val treePathInitialiser = InitTreePath[ConstraintES]()(SpecialConstraintESNegater)
    val initialTreePath = TreePath.empty[ConstraintES](SpecialConstraintESNegater)
    val pathAdded = loop(constraints, SetRoot, treePathInitialiser, emptyStore, initialTreePath, Nil)
    prescribedEvents = None // Always reset prescribed events, whether they were set or not
    pathAdded
  }

  type ConstraintAdded = (PathConstraintWith[ConstraintES, PCElementWithStoreUpdate[ConstraintES]],
     SetChild[ConstraintES],
     UpdateTreePath[ConstraintES],
     SymbolicStore,
     TreePath[ConstraintES])

  protected def addConstraint(
    constraint: ConstraintES,
    setChild: SetChild[ConstraintES],
    updateTreePath: UpdateTreePath[ConstraintES],
    currentConstraints: PathConstraintWith[ConstraintES, PCElementWithStoreUpdate[ConstraintES]],
    currentConstraintIsTrue: Boolean,
    storeInstance: SymbolicStore,
    treePath: TreePath[ConstraintES],
    lastStoreConstraints: List[StoreUpdate]
  ): ConstraintAdded = {
    val updatedConstraint = if (setChild == SetRoot) {
      implicitly[HasStoreUpdates[ConstraintES]].addStoreUpdatesBefore(constraint, lastStoreConstraints)
    } else {
      constraint
    }
    val childNode = setChild.existingChild
    val updatedStore = nodeHasStoreUpdates.applyToStore(childNode, storeInstance)
    // If childNode already existed, the store updates of its constraint (if there are any)
    // should be applied to the store
    lazy val newNode = constraintToNode(updatedConstraint, updatedStore)
    val node = if (childNode == UnexploredNode[ConstraintES]()) newNode else childNode
    invokeSetChild(setChild, node, currentConstraints, lastStoreConstraints)
    val newTreePath = updateTreePath.update(treePath, node)
    val newUpdateTreePath: UpdateTreePath[ConstraintES] = if (currentConstraintIsTrue) {
      AddThenBranchToTreePath(lastStoreConstraints)
    } else {
      AddElseBranchToTreePath(lastStoreConstraints)
    }
    node match {
      case _: SymbolicNodeWithConstraint[ConstraintES] =>
        val setChild = nodeToSetChild(node, currentConstraintIsTrue)
        (currentConstraints.tail, setChild, newUpdateTreePath, updatedStore, newTreePath)
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
        throw new Exception(s"Should not happen: node should not be a $node")
    }
  }
  private def updateChildWithUpdates(
    node: SymbolicNode[ConstraintES],
    storeUpdates: Iterable[StoreUpdate]
  ): SymbolicNode[ConstraintES] = node match {
    case bsn: BranchSymbolicNode[ConstraintES] =>
      val updatedConstraint = implicitly[HasStoreUpdates[ConstraintES]].addStoreUpdatesBefore(
        bsn.constraint, storeUpdates)
      bsn.copy(constraint = updatedConstraint)
    case _ => node
  }

  protected def invokeSetChild(
    setChild: SetChild[ConstraintES],
    child: SymbolicNode[ConstraintES],
    constraints: PathConstraintWith[ConstraintES, PCElementWithStoreUpdate[ConstraintES]],
    lastStoreConstraints: List[StoreUpdate]
  ): Unit = {
    //    val updatedChild = if (setChild == SetRoot) {
    //      updateChildWithUpdates(child, lastStoreConstraints)
    //    } else {
    //      child
    //    }
    //    setChild.setChild(updatedChild, lastStoreConstraints)
    setChild.setChild(child, lastStoreConstraints)
  }

  protected def constraintToNode(
    constraint: ConstraintES,
    storeInstance: SymbolicStore
  ): SymbolicNodeWithConstraint[ConstraintES] = {
    constraint match {
      case constraint@EventConstraintWithExecutionState(ec, es) => ec match {
        case bc: BranchConstraint =>
          /* There are no guarantees whether the identifier in the store has the same type as the expression
           * that will be replaced so we have to cast here. */
//          val newBc = bc.copy(exp = ExpressionSubstituter.replace(
//            bc.exp, storeInstance,
//            Set()).asInstanceOf[ConvertibleToBooleanExpression].toBool) // TODO exitIdentifiers = empty set ?
          val replacedExp = ExpressionSubstituter.replaceAtTopLevel(storeInstance, bc.exp)
          val newBc = bc.copy(exp = replacedExp.asInstanceOf[BooleanExpression])
          val newConstraint = EventConstraintWithExecutionState(newBc, es)
          new BranchSymbolicNode(newConstraint, ThenEdge.to(UnexploredNode()), ElseEdge.to(UnexploredNode()))
        case _: TargetChosen =>
          new BranchSymbolicNode(constraint, ThenEdge.to(UnexploredNode()), ElseEdge.to(UnexploredNode()))
        case stt: StopTestingTargets =>
          val newNode: BranchSymbolicNode[ConstraintES] = new BranchSymbolicNode(
            constraint, ThenEdge.to(UnexploredNode()), ElseEdge.to(UnexploredNode()))
          StopTestingTargetsCacher.add(stt, newNode)
          newNode
      }
    }
  }
}

object ConstraintESReporter {
  def newReporter(mode: Mode): ConstraintESReporter = {
    new ConstraintESReporter(mode, None)
  }
  def withPrescribedEvents(reporter: ConstraintESReporter, prescribedEvents: List[(Int, Int)]): ConstraintESReporter = {
    val newReporter = new ConstraintESReporter(reporter.mode, Some(prescribedEvents))
    newReporter.optRoot = reporter.optRoot
    newReporter
  }
}
