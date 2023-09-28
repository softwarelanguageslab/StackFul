package backend.tree.constraints.constraint_with_execution_state

import backend.Main
import backend.execution_state._
import backend.tree.constraints._

case class EventConstraintWithExecutionState(ec: EventConstraint, executionState: ExecutionState)
  extends ConstraintWithExecutionState {
  override def toString: String = {
    if (Main.useDebug) s"EC/ES($ec, $executionState)" else s"EC/ES($executionState, ${ec.getClass.getSimpleName})"
  }
}