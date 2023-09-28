package backend.tree.search_strategy

import scala.collection.immutable.LinearSeq

import backend.tree.SymbolicNode
import backend.tree.constraints.Constraint

trait SearchStrategy[C <: Constraint] {
  def findUnexploredNodes(
    symbolicNode: SymbolicNode[C]
  ): LinearSeq[TreePath[C]]
}
