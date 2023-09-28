package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression.Util.{i, input}
import backend.expression._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.merging.use_cases.automatically_generated.{Store, TreeTraversalResult}
import backend._
import backend.logging.Logger

import scala.util.Random

case class SimulatedUserEventB(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {

  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
    // 1 branch:
    // if (i_<id> == 0) {
    //   a = a + 1;
    // else
    //   c = <random_int>
    // assign a to a + 1 along true branch, assign c to random number along false branch

    val inputId = newInputId()
    val predIsTrue = Random.nextBoolean()
    Logger.n(Console.GREEN + s"Pred in event B was $predIsTrue" + Console.RESET)

    val pred = RelationalExpression(input(inputId, targetId), IntEqual, i(0))
    val (assignments, newStore) = if (predIsTrue) {
      val valueA = store("a")
      val oldSymValueA = valueA._1.replaceIdentifier(Some("a"))
      val oldConcValueA = valueA._2
      val newConcValueA = oldConcValueA + 1
      val newSymValueA = ArithmeticalVariadicOperationExpression(IntPlus, List(oldSymValueA, i(1)))
      val assignments = AssignmentsStoreUpdate(List("a" -> newSymValueA))
      val newStore = store + ("a" -> (newSymValueA, newConcValueA))
      (assignments, newStore)
    } else {
      val newConcValueC = getValueForC(eventId)
      val newSymValueC = i(newConcValueC)
      val assignments = AssignmentsStoreUpdate(List("c" -> newSymValueC))
      val newStore = store + ("c" -> (newSymValueC, newConcValueC))
      (assignments, newStore)
    }
    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val bc = EventConstraintWithExecutionState(BranchConstraint(pred, Nil), dummyExecutionState)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val bcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(bc, predIsTrue)
    val storeUpdateElement: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignments)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val pc = List(tcElement, bcElement, storeUpdateElement, sttElement)
    val path: Path = targetIdToPath(targetId) + (if (predIsTrue) "T" else "E") + (if (isLastEventInSequence) "T" else "E")
    (pc, newStore, path)
  }
}
