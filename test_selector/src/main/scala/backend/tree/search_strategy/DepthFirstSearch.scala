package backend.tree.search_strategy

import scala.annotation.tailrec
import scala.collection.immutable.LinearSeq

import backend.tree._
import backend.tree.constraints._

class DepthFirstSearch[C <: Constraint : ConstraintNegater] extends SearchStrategy[C] {

  def findUnexploredNodes(
    symbolicNode: SymbolicNode[C]
  ): LinearSeq[TreePath[C]] =
    symbolicNode match {
      case b: BranchSymbolicNode[C] =>
        val (path, _) = TreePath.init[C](b)
        //        if (isUnexplored) {
        //          // Very first node in the tree is unexplored, so just return that one
        //          Some(path.finished)
        //        } else {
        val stack: List[TreePath[C]] = List(path)
        loop(stack, List())
      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
        List()
    }

  @tailrec
  private def loop(
    stack: List[TreePath[C]],
    result: LinearSeq[TreePath[C]]
  ): LinearSeq[TreePath[C]] =
    stack match {
      case Nil => result
      case head :: rest =>
        head.lastNode match {
          case b: BranchSymbolicNode[C] =>
            val collection =
              if (!b.thenBranchTaken || !b.elseBranchTaken) {
                assert(b.thenBranchTaken != b.elseBranchTaken, "Should not happen: one of both branches should be True")
                List(head.finished)
              } else {
                List()
              }
            // Both branches have already been explored, so continue looking through both branches to find an unexplored node
            // Although both branches have been explored, it could be that they don't actually have any successors, e.g., because the branch ends

            // Only add child-branches if they are a BranchSymbolicNode
            val newStack: List[TreePath[C]] = (b.thenBranch.to, b.elseBranch.to) match {
              // Negate branches
              case (thenNode: SymbolicNodeWithConstraint[C],
              elseNode: SymbolicNodeWithConstraint[C]) =>
                head.addThenBranch(thenNode, b.thenBranch) :: head.addElseBranch(elseNode, b.elseBranch) :: rest
              case (thenNode: SymbolicNodeWithConstraint[C], _) =>
                head.addThenBranch(thenNode, b.thenBranch) :: rest
              case (_, elseNode: SymbolicNodeWithConstraint[C]) =>
                head.addElseBranch(elseNode, b.elseBranch) :: rest
              case (_, _) => rest
            }
            loop(newStack, result ++ collection)
        }
    }

}
