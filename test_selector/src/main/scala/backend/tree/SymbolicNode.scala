package backend.tree

import backend.{Main, Path}
import backend.execution_state.ExecutionState
import backend.execution_state.store._
import backend.expression.BooleanExpression
import backend.reporters.SetChild
import backend.tree.constraints._
import backend.tree.follow_path._
import backend.tree.merging._
import backend.tree.search_strategy.TreePath

sealed abstract class SymbolicNode[ConstraintType <: Constraint] {
  protected var parentsChildSetters: Map[SymbolicNode[ConstraintType], Set[SetChild[ConstraintType]]] = Map()
  def removeChildSetters(parent: SymbolicNode[ConstraintType]): Unit = {
    parentsChildSetters = parentsChildSetters - parent
  }
  def mergeChildSetters(otherNode: SymbolicNode[ConstraintType]): Unit = {
    otherNode.getParentsChildSetters.foreach(tuple => {
      tuple._2.foreach(childSetter => this.addChildSetter(tuple._1, childSetter))
    })
  }
  def getParentsChildSetters: Map[SymbolicNode[ConstraintType], Set[SetChild[ConstraintType]]] = parentsChildSetters
  def addChildSetter(parent: SymbolicNode[ConstraintType], setChild: SetChild[ConstraintType]): Unit = {
    parentsChildSetters = parentsChildSetters.updated(parent, parentsChildSetters.getOrElse(parent, Set()) + setChild)
  }
  def flagMergedForES(executionState: ExecutionState): Unit = {}
  def getMergedExecutionStates: Set[ExecutionState] = Set()
  def hasBeenMerged: Boolean = false
  def hasBeenMergedForES(executionState: ExecutionState): Boolean = hasBeenMerged
}

sealed trait SymbolicNodeWithConstraint[ConstraintType <: Constraint]
  extends SymbolicNode[ConstraintType] {
  def constraint: ConstraintType

  def replaceConstraint(
    newConstraint: ConstraintType
  ): SymbolicNodeWithConstraint[ConstraintType]
}

trait OffersVirtualPath {
  def getVirtualPathStoreForIdentifier(identifier: String): StoresVirtualPaths
  def getAllVirtualPathStores: Iterable[(String, StoresVirtualPaths)]
}

sealed trait EdgeWithoutTo[ConstraintType <: Constraint] {
  def toDirection: Direction
  def storeUpdates: Iterable[StoreUpdate]
}
case class ThenEdgeWithoutTo[ConstraintType <: Constraint](storeUpdates: Iterable[StoreUpdate])
  extends EdgeWithoutTo[ConstraintType] {
  override def toDirection: Direction = ThenDirection

  protected def replaceStoreUpdates(newStoreUpdates: Iterable[StoreUpdate]): ThenEdgeWithoutTo[ConstraintType] = {
    copy(storeUpdates = newStoreUpdates)
  }
}
case class ElseEdgeWithoutTo[ConstraintType <: Constraint](storeUpdates: Iterable[StoreUpdate])
  extends EdgeWithoutTo[ConstraintType] {
  override def toDirection: Direction = ElseDirection

  protected def replaceStoreUpdates(newStoreUpdates: Iterable[StoreUpdate]): ElseEdgeWithoutTo[ConstraintType] = {
    copy(storeUpdates = newStoreUpdates)
  }
}

sealed trait Edge[ConstraintType <: Constraint] {
  protected var id: Int = Edge.newId
  def to: SymbolicNode[ConstraintType]
  def toDirection: Direction
  def storeUpdates: Iterable[StoreUpdate]
  def edgeId: Int = id
  def getExitedIdentifiers: Set[String] = {
    storeUpdates.foldLeft(Set[String]())({
      case (set, ec: ExitScopeUpdate) =>
        set.union(ec.identifiers)
      case (set, _) => set
    })
  }
}

object Edge {
  private var edgeId: Int = 0
  def newId: Int = {
    val temp = edgeId
    edgeId += 1
    temp
  }
}

