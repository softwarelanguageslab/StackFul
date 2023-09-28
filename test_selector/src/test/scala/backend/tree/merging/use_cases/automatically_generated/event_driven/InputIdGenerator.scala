package backend.tree.merging.use_cases.automatically_generated.event_driven

trait InputIdGenerator {
  def processId: Int
  private var inputIdForEvent: Int = 0
  def reset(): Unit = {
    inputIdForEvent = 0
  }
  def newInputId(): Int = {
    val temp = inputIdForEvent
    inputIdForEvent += 1
    temp
  }
}
/**
  * Can be used by any event
  */
object NeutralEventInputIdGenerator extends InputIdGenerator {
  def processId: Int = -1
}
object EventAInputIdGenerator extends InputIdGenerator {
  def processId: Int = 0
}
object EventBInputIdGenerator extends InputIdGenerator {
  def processId: Int = 1
}
object EventCInputIdGenerator extends InputIdGenerator {
  def processId: Int = 2
}
object EventDInputIdGenerator extends InputIdGenerator {
  def processId: Int = 3
}
