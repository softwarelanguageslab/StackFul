package backend.tree.merging

import backend.tree._
import backend.tree.constraints.Constraint

trait WorkListFinder[C <: Constraint] {
  def findWorkList(
    root: SymbolicNode[C]
  ): Set[PathAndNode[C]]
}