private object EdgeMerger {
  def mergeStoreConstraints(
    predExp: BooleanExpression,
    storeConstraints1: Iterable[StoreUpdate],
    storeConstraints2: Iterable[StoreUpdate]
  ): Iterable[StoreUpdate] = {
    if (storeConstraints1.isEmpty) {
      storeConstraints2
    } else if (storeConstraints2.isEmpty) {
      storeConstraints1
    } else {
      storeConstraints1 // TODO?
//      assert(storeConstraints1.size == storeConstraints2.size)
//      storeConstraints1.zip(storeConstraints2).foldLeft(Nil: List[StoreUpdate])((list, tuple) => tuple match {
//        case (left: ExitScopeUpdate, right: ExitScopeUpdate) =>
//          assert(left == right)
//          list :+ left
//        case (left: EnterScopeUpdate, right: EnterScopeUpdate) =>
//          assert(left == right)
//          list :+ left
//        case (left: AssignmentsStoreUpdate, right: AssignmentsStoreUpdate) =>
////          val l = left.assignments
////          val leftKeys = left.identifiers.toSet
////          val r = right.assignments
////          val rightKeys = right.identifiers.toSet
////          assert(leftKeys == rightKeys)
////          val commonKeys = leftKeys.intersect(rightKeys)
////          val separateValues: Map[String, SymbolicExpression] = (l -- rightKeys) ++ (r -- leftKeys)
////          val mergedValues = commonKeys.foldLeft(Map[String, SymbolicExpression]())((map, key) => {
////            val mergedValue = ExpressionSubstituter.merge(l(key), predExp, Map(key -> r(key)), ???, ???, ???, ???)
////            map + (key -> mergedValue)
////          })
////          list :+ AssignmentsStoreUpdate(separateValues ++ mergedValues)
//          ???
//      })
    }
  }
}

case class ThenEdge[ConstraintType <: Constraint](
  storeUpdates: Iterable[StoreUpdate],
  to: SymbolicNode[ConstraintType]
)
  extends Edge[ConstraintType] {
  override def toDirection: Direction = ThenDirection
  def merge(predExp: BooleanExpression, other: Edge[ConstraintType]): ThenEdge[ConstraintType] = {
    this.copy(EdgeMerger.mergeStoreConstraints(predExp, storeUpdates, other.storeUpdates))
  }
  protected def replaceStoreUpdates(newStoreUpdates: Iterable[StoreUpdate]): ThenEdge[ConstraintType] = {
    copy(storeUpdates = newStoreUpdates)
  }
  def copy(
    storeUpdates: Iterable[StoreUpdate] = this.storeUpdates,
    to: SymbolicNode[ConstraintType] = this.to
  ): ThenEdge[ConstraintType] = {
    val copy = ThenEdge.apply(storeUpdates, to)
    copy.id = this.id
    copy
  }
}
case class ElseEdge[ConstraintType <: Constraint](
  storeUpdates: Iterable[StoreUpdate],
  to: SymbolicNode[ConstraintType]
)
  extends Edge[ConstraintType] {
  override def toDirection: Direction = ElseDirection
  def merge(predExp: BooleanExpression, other: Edge[ConstraintType]): ElseEdge[ConstraintType] = {
    this.copy(EdgeMerger.mergeStoreConstraints(predExp, storeUpdates, other.storeUpdates))
  }
  protected def replaceStoreUpdates(newStoreUpdates: Iterable[StoreUpdate]): ElseEdge[ConstraintType] = {
    copy(storeUpdates = newStoreUpdates)
  }
  def copy(
    storeUpdates: Iterable[StoreUpdate] = this.storeUpdates,
    to: SymbolicNode[ConstraintType] = this.to
  ): ElseEdge[ConstraintType] = {
    val copy = ElseEdge.apply(storeUpdates, to)
    copy.id = this.id
    copy
  }
}

