package backend.tree.merging

import backend.Path
import backend.execution_state._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.logging.TreeLogger
import backend.reporters.{ConstraintESReporter, SetChild}
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.follow_path._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.util.PathsToGlobalMergePointCounter

import scala.annotation.tailrec

case class ITEPredicateUpdateInformation(
  pathToConstraint: Path,
  pathToNode1: Path,
  pathToNode2: Path,
  newPredicateExp: BooleanExpression
)

private class NodeCacher[Node] {

  type N = SymbolicNode[ConstraintES]
  private var cachedMergedNodes: Map[CacheKey, Node] = Map()

  def getCachedNode(node: N): Option[Node] = {
    val cacheKey = CacheSingleNode(node)
    cachedMergedNodes.get(cacheKey)
  }

  def getCachedNode(node1: N, node2: N): Option[Node] = {
    val cacheKey1 = CacheNodePair(node1, node2)
    val cacheKey2 = CacheNodePair(node2, node1)
    cachedMergedNodes.get(cacheKey1).orElse(cachedMergedNodes.get(cacheKey2))
  }

  def addCachedNode(node: N, mergedNode: Node): Unit = {
    val cacheKey = CacheSingleNode(node)
    cachedMergedNodes += cacheKey -> mergedNode
  }

  def addCachedNode(node1: N, node2: N, mergedNode: Node): Unit = {
    val cacheKey = CacheNodePair(node1, node2)
    cachedMergedNodes += cacheKey -> mergedNode
  }

  def resetCache(): Unit = {
    cachedMergedNodes = Map()
  }

  sealed trait CacheKey
  case class CacheSingleNode(node: N) extends CacheKey
  case class CacheNodePair(node1: N, node2: N) extends CacheKey
}

