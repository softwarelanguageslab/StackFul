package backend.tree.constraints.event_constraints

import backend.execution_state.store.StoreUpdate
import backend.modes._
import backend.tree.constraints.{AllProcessesExplored, EventConstraint}

import scala.annotation.tailrec

case class TargetNotChosen(
  id: Int,
  processIdChosen: Int,
  targetIdChosen: Int,
  eventsAlreadyChosen: Map[Int, Set[Int]],
  info: ProcessesInfo
) extends EventConstraint {
  override def toString: String = s"TargetNotChosen(id:$id, pid:$processIdChosen, tid:$targetIdChosen)"
}
object TargetNotChosen {
  def inverseTargetChosen(targetChosen: TargetChosen): TargetNotChosen = {
    TargetNotChosen(
      targetChosen.id, targetChosen.processIdChosen, targetChosen.targetIdChosen,
      targetChosen.eventsAlreadyChosen, targetChosen.info)
  }
}

case class TargetChosen(
  id: Int,
  processIdChosen: Int,
  targetIdChosen: Int,
  eventsAlreadyChosen: Map[Int, Set[Int]],
  info: ProcessesInfo,
  startingProcess: Int,
  storeUpdates: Iterable[StoreUpdate]
)
  extends EventConstraint {
  override def toString: String = s"TargetChosen(id:$id, pid:$processIdChosen, tid:$targetIdChosen)"
  def lastPossibleTargetForThisId(mode: Mode): Boolean = mode match {
    case ExploreTreeMode | VerifyIntraMode | MergingMode =>
      if (info.processesInfo.isEmpty) {
        true
      } else {
        val updatedEventsAlreadyChosen =
          addTargetChosen(processIdChosen, targetIdChosen, eventsAlreadyChosen)
        TargetChosen.findNextProcessId(this, startingProcess, updatedEventsAlreadyChosen).isEmpty
      }
    case FunctionSummariesMode => true
    case SolvePathsMode => false
  }
  protected def addTargetChosen(
    processId: Int,
    eventId: Int,
    eventsAlreadyChosen: Map[Int, Set[Int]]
  ): Map[Int, Set[Int]] = {
    val oldProcessEventsChosen = eventsAlreadyChosen.getOrElse(processId, Set())
    eventsAlreadyChosen.updated(processId, oldProcessEventsChosen + eventId)
  }
  def addTargetChosen(processId: Int, eventId: Int): TargetChosen = {
    val newEventsChosen = addTargetChosen(processId, eventId, eventsAlreadyChosen)
    TargetChosen(id, processIdChosen, targetIdChosen, newEventsChosen, info, startingProcess, Nil)
  }

}

object TargetChosen {
  def makeDummyTargetChosen(dummyId: Int, processesInfo: List[Int], startingProcess: Int): Option[TargetChosen] = {
    val firstPossibleProcessId = processesInfo
      .drop(startingProcess)
      .indexWhere(_ > 0) + startingProcess // Start counting from Main.getStartingProcess
    if (firstPossibleProcessId >= 0) {
      val initialEventsAlreadyChosen =
        processesInfo.indices.foldLeft(Map[Int, Set[Int]]())((map, idx) => map + (idx -> Set()))
      val info = ProcessesInfo(processesInfo, allowCreatingExtraTargets = true)
      Some(TargetChosen(dummyId, firstPossibleProcessId, 0, initialEventsAlreadyChosen, info, startingProcess, Nil))
    } else {
      None
    }
  }
  def negate(tc: TargetChosen): TargetChosen = {
    val updatedIdsChosen = tc.addTargetChosen(tc.processIdChosen, tc.targetIdChosen, tc.eventsAlreadyChosen)
    findNextProcessId(tc, tc.startingProcess, updatedIdsChosen) match {
      case None =>
        if (tc.info.allowCreatingExtraTargets) {
          TargetChosen.makeDummyTargetChosen(tc.id + 1, tc.info.processesInfo, tc.startingProcess) match {
            case Some(newTargetChosen) => newTargetChosen
            case None => throw AllProcessesExplored
          }
        } else {
          throw AllProcessesExplored
        }
      case Some((newProcessId, newTargetId)) =>
        TargetChosen(tc.id, newProcessId, newTargetId, updatedIdsChosen, tc.info, tc.startingProcess, tc.storeUpdates)
    }
  }
  @tailrec
  private def findNextEventId(
    eventId: Int,
    nonEligableEvents: Set[Int],
    nrOfChoices: Int
  ): Option[Int] = {
    if (nonEligableEvents.contains(eventId)) {
      findNextEventId(eventId + 1, nonEligableEvents, nrOfChoices)
    } else if (eventId >= nrOfChoices) {
      None
    } else {
      Some(eventId)
    }
  }
  // Have to pass eventsAlreadyChosen instead of reusing the eventsAlreadyChosen-field of the class, because
  // eventsAlreadyChosen has been extended with targetIdChosen
  @tailrec
  private def findNextProcessId(
    tc: TargetChosen, processId: Int,
    eventsAlreadyChosen: Map[Int, Set[Int]]
  ): Option[(Int, Int)] = {
    if (processId >= tc.info.totalNrOfProcesses) {
      None
    } else {
      val nonEligableEvents = eventsAlreadyChosen(processId)
      val nrOfChoices = tc.info.processesInfo(processId)
      findNextEventId(0, nonEligableEvents, nrOfChoices) match {
        case Some(eventId) => Some((processId, eventId))
        case None => findNextProcessId(tc, processId + 1, eventsAlreadyChosen)
      }
    }
  }
}