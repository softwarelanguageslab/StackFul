package backend

import backend.tree.constraints.Constraint

package object tree {

  implicit def thenEdgeToThenEdgeWithout[T, C <: Constraint](thenEdge: ThenEdge[C]): ThenEdgeWithoutTo[C] = {
    ThenEdgeWithoutTo(thenEdge.storeUpdates)
  }
  implicit def elseEdgeToElseEdgeWithout[T, C <: Constraint](elseEdge: ElseEdge[C]): ElseEdgeWithoutTo[C] = {
    ElseEdgeWithoutTo(elseEdge.storeUpdates)
  }

}
