package backend.tree.merging

import backend.Path
import backend.execution_state._
import backend.execution_state.store._
import backend.logging._
import backend.reporters.{ConstraintESReporter, SetChild}
import backend.solvers._
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints._
import backend.tree.follow_path._
import backend.tree.path.SymJSState
import backend.tree.search_strategy._

import scala.annotation.tailrec

trait NodeMerger[C <: Constraint]

case object SpecialConstraintESNegater extends ConstraintNegater[ConstraintES] {
  override def negate(constraint: ConstraintES): ConstraintES = constraint match {
    case c@EventConstraintWithExecutionState(tc: TargetChosen, _) =>
      val negatedTc = TargetNotChosen.inverseTargetChosen(tc)
      c.copy(ec = negatedTc)
    case _ => implicitly[ConstraintNegater[ConstraintES]].negate(constraint)
  }
}

case class MergingState(
  prefixPath: Path,
  suffixExistingPath: Path,
  state: ExecutionState,
  store: SymbolicStore
)

class ConstraintESNodeMerger(val writer: TreeLogger, reporter: ConstraintESReporter)(
  implicit val x: ToBooleanExpression[ConstraintES]
)
  extends NodeMerger[ConstraintES] {

  val pathFollower = new ConstraintESNoEventsPathFollower[ConstraintES] // new ConstraintESPathFollower
  val storeMerger = new SymbolicStoreMerger
  val individualNodesMerger = new IndividualNodesMerger(storeMerger, writer.asInstanceOf[TreeLogger], reporter)

  def mergeWorkListStates(
    root: SymbolicNode[ConstraintES],
    pathConstraintToMerge: TreePath[ConstraintES],
    executionState: ExecutionState,
    optEndNode: Option[SymbolicNode[ConstraintES]] = None
  ): SolverResult = {

//    val startFrom: SymbolicNode[ConstraintES] = executionState match {
//      case CodeLocationExecutionState(_, _, optSequence) =>
//        optSequence match {
//          case None => root
//          case Some(sequence) =>
//            // Start from the StopTestingTargets node above the one that should be merged
//            val lastEventId = sequence.length - 2
//            StopTestingTargetsCacher
//              .get(lastEventId)
//              .getOrElse(root)
//              .asInstanceOf[SymbolicNode[ConstraintES]]
//        }
//      case TargetEndExecutionState(id) =>
//        // Start from the StopTestingTargets node above the one that should be merged
//        StopTestingTargetsCacher
//          .get(id - 1)
//          .getOrElse(root)
//          .asInstanceOf[SymbolicNode[ConstraintES]]
//    }

    val startFrom = root

    case class ToBeMerged(
      maybeMergedPreviously: PathAndNode[ConstraintES],
      notMergedYet: PathAndNode[ConstraintES]
    )
    def orderPathsToBeMerged(
      reached1: PathAndNode[ConstraintES],
      reached2: PathAndNode[ConstraintES]
    ): Option[ToBeMerged] = {
      val reachedNode1 = reached1.lastNode
      val reachedNode2 = reached2.lastNode

      if (reachedNode1.hasBeenMerged && reachedNode2.hasBeenMerged) {
        None
      } else if (reachedNode1.hasBeenMerged) {
        Some(ToBeMerged(reached1, reached2))
      } else if (reachedNode2.hasBeenMerged) {
        Some(ToBeMerged(reached2, reached1))
      } else {
        // Order doesn't matter
        Some(ToBeMerged(reached1, reached2))
      }
    }

    @tailrec
    def findPathFromGlobalMergePoint(
      globalMergePoint: SymbolicNode[ConstraintES],
      treePath: TreePath[ConstraintES]
    ): TreePath[ConstraintES] = {
      // globalMergePoint should definitely exist in the original list of visited nodes of treePath
      if (treePath.original.head == globalMergePoint) {
        treePath
      } else {
        findPathFromGlobalMergePoint(globalMergePoint, treePath.tail)
      }
    }

    @tailrec
    def loop(statesMerged: Int): Int = {
      Logger.d(s"Looping for ${statesMerged + 1}th time")
      println("Selecting paths to merge")
      val workListFinder: WorkListFinder[ConstraintES] = new ConstraintESWorkListFinderFollowPath(
        pathConstraintToMerge, executionState, optEndNode)
      var previousWorklistSize: Option[Int] = None
      val workListFound = workListFinder.findWorkList(startFrom)
      if (workListFound.isEmpty) {
        // No suitable paths found to merge with, so return early
        return statesMerged
      }

      val endNode: SymbolicNode[ConstraintES] = getNode(root, emptyStore, pathConstraintToMerge.getEdges)._1
      val firstWorklistItem = workListFound.head
      val reducedTreePath = findPathFromGlobalMergePoint(firstWorklistItem.lastGlobalMergePoint, pathConstraintToMerge)
      val workList = workListFound + PathAndNode(firstWorklistItem.pathToGlobalMergePoint, firstWorklistItem.lastGlobalMergePoint, reducedTreePath, endNode)
      val emptyPathsRemoved = workList.filter(_.treePath.getPath.nonEmpty).toList

      @tailrec
      def constructTuples(
        lst: List[PathAndNode[ConstraintES]],
        acc: List[ToBeMerged]
      ): List[ToBeMerged] = lst.headOption match {
        case None => acc
        case Some(reached1) =>
          val toAdd = lst.tail.foldLeft[List[ToBeMerged]](Nil)((acc, reached2) => {
            if (reached1.lastNode != reached2.lastNode) {
              val optOrder = orderPathsToBeMerged(reached1, reached2)
              optOrder.fold(acc)(_ :: acc)
            } else {
              acc
            }
          })
          constructTuples(lst.tail, toAdd ++ acc)
      }

      val combinedPaths = constructTuples(emptyPathsRemoved, Nil)
      val cprCombinedPathsTuples = combinedPaths.map(tuple => {
        val commonPrefixResult = TreePath.findCommonPrefix(tuple.maybeMergedPreviously.treePath, tuple.notMergedYet.treePath)
        (commonPrefixResult, tuple)
      })

      def hasChildren(node: SymbolicNode[ConstraintES]): Boolean = node match {
        case BranchSymbolicNode(_, thenEdge, elseEdge) =>
          val hasThenChild = thenEdge.to match {
            case _: BranchSymbolicNode[ConstraintES] => true
            case _ => false
          }
          val hasElseChild = elseEdge.to match {
            case _: BranchSymbolicNode[ConstraintES] => true
            case _ => false
          }
          hasThenChild || hasElseChild
        case _ => false
      }

      val usedStatesFiltered = cprCombinedPathsTuples.filter(cprCombinedPathsTuple => {
        val cpr = cprCombinedPathsTuple._1
        val pathAndNode = cprCombinedPathsTuple._2
        val (lastCommonAncestorNode, commonAncestorStore, _) = getNode(pathAndNode.maybeMergedPreviously.lastGlobalMergePoint, emptyStore, cpr.prefix.getEdges)
        assert(lastCommonAncestorNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
        val castedAncestor = lastCommonAncestorNode.asInstanceOf[BranchSymbolicNode[ConstraintES]]
        val isAllowed = !castedAncestor.hasBeenUsedAsITEPredicateForExecutionStates(executionState)

        val maybeMergedPreviously = cprCombinedPathsTuple._2.maybeMergedPreviously.lastNode
        val notMergedYet = cprCombinedPathsTuple._2.notMergedYet.lastNode
        val oneOfEitherDoesNotHaveChildren = !(hasChildren(maybeMergedPreviously) && hasChildren(notMergedYet))

        isAllowed && oneOfEitherDoesNotHaveChildren
      })

      // Filter out tuples where paths cross each other at some previously-merged node
//      val suffixPassByMergedNodeFiltered = usedStatesFiltered.filter(cprCombinedPathsTuple => {
//        val cpr = cprCombinedPathsTuple._1
//        !cpr.suffixPath2.original.tail.exists(node => cpr.suffixPath1.original.tail.contains(node))
//      })

      val finalSelection = usedStatesFiltered

      //
      //      val combinedPathsSorted = usedStatesFiltered.sorted(OrderPathsBySuffix)

      //      val workListSize = workList.size
      //      if (previousWorklistSize.exists(_ < 2)) {
      //        return statesMerged
      //      }

      previousWorklistSize = Some(finalSelection.size)
      //      if (workList.nonEmpty) {
      //        Logger.d(s"WORKLIST FOR $executionState NOT EMPTY")
      //      } else if (workListSize == 1) {
      //        Logger.d(s"WORKLIST FOR $executionState CONTAINED ONLY 1 ELEMENT")
      //      }

      if (finalSelection.isEmpty) {
        statesMerged
      } else {
        println("Found the paths to merge")
        val lastGlobalMergePoint = finalSelection.head._2.maybeMergedPreviously.lastGlobalMergePoint
        val pathToLastGlobalMergePoint = finalSelection.head._2.maybeMergedPreviously.pathToGlobalMergePoint
        val (_, startingStore, _) = getNode(root, emptyStore, pathToLastGlobalMergePoint.getEdges)
        mergeTwoPaths(
          root,
          executionState,
          lastGlobalMergePoint,
          startingStore,
          finalSelection.head._1,
          finalSelection.head._2.maybeMergedPreviously.treePath,
          finalSelection.head._2.notMergedYet.treePath)
        writer.d(root)
        loop(statesMerged + 1)
      }
    }

    writer.d(root)
    Logger.v("+++ Starting merge attempt +++")
    val statesMerged = loop(0)
    writer.n(root)
    Logger.v("--- Finishing merge attempt ---")
    if (statesMerged == 0) {
      ActionNotApplied("Did not find two states with same execution state")
    } else {
      ActionSuccessful
    }
  }

  private def mergeTwoPaths(
    root: SymbolicNode[ConstraintES], // Only used to determine whether the globalMergePoint is the root
    executionState: ExecutionState,
    globalMergePoint: SymbolicNodeWithConstraint[ConstraintES],
    startingStore: SymbolicStore,
    commonPrefixOfPaths: CommonPrefixResult[ConstraintES],
    path1: TreePath[ConstraintES],
    path2: TreePath[ConstraintES]
  ): Unit = {

    Logger.d(s"MergeTwoPaths called with (${path1.getPath}, ${path2.getPath})")

    def updateExistingMergedNodes(
      pathNode: SymbolicNode[ConstraintES],
      parentNode: SymbolicNode[ConstraintES],
      childSetter: SetChild[ConstraintES],
      updatedMergedNode: SymbolicNode[ConstraintES]
    ): Unit = {

      def invokeSetChild(
        childSetter: SetChild[ConstraintES],
        updatedMergedNode: SymbolicNode[ConstraintES],
        storeUpdates: Iterable[StoreUpdate]
      ): Unit = {
        var skip = Set[SymbolicNode[ConstraintES]]()

        def loop(node: SymbolicNode[ConstraintES]): Unit = {
          if (node.isInstanceOf[BranchSymbolicNode[ConstraintES]] && skip.contains(node)) {
            return
          }
          skip += node
          node match {
            case RegularLeafNode() | UnexploredNode() | UnsatisfiableNode() | SafeNode(_) |
                 MergedNode() =>
              node.getParentsChildSetters.foreach(tuple =>
                tuple._2.foreach(childSetter => childSetter.setChild(node)))
            case BranchSymbolicNode(_, thenBranch, elseBranch) =>
              loop(thenBranch.to)
              loop(elseBranch.to)
              node.getParentsChildSetters.foreach(tuple =>
                tuple._2.foreach(childSetter => childSetter.setChild(node)))
          }
        }

        loop(updatedMergedNode)
        childSetter.setChild(updatedMergedNode)
        updatedMergedNode.addChildSetter(parentNode, childSetter)
      }

      // parentNode can be connected with pathNode via more than 1 edge. Each of these edges has to be transferred to the merged node
      val allChildSettersOfParent = pathNode.getParentsChildSetters.getOrElse(parentNode, Set())
      // Two childSetters are being called to set the child of parentNode to the new, merged node.
      // However, pathNode is not guaranteed to have any child-setters of parentNode, because only nodes that were
      // created via merging have child-setters; "normal" nodes (nodes created by just inserting the path constraint
      // to the tree) do not have any. So if pathNode is a "normal" node, childSetters has to be used. If pathNode
      // is a merged node, it will be set as the child of parentNode twice, but this shouldn't be a problem.
      invokeSetChild(
        childSetter,
        updatedMergedNode,
        childSetter.existingEdge.map(_.storeUpdates).getOrElse(Nil))
      allChildSettersOfParent.foreach(childSetter =>
        invokeSetChild(childSetter, updatedMergedNode, Nil))
      //      mergedNode.addChildSetter(parentNode, childSetter)
      pathNode.removeChildSetters(parentNode)
      val allPathNodeParentsChildSetters = pathNode.getParentsChildSetters
      allPathNodeParentsChildSetters.foreach(tuple => {
        tuple._2.foreach(childSetter => {
          childSetter.setChild(updatedMergedNode)
          updatedMergedNode.addChildSetter(tuple._1, childSetter)
        })
        pathNode.removeChildSetters(tuple._1)
      })
    }

    val prefix = commonPrefixOfPaths.prefix
    val suffix1 = commonPrefixOfPaths.suffixPath1
    val suffix2 = commonPrefixOfPaths.suffixPath2
    Logger.d(s"MergeTwoPaths merging (${path1.getPath}, ${path2.getPath})")

    val (commonAncestor, commonAncestorStore, _) = getNode(
      globalMergePoint, startingStore, prefix.getEdges) // SymJSPath pointing to the last common ancestor of treePath1 and treePath2
    val castedAncestor = commonAncestor.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    if (suffix1.getPath.isEmpty || suffix2.getPath.isEmpty) {
      return // Don't merge along the same path. Nodes are along the same path if either suffix is empty
    }

    val (parentOfPath1Node, _, _) = getNode(
      globalMergePoint, startingStore, path1.getEdges.init) // Alternatively: getNode(commonAncestor, suffix1.getPath.init)
    val path1NodeParentToPath1NodeDirection = suffix1.getPath.last // Alternatively: treePath1.getPath.last
    val path1ParentChildSetter = SetChild(parentOfPath1Node, path1NodeParentToPath1NodeDirection)
    val (parentOfPath2Node, _, _) = getNode(
      globalMergePoint, startingStore, path2.getEdges.init) // Alternatively: getNode(commonAncestor, suffix2.getPath.init)
    val path2NodeParentToPath2NodeDirection = path2.getPath.last
    val path2ParentChildSetter = SetChild(parentOfPath2Node, path2NodeParentToPath2NodeDirection)

    // Path 1 likely leads to a merged node, with store updates in the constraint. Although we want to apply
    // these store updates to obtain the path1NodeStore (which will include ITE expressions from previous merges)
    // we don't want to include the assigned identifiers in that constraint's updates because otherwise
    // *any* identifier which was assigned at some point along a path leading to the merged node will
    // be included in the path1IdentifiersAssigned
    val (path1Node, path1NodeStore, path1IdentifiersAssigned) = getNode(
      commonAncestor, commonAncestorStore, suffix1.getEdges)
    val (path2Node, path2NodeStore, path2IdentifiersAssigned) = getNode(
      commonAncestor, commonAncestorStore, suffix2.getEdges)

    def derivePath(ancestorNode: BranchSymbolicNode[ConstraintES]): List[ConstraintES] = {
      val EventConstraintWithExecutionState(constraint, state) = ancestorNode.constraint
      // In case constraint is a TargetChosenConstraint (or related), force conversion to a BooleanExpression version of this constraint and convert to a BranchConstraint
      val exp = implicitly[ToBooleanExpression[EventConstraint]].toBoolExp(constraint).get
      val storeUpdates = implicitly[HasStoreUpdates[EventConstraint]].storeUpdates(constraint)
      val reconstructedConstraint =
        EventConstraintWithExecutionState(BranchConstraint(exp, storeUpdates), state)
      List(reconstructedConstraint)
    }

    val mergingPredicate = derivePath(castedAncestor)
    Logger.d(s"Merging predicate: $mergingPredicate")
    castedAncestor.usedAsITEPredicateFor(executionState, path1, path2)

    // TODO Take union of path1IdentifiersAssigned and path2IdentifiersAssigned or pass both sets and resolve it differently?
    // If taking union, there might be a bug if one path does not assign to an identifier (i.e., value of the identifier
    // remains the same as with the ancestorStore), while the other path does assign to the identifier
    val (mergedNode, mergedStore, optMergedIdentifiers) = individualNodesMerger.mergeTwoNodes(
      commonAncestorStore,
      globalMergePoint.constraint.executionState,
      mergingPredicate,
      PathInformation(executionState, path1Node, path1, path1NodeStore),
      PathInformation(executionState, path2Node, path2, path2NodeStore),
      Set(),
      path1IdentifiersAssigned.union(path2IdentifiersAssigned)
    )
    val mergedIdentifiers = optMergedIdentifiers.get

    // merge (store constraints of) the edges of parentOfPath1Node and parentOfPath2Node here and let them point to mergedNode

    val onlyMergedIdentifiersIncluded =
      mergedIdentifiers.foldLeft[Assignments](Nil)((assignments, identifier) => {
        assignments :+ (identifier, mergedStore(identifier))
      })
    // Add assignments for all of the identifiers in the merged store.
    // In the merged store, these identifiers might have different values than
    // those in the parent store. We have to override these identifiers so that, no matter the path followed to
    // the merged node, its merged store contains merged values.
    val assignmentConstraints = AssignmentsStoreUpdate(onlyMergedIdentifiersIncluded)

    writer.d(root)

    updateExistingMergedNodes(
      path2Node,
      parentOfPath2Node,
      path2ParentChildSetter,
      mergedNode)
    writer.d(root)

    updateExistingMergedNodes(
      path1Node,
      parentOfPath1Node,
      path1ParentChildSetter,
      mergedNode)

    mergedNode.flagMergedForES(executionState)
    castedAncestor.usedForMerging(executionState)
    CentralMergedPathRegistry.addPath(executionState, path1.getPath)
    CentralMergedPathRegistry.addPath(executionState, path2.getPath)
    Logger.d(s"Ancestor node: $castedAncestor")
    writer.d(root)
  }

  private def getNodeWithoutLastIdentifiersAssigned(
    root: SymbolicNode[ConstraintES],
    symbolicStore: SymbolicStore,
    edges: List[EdgeWithoutTo[ConstraintES]]
  ): (SymbolicNode[ConstraintES], SymbolicStore, Set[String]) = {
    val symJsState = SymJSState(Nil, edges)
    val optPathFollowed = pathFollower.followPathWithAssignments(root, symbolicStore, symJsState, false)
    optPathFollowed match {
      case None => throw new Exception("Path could not be followed")
      case Some(pathFollowed) =>
        (pathFollowed.pathFollowed.endNode,
          pathFollowed.pathFollowed.store,
          pathFollowed.identifiersAssigned)
    }
  }

  private def getNode(
    startFrom: SymbolicNode[ConstraintES],
    symbolicStore: SymbolicStore,
    edges: List[EdgeWithoutTo[ConstraintES]]
  ): (SymbolicNode[ConstraintES], SymbolicStore, Set[String]) = {
    val symJsState = SymJSState(Nil, edges)
    val optPathFollowed = pathFollower.followPathWithAssignments(startFrom, symbolicStore, symJsState)
    optPathFollowed match {
      case None =>
        throw new Exception("Path could not be followed")
      case Some(pathFollowed) =>
        (pathFollowed.pathFollowed.endNode,
          pathFollowed.pathFollowed.store,
          pathFollowed.identifiersAssigned)
    }
  }

  private[merging] def canMergeNode(node: SymbolicNode[ConstraintES]): Boolean = {
    def canMergeConstraint(constraint: ConstraintES): Boolean = constraint match {
      case c: EventConstraintWithExecutionState =>
        c.ec match {
          case _: TargetChosen | _: StopTestingTargets => false
          case _ => true
        }
      case _ => true
    }

    node match {
      case _: UnsatisfiableNode[ConstraintES] | _: SafeNode[ConstraintES] |
           _: UnexploredNode[ConstraintES] =>
        false
      case BranchSymbolicNode(constraint, _, _) => canMergeConstraint(constraint)
      case _ => true
    }
  }

}
