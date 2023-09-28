package backend.tree.constraints.event_constraints

import backend.execution_state.store.StoreUpdate
import backend.tree.constraints._

case class StopTestingTargets(
  id: Int,
  processesInfo: List[Int],
  storeUpdates: Iterable[StoreUpdate],
  startingProcess: Int
)
  extends EventConstraint {
  override def toString: String = s"StopTestingTargets($id)"
}
