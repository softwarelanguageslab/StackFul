package backend.tree.merging

import backend.execution_state.ExecutionState

case class MergingBarrier(executionState: ExecutionState, remaining: Int) {
  def arrivedAt: MergingBarrier = {
    this.copy(remaining = remaining - 1)
  }
}
