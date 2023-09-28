package backend.tree.merging

import backend.Path
import backend.execution_state.{ExecutionState, SymbolicStore}
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.tree.{OffersVirtualPath, SymbolicNode, _}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, _}
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.constraints.{ConstraintESHasStoreUpdates, ToBooleanExpression}

class SymbolicStoreMerger {

  type Result = (List[CreateITEFor], List[(String, SymbolicExpression)])

  def produceMergedStore(
    mergingPredicate: List[ConstraintES],
    node1Info: PathInformationOffersVirtualPath[ConstraintES],
    node2Info: PathInformationOffersVirtualPath[ConstraintES],
    parentStore: SymbolicStore,
    identifiersAssigned: Set[String]
  ): (SymbolicStore, List[String]) = {
    val trueStoreEntries = node1Info.store.toSet
    val falseStoreEntries = node2Info.store.toSet
    val parentEntries = parentStore.toSet
    val onlyTrueEntries = trueStoreEntries.filter(tuple => {
      val (variable, _) = tuple
      !node2Info.store.contains(variable)
      //      !(parentStore.contains(variable) || falseChildStore.contains(variable))
    })
    val onlyFalseEntries = falseStoreEntries.filter(tuple => {
      val (variable, _) = tuple
      !node1Info.store.contains(variable)
      //      !(parentStore.contains(variable) || trueChildStore.contains(variable))
    })
    val onlyTrueEntriesAdded: SymbolicStore = onlyTrueEntries.foldLeft(parentStore)((accStore, trueEntry) => {
      accStore.map + (trueEntry._1 -> trueEntry._2)
    })
    val onlyFalseEntriesAdded: SymbolicStore = onlyFalseEntries.foldLeft(onlyTrueEntriesAdded)(
      (accStore, falseEntry) => {
        accStore.map + (falseEntry._1 -> falseEntry._2)
      })
    val bothTrueFalseEntriesNames: Set[String] = trueStoreEntries.map(_._1).intersect(falseStoreEntries.map(_._1))

    import ConstraintESAllInOne.toBooleanExpression
    val mergingPredicateExp = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(mergingPredicate)
    val (toCreateITEFor, identicalExps) = produceToCreateITEFor(
      mergingPredicateExp, node1Info.store, node2Info.store, bothTrueFalseEntriesNames, identifiersAssigned)
    val identicalExpsAdded = identicalExps.foldLeft[SymbolicStore](onlyFalseEntriesAdded)((store, tuple) => {
      new SymbolicStore(store.map + tuple)
    })
    identicalExps.foreach(identicalExp => {
      val (identifier, exp) = identicalExp
      val trueVirtualPathStore = node1Info.offersVirtualPath.getVirtualPathStoreForIdentifier(identifier)
      val falseVirtualPathStore = node2Info.offersVirtualPath.getVirtualPathStoreForIdentifier(identifier)
      val iteCreator = new SymbolicITECreator(trueVirtualPathStore)
      iteCreator.addEmptyEntryInVirtualPathStoreFor(mergingPredicateExp, exp, node1Info.pathToNode, node2Info.pathToNode)
    })
    //    val mergedExpressions = new SymbolicITECreator(trueOffersVirtualPath, falseOffersVirtualPath).createITE(mergingPredicateExp, pathToTrueStore, pathToFalseStore, toCreateITEFor)
    val mergedExpressions = toCreateITEFor.foldLeft[List[(String, SymbolicExpression)]](Nil)((acc, toCreateITEFor) => {
      val trueVirtualPathStore = node1Info.offersVirtualPath.getVirtualPathStoreForIdentifier(toCreateITEFor.identifier)
      val falseVirtualPathStore = node2Info.offersVirtualPath.getVirtualPathStoreForIdentifier(toCreateITEFor.identifier)
      val iteCreator = new SymbolicITECreator(trueVirtualPathStore)
      acc ++ iteCreator.createITE(mergingPredicateExp, node1Info.pathToNode, node2Info.pathToNode, List(toCreateITEFor))
    })
    val result = mergedExpressions.foldLeft[SymbolicStore](identicalExpsAdded)((store, tuple) => {
      //      val identifier: String = tuple._1
      //      val exp: SymbolicExpression = tuple._2
      //      val (identifier: String, exp: SymbolicExpression) = tuple
      new SymbolicStore(store.map + tuple)
    })
    (result, toCreateITEFor.map(_.identifier))

    //    val result = bothTrueFalseEntriesNames.foldLeft(onlyFalseEntriesAdded)((accStore, variable) => {
    //      val trueSymValue = trueChildStore(variable) // trueChildStore.getOrElse(variable, falseChildStore(variable)) // TODO Original: trueChildStore(variable)
    //      val iteExp = produceMergedExp(falseChildStore, mergingPredicate, trueSymValue, pathToTrueStore, pathToFalseStore, trueVirtualPathStore, falseVirtualPathStore)
    //      accStore.map + (variable -> iteExp)
    //    })
    //    result
  }

