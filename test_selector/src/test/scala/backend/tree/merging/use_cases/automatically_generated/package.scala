package backend.tree.merging.use_cases

import backend._
import backend.execution_state.{ExecutionState, SymbolicStore}
import backend.expression.SymbolicExpression
import backend.tree.constraints.constraint_with_execution_state.ConstraintES

package object automatically_generated {

  type Store[T] = Map[String, (SymbolicExpression, T)]
  type TreeTraversalResult[T] = (PathConstraintWithStoreUpdates[ConstraintES], Store[T], Path)
  type TreeTraversalResultWithES[T] = (PathConstraintWithStoreUpdates[ConstraintES], Store[T], Path, ExecutionState)

  def storeToSymStore[T](store: Store[T]): SymbolicStore = {
    store.view.mapValues(_._1).toMap
  }

}
