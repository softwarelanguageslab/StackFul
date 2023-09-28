package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression.Util.{i, input}
import backend.expression.{IntEqual, RelationalExpression}
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.merging.use_cases.automatically_generated.{Store, TreeTraversalResult}
import backend._

case class SimulatedUserEventA(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {

  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
    // No branching, assign a to b, and assign b to 123

    // TODO debugging: add 1 if-statement which always follows the then-branch
    val (symValueB, concValueB) = store("b")
    val newSymValueA = symValueB.replaceIdentifier(Some("b"))
    val newConcValueB = 123
    val newSymValueB = i(newConcValueB)

    val assignments = AssignmentsStoreUpdate(List("a" -> newSymValueA, "b" -> newSymValueB))
    val newStore = store + ("a" -> (newSymValueA, concValueB)) + ("b" -> (newSymValueB, newConcValueB))
    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val pred = RelationalExpression(input(newInputId(), targetId), IntEqual, i(99))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val bc = EventConstraintWithExecutionState(BranchConstraint(pred, Nil), dummyExecutionState)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val bcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(bc, true)
    val storeUpdateElement: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignments)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val pc = List(tcElement, bcElement, storeUpdateElement, sttElement)
    val path: Path = targetIdToPath(targetId) + "T" + (if (isLastEventInSequence) "T" else "E")
    (pc, newStore, path)
  }
}