  def produceToCreateITEFor(
    mergingPredicate: BooleanExpression,
    thenStore: SymbolicStore,
    elseStore: SymbolicStore,
    bothTrueFalseEntriesNames: Set[String],
    identifiersAssigned: Set[String]
  ): Result = {
    bothTrueFalseEntriesNames.foldLeft[Result]((Nil, Nil))((acc, identifier) => {
      val thenExp = thenStore(identifier)
      if (!elseStore.contains(identifier)) {
        throw new Exception
      }
      val elseExp = elseStore(identifier)
      if (thenExp == elseExp && (!identifiersAssigned.contains(identifier))) {
        (acc._1, (identifier, thenExp) :: acc._2)
      } else {
        val toCreate = CreateITEFor(thenExp, elseExp, identifier)
        (toCreate :: acc._1, acc._2)
      }
    })
  }

  def produceMergedConstraint(
    globalMergePointExecutionState: ExecutionState,
    executionState: ExecutionState,
    store: SymbolicStore,
    mergingPredicate: List[ConstraintES],
    pathToExistingNode: Path,
    pathToElseNode: Path,
    existingNode: SymbolicNode[ConstraintES],
    elseNode: SymbolicNode[ConstraintES],
    edgeExitIdentifiers: Set[String]
  ): ConstraintES = {
    def produceConstraint(c: ConstraintES): ConstraintES = c match {
      case c@EventConstraintWithExecutionState(_: TargetChosen, _) => c
      case c@EventConstraintWithExecutionState(_: StopTestingTargets, _) => c
      case EventConstraintWithExecutionState(BranchConstraint(existingNodeExp, storeUpdates), es) =>
        val pathStore = CentralVirtualPathStore.getStore(globalMergePointExecutionState)
        val mergedExp = produceMergedExp(executionState,
          store, mergingPredicate, existingNodeExp, pathToExistingNode, pathToElseNode,
          pathStore).asInstanceOf[BooleanExpression]
        new ConstraintESCreator(es).createBooleanConstraint(mergedExp)
    }

    existingNode match {
      case BranchSymbolicNode(c: EventConstraintWithExecutionState, _, _) => produceConstraint(c)
    }
  }

  def produceMergedExp(
    executionState: ExecutionState,
    store: SymbolicStore,
    mergingPredicate: List[ConstraintES],
    exp: SymbolicExpression,
    pathToExpNode: Path,
    pathToElse: Path,
    virtualPathStore: OffersVirtualPath
  ): SymbolicExpression = {
    val allInOne = ConstraintESAllInOne
    import allInOne.toBooleanExpression
    val mergingPredicateExp = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(mergingPredicate)
    ExpressionSubstituter.merge(executionState,
      exp, mergingPredicateExp, store, pathToExpNode, pathToElse, virtualPathStore)
  }