class ThenEdgeHasStoreUpdates[C <: Constraint] extends HasStoreUpdates[ThenEdge[C]] {
  def storeUpdates(thenEdge: ThenEdge[C]): Iterable[StoreUpdate] = thenEdge.storeUpdates
  def replaceStoreUpdates(
    thenEdge: ThenEdge[C],
    newStoreUpdates: Iterable[StoreUpdate]
  ): ThenEdge[C] = {
    thenEdge.copy(storeUpdates = newStoreUpdates)
  }
}
class ElseEdgeHasStoreUpdates[C <: Constraint] extends HasStoreUpdates[ElseEdge[C]] {
  def storeUpdates(elseEdge: ElseEdge[C]): Iterable[StoreUpdate] = elseEdge.storeUpdates
  def replaceStoreUpdates(
    elseEdge: ElseEdge[C],
    newStoreUpdates: Iterable[StoreUpdate]
  ): ElseEdge[C] = {
    elseEdge.copy(storeUpdates = newStoreUpdates)
  }
}

case object ThenEdge {
  def to[ConstraintType <: Constraint](to: SymbolicNode[ConstraintType]): ThenEdge[ConstraintType] = {
    apply(Nil, to)
  }
}
case object ElseEdge {
  def to[ConstraintType <: Constraint](to: SymbolicNode[ConstraintType]): ElseEdge[ConstraintType] = {
    apply(Nil, to)
  }
}

class BranchSymbolicNode[ConstraintType <: Constraint](
  val constraint: ConstraintType,
  var thenBranch: ThenEdge[ConstraintType],
  var elseBranch: ElseEdge[ConstraintType]
)
  extends SymbolicNodeWithConstraint[ConstraintType] {

  // Keep a set of all paths from root of tree to this node, if this node has been merged.
  // If this node is then merged again later on, can walk through descendants of the merged node and equate their global paths in the virtual path store

//  private var virtualPathStorePerIdentifier: VirtualPathStorePerIdentifier = new VirtualPathStorePerIdentifier
  private var hasMergedExecutionStates: Set[ExecutionState] = Set()
  private var usedAsITEPredicateForExecutionStates: Set[ExecutionState] = Set()
  private var pathsUsingThisAsITEPredicate: Set[Path] = Set()
//  def getVirtualPathStoreForIdentifier(identifier: String): StoresVirtualPaths = {
//    virtualPathStorePerIdentifier.getVirtualPathStoreForIdentifier(identifier)
//  }
//  def getAllVirtualPathStores: Iterable[(String, StoresVirtualPaths)] = {
//    virtualPathStorePerIdentifier.getAllVirtualPathStores
//  }
  override def getMergedExecutionStates: Set[ExecutionState] = hasMergedExecutionStates

  override def flagMergedForES(executionState: ExecutionState): Unit = {
    hasMergedExecutionStates += executionState
  }

  override def hasBeenMerged: Boolean = hasMergedExecutionStates.nonEmpty
  override def hasBeenMergedForES(executionState: ExecutionState): Boolean = {
    hasMergedExecutionStates.contains(executionState)
  }
  def hasBeenUsedAsITEPredicateForExecutionStates(state: ExecutionState): Boolean = {
    usedAsITEPredicateForExecutionStates.contains(state)
  }
  def hasBeenUsedAsITEPredicateForAnyExecutionState: Boolean = {
    usedAsITEPredicateForExecutionStates.nonEmpty
  }
  def getESUsedAsITEPredicateFor: Set[ExecutionState] = {
//    assert(usedAsITEPredicateForExecutionStates.size <= 1)
    usedAsITEPredicateForExecutionStates
  }
  def usedForMerging(state: ExecutionState): Unit = usedAsITEPredicateForExecutionStates += state
  def usedAsITEPredicateFor(state: ExecutionState, path1: TreePath[ConstraintType], path2: TreePath[ConstraintType]): Unit = {
//    SymbolicITECreator.findLastSharedNodeBetween(state, path1.tail, path2.tail) match {
//      case Some((shortThenPath, shortElsePath, _)) =>
//        pathsUsingThisAsITEPredicate += shortThenPath
//        pathsUsingThisAsITEPredicate += shortElsePath
//      case None =>
        pathsUsingThisAsITEPredicate += path1.getPath
        pathsUsingThisAsITEPredicate += path2.getPath
//    }
  }
  def getUsedAsITEPredicateFor: Set[Path] = pathsUsingThisAsITEPredicate

  override def toString: String = {
    if (Main.useDebug) {
      s"BranchSymbolicNode($constraint, $thenBranch, $elseBranch)"
    } else {
      s"BSN($constraint)"
    }
  }
  def replaceConstraint(newConstraint: ConstraintType): BranchSymbolicNode[ConstraintType] = {
    val newNode = copy(constraint = newConstraint)
    newNode.parentsChildSetters = this.parentsChildSetters
    newNode
  }
  def copy(
    constraint: ConstraintType = this.constraint,
    thenBranch: ThenEdge[ConstraintType] = this.thenBranch,
    elseBranch: ElseEdge[ConstraintType] = this.elseBranch
  ): BranchSymbolicNode[ConstraintType] = {
    val node = new BranchSymbolicNode[ConstraintType](constraint, thenBranch, elseBranch)
    node.usedAsITEPredicateForExecutionStates = this.usedAsITEPredicateForExecutionStates
    node.pathsUsingThisAsITEPredicate = this.pathsUsingThisAsITEPredicate
//    node.virtualPathStorePerIdentifier = this.virtualPathStorePerIdentifier
    node.hasMergedExecutionStates = this.hasMergedExecutionStates
    node
  }
  def thenBranchTaken: Boolean = thenBranch.to match {
    case UnexploredNode() => false
    case _ => true
  }
  def elseBranchTaken: Boolean = elseBranch.to match {
    case UnexploredNode() => false
    case _ => true
  }

  def setElseBranch(child: ElseEdge[ConstraintType]): Unit = {
    elseBranch = child
  }
  def setThenBranch(child: ThenEdge[ConstraintType]): Unit = {
    thenBranch = child
  }
}

