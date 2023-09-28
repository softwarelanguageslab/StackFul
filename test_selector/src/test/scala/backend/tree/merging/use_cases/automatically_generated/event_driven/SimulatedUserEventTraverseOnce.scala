package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend.execution_state.TargetEndExecutionState
import backend.expression.{SymbolicInputInt, Util}
import backend.tree.merging.use_cases.automatically_generated._

trait SimulatedUserEvent {
  def eventId: Int
  def targetId: Int
  def processId: Int = 0
  def eventSequenceLength: Int

  def inputIdGenerator: InputIdGenerator
  protected def targetEndExecutionState: TargetEndExecutionState = TargetEndExecutionState(eventId)
  protected def newInputId(): Int = inputIdGenerator.newInputId()
  protected def newInput(): SymbolicInputInt = {
    Util.input(newInputId(), targetId)
  }
  protected def isLastEventInSequence: Boolean = {
    eventId == eventSequenceLength - 1
  }
}

trait SimulatedUserEventTraverseOnce extends SimulatedUserEvent {

  def traverse(startingStore: Store[Int]): TreeTraversalResult[Int]
}
