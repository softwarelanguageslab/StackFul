package backend.execution_state

import backend.{Main, execution_state}

sealed trait ExecutionState {
  def stackLength: Int
}

case class CodeLocationExecutionState(
  codePosition: CodePosition,
  functionStack: FunctionStack,
  currentEvent: Option[(Int, Int, Int)],
  stackLength: Int
) extends ExecutionState {
  override def toString: String = if (Main.useDebug) s"ES($codePosition, $currentEvent)" else s"ES($codePosition)"
}

object CodeLocationExecutionState {
  def dummyExecutionState: CodeLocationExecutionState = {
    val position = SerialCodePosition("", -1)
    val stack = FunctionStack(Nil)
    CodeLocationExecutionState(position, stack, None, 1)
  }
}

case class TargetEndExecutionState(id: Int) extends ExecutionState {
  override def toString: String = s"Stop($id)"
  override def stackLength: Int = 0
  def applyToStore(store: execution_state.SymbolicStore): SymbolicStore = store
}

case class TargetTriggeredExecutionState(eventId: Int, processId: Int) extends ExecutionState {
  override def toString: String = s"TT(id:${this.eventId}, pid:${this.processId})"
  override def stackLength: Int = 0
}