object BranchSymbolicNode {
  def apply[ConstraintType <: Constraint](
    constraint: ConstraintType,
    thenBranch: ThenEdge[ConstraintType],
    elseBranch: ElseEdge[ConstraintType]
  ): BranchSymbolicNode[ConstraintType] = {
    new BranchSymbolicNode[ConstraintType](constraint, thenBranch, elseBranch)
  }
  def unapply[ConstraintType <: Constraint](node: BranchSymbolicNode[ConstraintType]): Option[(ConstraintType, ThenEdge[ConstraintType], ElseEdge[ConstraintType])] = {
    Some((node.constraint, node.thenBranch, node.elseBranch))
  }
}

case class MergedNode[ConstraintType <: Constraint]()
  extends SymbolicNode[ConstraintType]
case class RegularLeafNode[ConstraintType <: Constraint]()
  extends SymbolicNode[ConstraintType]
case class UnexploredNode[ConstraintType <: Constraint]()
  extends SymbolicNode[ConstraintType]
case class UnsatisfiableNode[ConstraintType <: Constraint]()
  extends SymbolicNode[ConstraintType]
case class SafeNode[ConstraintType <: Constraint](node: SymbolicNode[ConstraintType])
  extends SymbolicNode[ConstraintType]

class SymbolicNodeHasStoreUpdates[C <: Constraint : HasStoreUpdates] extends HasStoreUpdates[SymbolicNode[C]] {
  override def storeUpdates(node: SymbolicNode[C]): Iterable[StoreUpdate] = node match {
    case node: SymbolicNodeWithConstraint[C] =>
      implicitly[HasStoreUpdates[C]].storeUpdates(node.constraint)
    case _ => Nil
  }
  override def replaceStoreUpdates(
    node: SymbolicNode[C],
    newStoreUpdates: Iterable[StoreUpdate]
  ): SymbolicNode[C] = node match {
    case bsn: BranchSymbolicNode[C] =>
      val constraintWithReplacedUpdates = implicitly[HasStoreUpdates[C]].replaceStoreUpdates(bsn.constraint, newStoreUpdates)
      bsn.replaceConstraint(constraintWithReplacedUpdates)
    case _ => node
  }
}