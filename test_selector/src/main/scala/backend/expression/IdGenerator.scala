package backend.expression

object IdGenerator {
  private var id: Int = 0

  def resetId(): Unit = {
    id = 0
  }

  def newSymbolicInputInt: SymbolicInput = {
    SymbolicInputInt(newId())
  }
  def newSymbolicInputString: SymbolicInput = {
    SymbolicInputString(newId())
  }
  private def newId(): RegularId = {
    val current = id
    id += 1
    RegularId(current, 0)
  }
  def newSymbolicAddress: SymbolicAddress = {
    SymbolicAddress(newId().id)
  }
}
