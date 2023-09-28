package backend.tree.constraints.basic_constraints

import backend.execution_state.store.StoreUpdate
import backend.expression.BooleanExpression
import backend.tree.constraints._

case class BranchConstraint(exp: BooleanExpression, storeUpdates: Iterable[StoreUpdate])
  extends BasicConstraint
    with EventConstraint {
  override def toString: String = s"BC(${exp.toString})"
  def negate: BranchConstraint = {
    BranchConstraint(exp.negate, storeUpdates)
  }
}
