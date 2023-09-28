package backend.tree

import backend._
import backend.execution_state.store.StoreUpdate
import backend.tree.constraints._

import scala.annotation.tailrec

package object search_strategy {

  type Edges[C <: Constraint] = List[Edge[C]]
  type EdgeWithoutTos[C <: Constraint] = List[EdgeWithoutTo[C]]

  /**
    *
    * @param original The original expressions recorded by the Reporter
    * @param seen     Nodes in which the else-branch were taken are negated to represent the path actually taken during execution
    */
  case class TreePath[C <: Constraint] private(
    original: List[SymbolicNodeWithConstraint[C]],
    private val seen: List[C],
    private val path: EdgeWithoutTos[C],
    private val pathWithEdges: Edges[C]
  )(implicit negater: ConstraintNegater[C]) {

    private var isFinished = false
    def finishedByDroppingLastConstraint: TreePath[C] = {
      this.copy(original = if (original.isEmpty) Nil else original.init).finished
    }
    def finishedViaElse: TreePath[C] = {
      val last = original.last
      assert(last.isInstanceOf[BranchSymbolicNode[C]])
      val castedLast = last.asInstanceOf[BranchSymbolicNode[C]]
      val seenAdded = seen :+ negater.negate(last.constraint)
      this.copy(seen = seenAdded, path = path :+ ElseEdgeWithoutTo(castedLast.elseBranch.storeUpdates)).finished
    }
    def finishedViaThen: TreePath[C] = {
      val last = original.last
      assert(last.isInstanceOf[BranchSymbolicNode[C]])
      val castedLast = last.asInstanceOf[BranchSymbolicNode[C]]
      this.copy(
        seen = seen :+ last.constraint, path = path :+ ThenEdgeWithoutTo(castedLast.thenBranch.storeUpdates)).finished
    }
    def finished: TreePath[C] = {
      isFinished = true
      this
    }
    def length: Int = path.length

    def last: (SymbolicNodeWithConstraint[C], Constraint, EdgeWithoutTo[C]) = {
      (lastNode, seen.last, path.last)
    }

    def lastNode: SymbolicNodeWithConstraint[C] = original.last

    def init: TreePath[C] = {
      val newPath = TreePath(
        original.init, if (seen.isEmpty) seen else seen.init, if (path.isEmpty) path else path.init,
        if (pathWithEdges.isEmpty) pathWithEdges else pathWithEdges.init)
      newPath.isFinished = this.isFinished
      newPath
    }
    def tail: TreePath[C] = {
      val newPath = TreePath(
        original.tail, if (seen.isEmpty) seen else seen.tail, if (path.isEmpty) path else path.tail,
        if (pathWithEdges.isEmpty) pathWithEdges else pathWithEdges.tail)
      newPath.isFinished = this.isFinished
      newPath
    }

    // Same as addThenBranch
    def :+(node: SymbolicNodeWithConstraint[C], thenEdge: ThenEdge[C]): TreePath[C] = addThenBranch(
      node, thenEdge)
    def addThenBranch(node: SymbolicNodeWithConstraint[C], thenEdge: ThenEdge[C]): TreePath[C] = {
      def add(storeConstraints: Iterable[StoreUpdate]): TreePath[C] = {
        val originalAdded = original :+ node
        val newPath = TreePath(
          originalAdded, seen :+ original.last.constraint, path :+ ThenEdgeWithoutTo(storeConstraints),
          pathWithEdges :+ thenEdge)
        newPath.isFinished = this.isFinished
        newPath
      }

      original.lastOption match {
        case None => add(Nil)
        case Some(bsn: BranchSymbolicNode[C]) => add(bsn.thenBranch.storeUpdates)
        case Some(_) => throw InvalidPathException(path.map(_.toDirection))
      }
    }
    // Same as addElseBranch
    def !:+(node: SymbolicNodeWithConstraint[C], elseEdge: ElseEdge[C]): TreePath[C] = addElseBranch(
      node, elseEdge)
    def addElseBranch(node: SymbolicNodeWithConstraint[C], elseEdge: ElseEdge[C]): TreePath[C] = {
      def add(storeConstraints: Iterable[StoreUpdate]): TreePath[C] = {
        val seenAdded = seen :+ negater.negate(original.last.constraint)
        val newPath = TreePath(
          original :+ node, seenAdded, path :+ ElseEdgeWithoutTo(storeConstraints), pathWithEdges :+ elseEdge)
        newPath.isFinished = this.isFinished
        newPath
      }

      original.lastOption match {
        case None => add(Nil)
        case Some(bsn: BranchSymbolicNode[C]) => add(bsn.elseBranch.storeUpdates)
        case Some(_) => throw InvalidPathException(path.map(_.toDirection))
      }
    }
    def addFirst(otherPath: TreePath[C]): TreePath[C] = {
      TreePath(
        this.original :+ otherPath.original.head,
        this.seen :+ otherPath.getObserved.head,
        this.path :+ otherPath.getEdges.head,
        this.pathWithEdges :+ otherPath.getPathWithEdges.head)
    }
    def getObserved: List[C] = seen
    def getEdges: EdgeWithoutTos[C] = path
    def getPathWithEdges: Edges[C] = pathWithEdges
    def getPath: Path = path.map(_.toDirection)

  }

  case class CommonPrefixResult[C <: Constraint : ConstraintNegater](
    prefix: TreePath[C],
    suffixPath1: TreePath[C],
    suffixPath2: TreePath[C]
  )

  object TreePath {

    def fromPathConstraint[C <: Constraint : ConstraintNegater, PCElementUsed <: RegularPCElement[C]](
      originalPC: List[PCElementUsed],
      root: SymbolicNode[C]
    ): TreePath[C] = {
      @tailrec
      def loop(
        pc: List[PCElementUsed],
        node: SymbolicNode[C],
        acc: TreePath[C]
      ): TreePath[C] = pc.headOption match {
        case Some(pcElement) => node match {
          case bsn: BranchSymbolicNode[C] =>
            if (pcElement.isTrue) {
              bsn.thenBranch.to match {
                case nwc: SymbolicNodeWithConstraint[C] =>
                  loop(pc.tail, nwc, acc.addThenBranch(nwc, bsn.thenBranch))
                case _ =>
                  if (pc.tail.nonEmpty) {
                    throw InvalidPathException(pathConstraintToPath(originalPC))
                  } else {
                    acc.finishedViaThen
                  }
              }
            } else {
              bsn.elseBranch.to match {
                case nwc: SymbolicNodeWithConstraint[C] =>
                  loop(pc.tail, nwc, acc.addElseBranch(nwc, bsn.elseBranch))
                case _ =>
                  if (pc.tail.nonEmpty) {
                    throw InvalidPathException(pathConstraintToPath(originalPC))
                  } else {
                    acc.finishedViaElse
                  }
              }
            }
        }
        case None => acc.finishedByDroppingLastConstraint
      }
      loop(originalPC, root, TreePath.init(root)._1)
    }

    /**
      * Initialises a tree-path and returns whether this path immediately refers to a non-fully-explored path
      *
      * @param usn The node to start with
      * @tparam C The type for the Constraint
      * @return The new tree-path and whether or not usn contains an unexplored branch
      */
    def init[C <: Constraint : ConstraintNegater](
      usn: SymbolicNode[C]
    ): (TreePath[C], Boolean) = usn match {
      case bsn: BranchSymbolicNode[C] => (TreePath[C](
        List(bsn), Nil, Nil, Nil), !bsn.elseBranchTaken || !bsn.thenBranchTaken)
      case _ => throw new Exception("Node should be a SymbolicNodeWithConstraint")
    }
    def findCommonPrefix[C <: Constraint : ConstraintNegater](
      treePath1: TreePath[C],
      treePath2: TreePath[C]
    ): CommonPrefixResult[C] = {
      @tailrec
      def loop(
        acc: TreePath[C],
        treePath1: TreePath[C],
        treePath2: TreePath[C]
      ): CommonPrefixResult[C] = {
        if (treePath1.getPath.nonEmpty && treePath2.getPath.nonEmpty &&
          treePath1.getPath.head == treePath2.getPath.head) {
          loop(acc.addFirst(treePath1), treePath1.tail, treePath2.tail)
        } else {
          CommonPrefixResult(acc, treePath1, treePath2)
        }
      }
      loop(TreePath.empty, treePath1, treePath2)
    }
    /* Should only be used to immediately return a pathed without later adding constraints/nodes to it.
    * TODO Make this type-safe by returning a 3-tuple instead of an actual TreePath? */
    def empty[C <: Constraint](implicit negater: ConstraintNegater[C]): TreePath[C] = {
      val path = TreePath[C](Nil, Nil, Nil, Nil)(negater)
      //        path.isFinished = true
      path
    }
  }

}
