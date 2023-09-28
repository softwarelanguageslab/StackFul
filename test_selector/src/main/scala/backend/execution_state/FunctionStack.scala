package backend.execution_state

case class FunctionStack(stack: List[FunctionId]) {
  override def toString: String = s"[${stack.mkString(",")}]"
}

object FunctionStack {
  def empty: FunctionStack = FunctionStack(Nil)
}
