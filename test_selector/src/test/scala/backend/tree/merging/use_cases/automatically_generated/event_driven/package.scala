package backend.tree.merging.use_cases.automatically_generated

import backend.Path
import backend.execution_state.CodeLocationExecutionState
import backend.tree.constraints.event_constraints.ProcessesInfo
import backend.tree.follow_path.{ElseDirection, ThenDirection}

package object event_driven extends UsesRandomInt {

  val unassigned = "unassigned"
  val valueUnassigned = 42

  def makeProcessesInfo(nrOfEvents: Int): ProcessesInfo = ProcessesInfo(List(nrOfEvents), true)

  def targetIdToPath(targetId: Int): Path = {
    (1.to(targetId).map(_ => ElseDirection) :+ ThenDirection).toList
  }

  val dummyExecutionState: CodeLocationExecutionState = CodeLocationExecutionState.dummyExecutionState

  /*
   * Keeps track of which events (identified with their eventId) generated which values for c,
   * since these have to be reused.
   */
  private var randomValuesForC: Map[Int, Int] = Map()
  def getValueForC(eventId: Int): Int = {
    randomValuesForC.get(eventId) match {
      case Some(value) => value
      case None =>
        val newValue = randomInt()
        randomValuesForC += eventId -> newValue
        newValue
    }
  }

}