class IndividualNodesMerger(
  val storeMerger: SymbolicStoreMerger,
  treeLogger: TreeLogger,
  reporter: ConstraintESReporter
)(implicit x: ToBooleanExpression[ConstraintES]) {

  type E = Edge[ConstraintES]
  type N = SymbolicNode[ConstraintES]
  type BSN = BranchSymbolicNode[ConstraintES]

  type MergedNode = (N, SymbolicStore)
  type MergedBranch = (BSN, SymbolicStore)

  private val bsnCache = new NodeCacher[MergedBranch]

  private var optBarrier: Option[MergingBarrier] = None
  private def makeNewBarrier(node: SymbolicNode[ConstraintES]): Option[MergingBarrier] = {
    None
//    val optNext = PathsToGlobalMergePointCounter.countPaths(node)
//    optNext.map(tuple => MergingBarrier(tuple._1.constraint.executionState, tuple._2))
  }

  private implicit val constraintESHasStoreUpdates: HasStoreUpdates[ConstraintES] =
    implicitly[HasStoreUpdates[ConstraintES]]
  private implicit val thenEdgeHasStoreUpdates: HasStoreUpdates[ThenEdge[ConstraintES]] =
    new ThenEdgeHasStoreUpdates[ConstraintES]
  private implicit val elseEdgeHasStoreUpdates: HasStoreUpdates[ElseEdge[ConstraintES]] =
    new ElseEdgeHasStoreUpdates[ConstraintES]

  def unexploredNode: UnexploredNode[ConstraintES] = UnexploredNode()

  def writeTree(): Unit = {
    treeLogger.n(reporter.getRoot.get)
  }

  def replaceSingleNode(
    globalMergePointExecutionState: ExecutionState,
    mergedStore: SymbolicStore,
    identifiersToReplace: Iterable[String],
    node: N,
    exitIdentifiers: Set[String],
    globalPath: Path,
    globalPathsToChange: List[ITEPredicateUpdateInformation]
  ): N =
    node match {
      case UnsatisfiableNode() => UnexploredNode()
      case RegularLeafNode() | UnexploredNode() => node
      case bsn: BranchSymbolicNode[ConstraintES] =>
        replaceSingleBranchNode(globalMergePointExecutionState, mergedStore, identifiersToReplace, bsn, exitIdentifiers, globalPath, globalPathsToChange)
    }

  def mergeTwoNonConstraintNodes(
    node1: SymbolicNode[ConstraintES],
    node2: SymbolicNode[ConstraintES]
  ): SymbolicNode[ConstraintES] = (node1, node2) match {
    case (t@UnexploredNode(), e@UnexploredNode()) =>
      t.mergeChildSetters(e)
      t
    case (t@UnsatisfiableNode(), e: BranchSymbolicNode[ConstraintES]) =>
      e.mergeChildSetters(t)
      e
    case (t@UnsatisfiableNode(), e@RegularLeafNode()) =>
      e.mergeChildSetters(t)
      e
    case (t@UnsatisfiableNode(), e@UnexploredNode()) =>
      e.mergeChildSetters(t)
      e
    case (t@UnsatisfiableNode(), e@UnsatisfiableNode()) =>
      val newNode = UnexploredNode[ConstraintES]()
      t.mergeChildSetters(e)
      newNode.mergeChildSetters(t)
      newNode
    case (t@UnexploredNode(), e@UnsatisfiableNode()) =>
      t.mergeChildSetters(e)
      t
    case (t: RegularLeafNode[ConstraintES], e@RegularLeafNode()) =>
      t.mergeChildSetters(e)
      t
    case (t: RegularLeafNode[ConstraintES], e@UnexploredNode()) =>
      t.mergeChildSetters(e)
      t
    case (t: RegularLeafNode[ConstraintES], e@UnsatisfiableNode()) =>
      t.mergeChildSetters(e)
      t
    case (t@UnexploredNode(), e: RegularLeafNode[ConstraintES]) =>
      e.mergeChildSetters(t)
      e
  }

  /**
    *
    * @return Tuple of newly merged node and the store containing the identifiers which were merged
    */
  def mergeTwoNodes(
    ancestorStore: SymbolicStore,
    globalMergePointExecutionState: ExecutionState,
    mergingPredicate: List[ConstraintES],
    node1Info: PathInformation[ConstraintES, SymbolicNode[ConstraintES]],
    node2Info: PathInformation[ConstraintES, SymbolicNode[ConstraintES]],
    edgeExitIdentifiers: Set[String],
    identifiersAssigned: Set[String]
  ): (SymbolicNode[ConstraintES], SymbolicStore, Option[List[String]]) = (node1Info.node, node2Info.node) match {
    case (t@UnexploredNode(), e@UnexploredNode()) =>
      t.mergeChildSetters(e)
      (t, node1Info.store, None)
    case (t@UnsatisfiableNode(), e@RegularLeafNode()) =>
      e.mergeChildSetters(t)
      (e, node2Info.store, None)
    case (t@UnsatisfiableNode(), e@UnexploredNode()) =>
      e.mergeChildSetters(t)
      (e, node2Info.store, None)
    case (t@UnsatisfiableNode(), e@UnsatisfiableNode()) =>
      val newNode = UnexploredNode[ConstraintES]()
      t.mergeChildSetters(e)
      newNode.mergeChildSetters(t)
      (newNode, node1Info.store, None)
    case (t@UnexploredNode(), e@UnsatisfiableNode()) =>
      t.mergeChildSetters(e)
      (t, node1Info.store, None)
    case (t: RegularLeafNode[ConstraintES], e@RegularLeafNode()) =>
      t.mergeChildSetters(e)
      (t, node1Info.store, None)
    case (t: RegularLeafNode[ConstraintES], e@UnexploredNode()) =>
      t.mergeChildSetters(e)
      (t, node1Info.store, None)
    case (t: RegularLeafNode[ConstraintES], e@UnsatisfiableNode()) =>
      t.mergeChildSetters(e)
      (t, node1Info.store, None)
    case (t@UnexploredNode(), e: RegularLeafNode[ConstraintES]) =>
      e.mergeChildSetters(t)
      (e, node2Info.store, None)

    // node1SymbolicStore and node2SymbolicStore should both exist here.
    // When starting to merge, t and e should both be SymbolicNodeWithConstraints, which always include a symbolic store
    // While in the process of merging the children of these nodes, t and e could be a combination of a BranchSymbolicNode
    // and a leaf, unexplored or unsatisfiable node, but in that case, node1SymbolicStore and node2SymbolicStore
    // should just be the stores of the "common" BranchSymbolicNode
    case (t: BranchSymbolicNode[ConstraintES], e@UnexploredNode()) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node1Info.store, Set(), t, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(e)
      (mergedNode._1, mergedNode._2, None)
    case (t: BranchSymbolicNode[ConstraintES], e@UnsatisfiableNode()) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node1Info.store, Set(), t, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(e)
      (mergedNode._1, mergedNode._2, None)
    case (t: BranchSymbolicNode[ConstraintES], e@RegularLeafNode()) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node1Info.store, Set(), t, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(e)
      (mergedNode._1, mergedNode._2, None)

    case (t@UnexploredNode(), e: BranchSymbolicNode[ConstraintES]) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node2Info.store, Set(), e, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(t)
      (mergedNode._1, mergedNode._2, None)
    case (t@UnsatisfiableNode(), e: BranchSymbolicNode[ConstraintES]) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node2Info.store, Set(), e, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(t)
      (mergedNode._1, mergedNode._2, None)
    case (t@RegularLeafNode(), e: BranchSymbolicNode[ConstraintES]) =>
      val mergedNode = mergeOrRetrieveSingleBranchNode(globalMergePointExecutionState, node2Info.store, Set(), e, edgeExitIdentifiers, Nil, Nil)
      mergedNode._1.mergeChildSetters(t)
      (mergedNode._1, mergedNode._2, None)

    case (t: BranchSymbolicNode[ConstraintES], e: BranchSymbolicNode[ConstraintES]) =>
      val mergedNode = mergeOrRetrieveTwoBranchNodes(
        ancestorStore,
        globalMergePointExecutionState,
        mergingPredicate,
        node1Info.copy(node = t),
        node2Info.copy(node = e),
        edgeExitIdentifiers,
        identifiersAssigned)
      mergedNode
  }

  /**
    * Used when traversing the descendants of a merged node
    */
  private[merging] def replaceBranchConstraint(
    mergedStore: SymbolicStore,
    node: N,
    edgeExitIdentifiers: Set[String]
  ): (ConstraintES, SymbolicStore) = {
    val mergedConstraint = storeMerger.replaceConstraintOnlyReplaceButDoNotAddStoreUpdate(mergedStore, node, edgeExitIdentifiers)
    (mergedConstraint, mergedStore)
  }

  /**
    * Used when creating the constraint for a newly merged node
    */
  private[merging] def replaceBranchConstraint(
    ancestorStore: SymbolicStore,
    mergingPredicate: List[ConstraintES],
    node1Info: PathInformationOffersVirtualPath[ConstraintES],
    node2Info: PathInformationOffersVirtualPath[ConstraintES],
    node: N,
    edgeExitIdentifiers: Set[String],
    identifiersAssigned: Set[String]
  ): (ConstraintES, SymbolicStore, List[String]) = {
    val (mergedStore: SymbolicStore, mergedIdentifiers) = storeMerger.produceMergedStore(
      mergingPredicate,
      node1Info,
      node2Info,
      ancestorStore,
      identifiersAssigned
    )
    val mergedConstraint = storeMerger.replaceConstraintAndAddStoreUpdate(mergedStore, node, edgeExitIdentifiers, mergedIdentifiers)
    (mergedConstraint, mergedStore, mergedIdentifiers)
  }

  private[merging] def mergeTwoBranchNodes(
    ancestorStore: SymbolicStore,
    globalMergePointExecutionState: ExecutionState,
    mergingPredicate: List[ConstraintES],
    node1Info: PathInformation[ConstraintES, BranchSymbolicNode[ConstraintES]],
    node2Info: PathInformation[ConstraintES, BranchSymbolicNode[ConstraintES]],
    exitIdentifiers: Set[String],
    identifiersAssigned: Set[String]
  )
  : (BranchSymbolicNode[ConstraintES], SymbolicStore, List[String]) = {

    val constraintUpdatedTStore = constraintESHasStoreUpdates.applyToStore(node1Info.node.constraint, node1Info.store)
    val constraintUpdatedEStore = constraintESHasStoreUpdates.applyToStore(node2Info.node.constraint, node2Info.store)

    // Using t here to create a new, merged node, but can also use e. Only important parts of the node are the constraint, which should be
    // the same for t and e, and the parentChildSetters, which are merged.
    val (replacedConstraint, mergedStore, mergedIdentifiers) = replaceBranchConstraint(
      ancestorStore,
      mergingPredicate,
      node1Info.copy(store = constraintUpdatedTStore).toOffersVirtualPath(CentralVirtualPathStore.getStore(node1Info.node.constraint.executionState)),
      node2Info.copy(store = constraintUpdatedEStore).toOffersVirtualPath(CentralVirtualPathStore.getStore(node1Info.node.constraint.executionState)),
      node1Info.node,
      exitIdentifiers,
      identifiersAssigned)

    val t = node1Info.node
    val e = node2Info.node

    val newTThenEdge = thenEdgeHasStoreUpdates.replaceIdentifiersWith(t.thenBranch, mergedStore)
    val newEThenEdge = thenEdgeHasStoreUpdates.replaceIdentifiersWith(e.thenBranch, mergedStore)
    val newTElseEdge = elseEdgeHasStoreUpdates.replaceIdentifiersWith(t.elseBranch, mergedStore)
    val newEElseEdge = elseEdgeHasStoreUpdates.replaceIdentifiersWith(e.elseBranch, mergedStore)

    val tThenStore = thenEdgeHasStoreUpdates.applyToStore(newTThenEdge, mergedStore)
    val eThenStore = thenEdgeHasStoreUpdates.applyToStore(newEThenEdge, mergedStore)
    val tElseStore = elseEdgeHasStoreUpdates.applyToStore(newTElseEdge, mergedStore)
    val eElseStore = elseEdgeHasStoreUpdates.applyToStore(newEElseEdge, mergedStore)

    val thenExitIdentifiers = exitIdentifiers ++ t.thenBranch.getExitedIdentifiers ++ e.thenBranch.getExitedIdentifiers
    val elseExitIdentifiers = exitIdentifiers ++ t.elseBranch.getExitedIdentifiers ++ e.elseBranch.getExitedIdentifiers

    assert(t.constraint.executionState == e.constraint.executionState)

    val isGlobalMergePoint = backend.tree.merging.isGlobalMergePoint(t) // Can choose to use t or e
    val globalMergePointExecutionStateToUse = if (isGlobalMergePoint) replacedConstraint.executionState else globalMergePointExecutionState
    val mergedThenNodes: SymbolicNode[ConstraintES] = (t.thenBranch.to, e.thenBranch.to) match {
      case (t: BranchSymbolicNode[ConstraintES], e: BranchSymbolicNode[ConstraintES]) =>
        throw new Exception(s"One of either $t or $e should be empty")
      case (tt: BranchSymbolicNode[ConstraintES], _) =>
        val ttPath = if (isGlobalMergePoint) {
          List(ThenDirection)
        } else {
          node1Info.pathToNode.getPath :+ ThenDirection
        }
        optBarrier = makeNewBarrier(tt)
        replaceSingleBranchNode(
          globalMergePointExecutionStateToUse, tThenStore, mergedIdentifiers, tt, thenExitIdentifiers, ttPath, Nil)
      case (_, et: BranchSymbolicNode[ConstraintES]) =>
        val etPath = if (isGlobalMergePoint) {
          List(ThenDirection)
        } else {
          node2Info.pathToNode.getPath :+ ThenDirection
        }
        optBarrier = makeNewBarrier(et)
        replaceSingleBranchNode(
          globalMergePointExecutionStateToUse, eThenStore, mergedIdentifiers, et, elseExitIdentifiers, etPath, Nil)
      case (tt, et) =>
        mergeTwoNonConstraintNodes(tt, et)
    }

    var mergedElseNodes: SymbolicNode[ConstraintES] = (t.elseBranch.to, e.elseBranch.to) match {
        case (t: BranchSymbolicNode[ConstraintES], e: BranchSymbolicNode[ConstraintES]) =>
          throw new Exception(s"One of either $t or $e should be empty")
        case (te: BranchSymbolicNode[ConstraintES], _) =>
          val tePath = if (isGlobalMergePoint) {
            List(ElseDirection)
          } else {
            node1Info.pathToNode.getPath :+ ElseDirection
          }

          optBarrier = makeNewBarrier(te)
          replaceSingleBranchNode(
            globalMergePointExecutionStateToUse, tElseStore, mergedIdentifiers, te, elseExitIdentifiers, tePath, Nil)
        case (_, ee: BranchSymbolicNode[ConstraintES]) =>
          val eePath = if (isGlobalMergePoint) {
            List(ElseDirection)
          } else {
            node2Info.pathToNode.getPath :+ ElseDirection
          }

          optBarrier = makeNewBarrier(ee)
          replaceSingleBranchNode(
            globalMergePointExecutionStateToUse, eElseStore, mergedIdentifiers, ee, elseExitIdentifiers, eePath, Nil)
        case (te, ee) =>
          mergeTwoNonConstraintNodes(te, ee)
      }

//    var mergedElseNodes = mergeTwoNodes(
//      ancestorStore,
//      mergingPredicate,
//      tePath,
//      eePath,
//      t.elseBranch.to,
//      e.elseBranch.to,
//      tElseStore,
//      eElseStore,
//      elseExitIdentifiers,
//      Set()) // TODO probably shouldn't pass empty set here, but shouldn't matter if descendants of one are empty
    val mergedNode = t.replaceConstraint(replacedConstraint)
    updateStopTestingTargetsCache(replacedConstraint, mergedNode)

    /*
     * If the constraint being merged now is a TargetChosen constraint which originally had an Unsatisfiable
     * else-branch, then make sure the new else-branch is still unsatisfiable. This is a concern because when
     * an UnexploredNode is merged with an UnsatisfiableNode, an UnexploredNode is generated.
     */
    replacedConstraint match {
      case EventConstraintWithExecutionState(_: TargetChosen, _) =>
        if ((t.elseBranch.to == UnsatisfiableNode() || e.elseBranch.to == UnsatisfiableNode()) && mergedElseNodes != UnsatisfiableNode()) {
          mergedElseNodes = UnsatisfiableNode[ConstraintES]()
        }
      case _ =>
    }
    //    val mergedNode = BranchSymbolicNode(replacedConstraint, mergedThenNodes, mergedElseNodes, t.extraInfo)
    mergedNode.mergeChildSetters(e)
    val predExp = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(mergingPredicate)
    mergedNode.thenBranch = ThenEdge.to(mergedThenNodes).merge(predExp, t.thenBranch).merge(predExp, e.thenBranch)
    mergedNode.elseBranch = ElseEdge.to(mergedElseNodes).merge(predExp, t.elseBranch).merge(predExp, e.elseBranch)
    mergedThenNodes.addChildSetter(mergedNode, SetChild(mergedNode, ThenDirection))
    mergedElseNodes.addChildSetter(mergedNode, SetChild(mergedNode, ElseDirection))
    (mergedNode, mergedStore, mergedIdentifiers)
  }

  private def updateStopTestingTargetsCache(
    replacedConstraint: ConstraintES,
    mergedNode: BranchSymbolicNode[ConstraintES]
  ): Unit = {
    replacedConstraint match {
      case EventConstraintWithExecutionState(stt: StopTestingTargets, _) =>
        StopTestingTargetsCacher.add(
          stt,
          mergedNode.asInstanceOf[BranchSymbolicNode[ConstraintES]])
      case _ =>
    }
  }

  private def updateITEPredicates(
    globalMergePointExecutionState: ExecutionState,
    node: BranchSymbolicNode[ConstraintES],
    store: SymbolicStore,
    globalPathsToChange: List[ITEPredicateUpdateInformation]
  ): SymbolicStore = {
    if (isMergedGlobalMergePoint(node)) {
      globalPathsToChange.foldLeft(store)((accStore, information) => {
          val newMapOfAssignments = accStore.map.map(keyValue => {
            val (key, value) = keyValue
            val virtualPathStore = CentralVirtualPathStore.getStore(globalMergePointExecutionState).getVirtualPathStoreForIdentifier(key)
            val newValue = updateITEPredicate(information.pathToNode1, information.pathToNode2, information.newPredicateExp, value, virtualPathStore)
            (key, newValue)
          })
          new SymbolicStore(newMapOfAssignments)
      })
    } else {
      store
    }
  }

  // Replace a BranchNode for which there was no corresponding node
  private[merging] def replaceSingleBranchNode(
    globalMergePointExecutionState: ExecutionState,
    mergedStore: SymbolicStore,
    identifiersToReplace: Iterable[String],
    node: BranchSymbolicNode[ConstraintES],
    exitIdentifiers: Set[String],
    globalPath: Path,
    globalPathsToChange: List[ITEPredicateUpdateInformation]
  ): BSN = {
    val isGlobalMergePoint = isMergedGlobalMergePoint(node)
    // Possibly switch to a new global merge-point ES for the descendants
    val globalMergePointExecutionStateToUse = if (isGlobalMergePoint) node.constraint.executionState else globalMergePointExecutionState

    val mergedStore2: SymbolicStore = updateNestedITEExpressions(globalMergePointExecutionStateToUse, identifiersToReplace, mergedStore, node, globalPath)

    val itePredicatesFixed = updateITEPredicates(globalMergePointExecutionStateToUse, node, mergedStore2, globalPathsToChange)

    // Updates the store updates in the constraint, but does not update the constraint itself. Should: 1) replace identifiers in store updates of constraint, 2) apply those store updates to store, 3) replace identifiers in constraint itself by identifiers' values in new store
    val nodeConstraintWithStoreUpdatesIdentifiersReplaced = constraintESHasStoreUpdates.replaceIdentifiersWith(
      node.constraint, itePredicatesFixed)
    val nodeStore: SymbolicStore = constraintESHasStoreUpdates.applyToStore(
      nodeConstraintWithStoreUpdatesIdentifiersReplaced, mergedStore)

    val oldNodeWithNewConstraintsStoreUpdates = node.replaceConstraint(nodeConstraintWithStoreUpdatesIdentifiersReplaced)

    val thenExitIdentifiers = exitIdentifiers ++ oldNodeWithNewConstraintsStoreUpdates.thenBranch.getExitedIdentifiers
    val elseExitIdentifiers = exitIdentifiers ++ oldNodeWithNewConstraintsStoreUpdates.elseBranch.getExitedIdentifiers

    // Replace the identifiers in the constraint itself with the values found in the new store
    val (replacedConstraint, updatedNodeStore) = replaceBranchConstraint(nodeStore, oldNodeWithNewConstraintsStoreUpdates, exitIdentifiers)

    val newGlobalPathsToChange =
      if (isGlobalMergePoint) {
      Nil
    } else {
      globalPathsToChange
    } ++ (replacedConstraint match {
      case EventConstraintWithExecutionState(bc: BranchConstraint, _) =>
        if (node.getUsedAsITEPredicateFor.nonEmpty) {
          List(ITEPredicateUpdateInformation(globalPath, node.getUsedAsITEPredicateFor.head, node.getUsedAsITEPredicateFor.tail.head, bc.exp))
        } else {
          Nil
        }
      case _ => Nil
    })

    val (thenEdgeWithStoreUpdatesIdentifiersReplaced, newThenStore) =
      updateITEsAndApplyStoreUpdates(oldNodeWithNewConstraintsStoreUpdates.thenBranch, updatedNodeStore)
    val (elseEdgeWithStoreUpdatesIdentifiersReplaced, newElseStore) =
      updateITEsAndApplyStoreUpdates(oldNodeWithNewConstraintsStoreUpdates.elseBranch, updatedNodeStore)

    val mergedNode = oldNodeWithNewConstraintsStoreUpdates.replaceConstraint(replacedConstraint)
    oldNodeWithNewConstraintsStoreUpdates.thenBranch.to.addChildSetter(mergedNode, SetChild(mergedNode, ThenDirection))
    oldNodeWithNewConstraintsStoreUpdates.elseBranch.to.addChildSetter(mergedNode, SetChild(mergedNode, ElseDirection))
    val parentsChildSetters: Map[SymbolicNode[ConstraintES], Set[SetChild[ConstraintES]]] = mergedNode.getParentsChildSetters
    parentsChildSetters.foreach(tuple => {
      val childSetters = tuple._2
      childSetters.foreach(childSetter => childSetter.setChild(mergedNode))
    })
    updateStopTestingTargetsCache(replacedConstraint, mergedNode)

    def descendIntoThen(): Unit = {
      val globalPathToThen = if (isGlobalMergePoint) List(ThenDirection) else globalPath :+ ThenDirection
      val mergedThenNode = replaceSingleNode(
        globalMergePointExecutionStateToUse, newThenStore, identifiersToReplace, mergedNode.thenBranch.to,
        thenExitIdentifiers, globalPathToThen, newGlobalPathsToChange)
      mergedThenNode.removeChildSetters(oldNodeWithNewConstraintsStoreUpdates)
      mergedNode.thenBranch.to.getParentsChildSetters.foreach(tuple =>
        tuple._2.foreach(childSetter => childSetter.setChild(mergedThenNode)))
      mergedNode.thenBranch = mergedNode.thenBranch.copy(
        to = mergedThenNode, storeUpdates = thenEdgeWithStoreUpdatesIdentifiersReplaced.storeUpdates)
      mergedThenNode.removeChildSetters(oldNodeWithNewConstraintsStoreUpdates)
      mergedThenNode.addChildSetter(mergedNode, SetChild(mergedNode, ThenDirection))
    }

    def descendIntoElse(): Unit = {
      val globalPathToElse = if (isGlobalMergePoint) List(ElseDirection) else globalPath :+ ElseDirection
      var mergedElseNode = replaceSingleNode(
        globalMergePointExecutionStateToUse, newElseStore, identifiersToReplace, mergedNode.elseBranch.to,
        elseExitIdentifiers, globalPathToElse, newGlobalPathsToChange)

      /*
     * If the constraint being merged now is a TargetChosen constraint which originally had an Unsatisfiable
     * else-branch, then make sure the new else-branch is still unsatisfiable. This is a concern because when
     * an UnexploredNode is merged with an UnsatisfiableNode, an UnexploredNode is generated.
     */
      replacedConstraint match {
        case EventConstraintWithExecutionState(_: TargetChosen, _) =>
          if ((mergedNode.elseBranch.to == UnsatisfiableNode()) && mergedElseNode != UnsatisfiableNode()) {
            mergedElseNode = UnsatisfiableNode()
          }
        case _ =>
      }

      mergedElseNode.removeChildSetters(oldNodeWithNewConstraintsStoreUpdates)
      mergedNode.elseBranch.to.getParentsChildSetters.foreach(tuple =>
        tuple._2.foreach(childSetter => childSetter.setChild(mergedElseNode)))
      mergedNode.elseBranch = mergedNode.elseBranch.copy(
        to = mergedElseNode, storeUpdates = elseEdgeWithStoreUpdatesIdentifiersReplaced.storeUpdates)
      mergedElseNode.removeChildSetters(oldNodeWithNewConstraintsStoreUpdates)
      //    mergedNode.elseBranch = ElseEdge.to(mergedElseNode).merge(tElseBranch).merge(eElseBranch)
      mergedElseNode.addChildSetter(mergedNode, SetChild(mergedNode, ElseDirection))
    }

    def descend(): Unit = {
      descendIntoThen()
      descendIntoElse()
    }


//    if (optBarrier.exists(_.executionState == mergedNode.constraint.executionState)) {
//      val newOptBarrier = optBarrier.map(_.arrivedAt)
//      newOptBarrier.foreach(barrier => assert(barrier.remaining >= 0))
//      optBarrier = newOptBarrier
//    }

    optBarrier match {
      case Some(barrier) =>
        if (isGlobalMergePoint && barrier.executionState == mergedNode.constraint.executionState && barrier.remaining == 0) {
          optBarrier = makeNewBarrier(mergedNode.thenBranch.to)
          descend()
        } else if (isGlobalMergePoint && barrier.executionState == mergedNode.constraint.executionState) {
          // Do not descend
        } else {
          descend()
        }
      case None => descend()
    }

    mergedNode
  }

  /**
    * ITE expressions used as right-hand-side expressions in this node's constraint's store update
    * may have been created while merging an ancestor of this node. If that merged ancestor was merged again with
    * another path, its corresponding ITE expression will have been expanded. The "old" ITE expression in this node's
    * update should therefore be replaced with the "new", expanded ITE.
    */
  /**
    * Assume that each identifier is mapped to an ITE of a certain "colour", according to the "layer" of the part
    * of the tree from which it was generated, e.g., the first layer (all nodes from the root to the first merged node)
    * of the tree generates red ITEs, the second blue ITEs etc. If, at some point, a new path is added in the first
    * layer which is merged with the first layer's merged node and if a new value is assigned to a variable x along
    * that path, x's red ITE will be expanded with this new value. However, this also means that the the red ITE
    * will have to be updated inside the blue ITE for all expressions/updates using that blue ITE in the second
    * layer or other layers below.
    */

  protected def prefixOf(path1: Path, path2: Path): Path = {
    @tailrec
    def loop(path1: Path, path2: Path, prefix: Path): Path = {
      if (path1.nonEmpty && path2.nonEmpty &&
        path1.head == path2.head) {
        loop(path1.tail, path2.tail, prefix :+ path1.head)
      } else {
        prefix
      }
    }
    loop(path1, path2, Nil)
  }

  protected def updateITEPredicate(
    globalPathToNode1: Path,
    globalPathToNode2: Path,
    newPredicateExp: BooleanExpression,
    iteExp: SymbolicExpression,
    node: StoresVirtualPaths
  ): SymbolicExpression = {
    val optITEPath1 = node.getITEPath(globalPathToNode1)
    val optITEPath2 = node.getITEPath(globalPathToNode2)
    (optITEPath1, optITEPath2) match {
      case (None, None) => iteExp
      case (Some(itePath1), Some(itePath2)) =>
        if (itePath1 == itePath2) {
          // No ITE has been constructed yet for these paths
          node.updateITEPredicateFor(itePath1, itePath2, newPredicateExp)
          node.updateSavedExp(itePath1, itePath2, iteExp)
          iteExp
        } else {
          val prefixITEPath = prefixOf(itePath1, itePath2)
          SymbolicITEReplacer.replacePredicateIn(iteExp, newPredicateExp, prefixITEPath)
        }
      case (None, _) => iteExp
      case (_, None) => iteExp
    }
  }

  protected def updateNestedITEExpressions(
    globalMergePointExecutionState: ExecutionState,
    identifiersToReplace: Iterable[String],
    mergedStore: SymbolicStore,
    node: BranchSymbolicNode[ConstraintES],
    globalPath: Path
  ): SymbolicStore = {
    if (node.hasBeenMerged) {
      val storeUpdates = constraintESHasStoreUpdates.storeUpdates(node.constraint)
      assert(storeUpdates.size == 1)
      val storeUpdate = storeUpdates.head.asInstanceOf[AssignmentsStoreUpdate]
      if (isGlobalMergePoint(node)) {
        storeUpdate.assignments.map(assignment => {
          val (identifier, value) = assignment
          if (identifiersToReplace.exists(_ == identifier)) {
            val virtualPathStore = CentralVirtualPathStore.getStore(globalMergePointExecutionState).getVirtualPathStoreForIdentifier(identifier)
            val optItePath = virtualPathStore.getITEPath(globalPath)
            optItePath match {
              case None => (identifier, value)
              case Some(itePath) =>
                val newValue = SymbolicITEReplacer.replaceIn(assignment._2, mergedStore(identifier), itePath)
                (identifier, newValue)
            }
          } else {
            (identifier, value) // identifier does not have to be replaced, so do change the assignment to identifier
          }
        }).toMap
      } else {
        // Node is not a global merge point, but was still previously merged and hence has a store update in its
        // constraint. Hence, return the store generated via this store update as is, without changing any of
        // the expressions in it.
        storeUpdate.assignments.toMap
      }
    } else {
      mergedStore
    }
  }

  protected def updateITEsAndApplyStoreUpdates[S : HasStoreUpdates](s: S, replaceWith: SymbolicStore): (S, SymbolicStore) = {
    val (replacedS, newStore) = implicitly[HasStoreUpdates[S]].replaceAndApply(s, replaceWith)
//    val newThenStore: SymbolicStore = implicitly[HasStoreUpdates[S]].applyToStore(replaced, replaceWith)
    (replacedS, newStore)
  }

  private def mergeOrRetrieveSingleBranchNode(
    globalMergePointExecutionState: ExecutionState,
    mergedStore: SymbolicStore,
    identifiersToReplace: Iterable[String],
    node: BranchSymbolicNode[ConstraintES],
    edgeExitIdentifiers: Set[String],
    globalPath: Path,
    globalPathsToChange: List[ITEPredicateUpdateInformation]
  ): MergedBranch =
    bsnCache.getCachedNode(node) match {
      case Some(mergedNode) => mergedNode
      case None =>
        val mergedNode = replaceSingleBranchNode(
          globalMergePointExecutionState, mergedStore, identifiersToReplace, node, edgeExitIdentifiers, globalPath,
          globalPathsToChange)
        val result: MergedBranch = (mergedNode, mergedStore)
        bsnCache.addCachedNode(node, result)
        result
    }

  private def mergeOrRetrieveTwoBranchNodes(
    ancestorStore: SymbolicStore,
    globalMergePointExecutionState: ExecutionState,
    mergingPredicate: List[ConstraintES],
    node1Info: PathInformation[ConstraintES, BranchSymbolicNode[ConstraintES]],
    node2Info: PathInformation[ConstraintES, BranchSymbolicNode[ConstraintES]],
    edgeExitIdentifiers: Set[String],
    identifiersAssigned: Set[String]
  )
  : (BranchSymbolicNode[ConstraintES], SymbolicStore, Option[List[String]]) =
    bsnCache.getCachedNode(node1Info.node, node2Info.node) match {
      case Some(mergedNode) => (mergedNode._1, mergedNode._2, None)
      case None =>
        val mergedNode = mergeTwoBranchNodes(
          ancestorStore,
          globalMergePointExecutionState,
          mergingPredicate,
          node1Info,
          node2Info,
          edgeExitIdentifiers,
          identifiersAssigned)
        bsnCache.addCachedNode(node1Info.node, node2Info.node, (mergedNode._1, mergedNode._2))
        (mergedNode._1, mergedNode._2, Some(mergedNode._3))
    }

}