  def replaceConstraintAndAddStoreUpdate(
    store: SymbolicStore,
    existingNode: SymbolicNode[ConstraintES],
    edgeExitIdentifiers: Set[String],
    identifiersToReplace: List[String]
  ): ConstraintES = {
    def produceConstraint(c: ConstraintES): ConstraintES = {
      def newAssignmentsUpdate(identifiersAssignedInExistingUpdate: List[String]) = {
        val useEntireStore = false
        if (useEntireStore) {
          AssignmentsStoreUpdate(store.map.toList)
        } else {
          AssignmentsStoreUpdate(store.map.filter(keyValue => {
            val storeIdentifier = keyValue._1
            // Replace the value of the identifier if either the identifier was changed,
            // or if it already existed in the store update of the constraint
            identifiersToReplace.contains(storeIdentifier) ||
              identifiersAssignedInExistingUpdate.contains(storeIdentifier)
          }).toList)
        }
      }
      val constraintAssignsIdentifiers = ConstraintESHasStoreUpdates.storeUpdates(c).foldLeft(Nil: List[String])(
        (acc, update) => acc ++ update.assignsIdentifiers)
      c match {
        case c@EventConstraintWithExecutionState(_: TargetChosen, _) => c
        case c@EventConstraintWithExecutionState(stt: StopTestingTargets, es) =>
          EventConstraintWithExecutionState(
            stt.copy(storeUpdates = List(newAssignmentsUpdate(constraintAssignsIdentifiers))), es)
        case EventConstraintWithExecutionState(BranchConstraint(existingNodeExp, _), es) =>
          val mergedExp = replaceExp(store, existingNodeExp, edgeExitIdentifiers).asInstanceOf[BooleanExpression]
          EventConstraintWithExecutionState(
            BranchConstraint(mergedExp, List(newAssignmentsUpdate(constraintAssignsIdentifiers))), es)
      }
    }

    existingNode match {
      case BranchSymbolicNode(c: EventConstraintWithExecutionState, _, _) => produceConstraint(c)
    }
  }

  def replaceConstraintOnlyReplaceButDoNotAddStoreUpdate(
    store: SymbolicStore,
    existingNode: SymbolicNode[ConstraintES],
    edgeExitIdentifiers: Set[String]
  ): ConstraintES = {
    def produceConstraint(c: ConstraintES): ConstraintES = {
      def newAssignmentsUpdate(oldAssignmentsStoreUpdate: AssignmentsStoreUpdate) = {
        // Only replace the identifiers which actually appear in the existing update
        AssignmentsStoreUpdate(store.map.filter(keyValue => oldAssignmentsStoreUpdate.contains(keyValue._1)).toList)
      }
      c match {
        case c@EventConstraintWithExecutionState(_: TargetChosen, _) => c
        case c@EventConstraintWithExecutionState(stt: StopTestingTargets, es) =>
          val newStoreUpdates = stt.storeUpdates.headOption match {
            case None => Nil
            case Some(asu: AssignmentsStoreUpdate) => List(newAssignmentsUpdate(asu))
            case _ => Nil
          }
          EventConstraintWithExecutionState(stt.copy(storeUpdates = newStoreUpdates), es)
        case EventConstraintWithExecutionState(BranchConstraint(existingNodeExp, storeUpdates), es) =>
          val mergedExp = replaceExp(store, existingNodeExp, edgeExitIdentifiers).asInstanceOf[BooleanExpression]
//          val newStoreUpdates = if (storeUpdates.isEmpty) { Nil } else { List(newAssignmentsUpdate) }
          val newStoreUpdates = storeUpdates.headOption match {
            case None => Nil
            case Some(asu: AssignmentsStoreUpdate) => List(newAssignmentsUpdate(asu))
            case _ => Nil
          }
          EventConstraintWithExecutionState(BranchConstraint(mergedExp, newStoreUpdates), es)
      }
    }
    existingNode match {
      case BranchSymbolicNode(c: EventConstraintWithExecutionState, _, _) => produceConstraint(c)
    }
  }

  def replaceExp(
    store: SymbolicStore,
    exp: SymbolicExpression,
    edgeExitIdentifiers: Set[String]
  ): SymbolicExpression = {
    ExpressionSubstituter.replaceAtTopLevels(exp, store, edgeExitIdentifiers)
//    ExpressionSubstituter.replace(exp, store, edgeExitIdentifiers)
  }

}
