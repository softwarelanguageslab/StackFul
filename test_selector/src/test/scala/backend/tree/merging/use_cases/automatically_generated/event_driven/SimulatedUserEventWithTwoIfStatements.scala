package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend._
import backend.execution_state._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.expression.Util.i
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.merging.use_cases.automatically_generated._

trait SimulatedUserEventTraverseMultipleTimes extends SimulatedUserEvent {
  def randomBoolGenerator: GeneratesRandomBooleans = PseudoRandomBoolGenerator
  def traverse(startingStore: Store[Int], eventsInvoked: List[(Int, Int)]): Iterable[TreeTraversalResultWithES[Int]]
}

case class SimulatedUserEventWithTwoIfStatements1(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int,
  identifier: String,
  comparisonValue1: Int,
  comparisonValue2: Int
) extends SimulatedUserEventWithTwoIfStatements(
  targetId, eventId, eventSequenceLength, inputIdGenerator, nrOfEventsInThisTest, identifier, comparisonValue1,
  comparisonValue2) {
  override protected val if1Serial = 44
  override protected val if2Serial = 55
}

case class SimulatedUserEventWithTwoIfStatements2(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int,
  identifier: String,
  comparisonValue1: Int,
  comparisonValue2: Int
) extends SimulatedUserEventWithTwoIfStatements(
  targetId, eventId, eventSequenceLength, inputIdGenerator, nrOfEventsInThisTest, identifier, comparisonValue1,
  comparisonValue2) {
  override protected val if1Serial = 70
  override protected val if2Serial = 81
}

case class SimulatedUserEventWithTwoIfStatements3(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int,
  identifier: String,
  comparisonValue1: Int,
  comparisonValue2: Int
) extends SimulatedUserEventWithTwoIfStatements(
  targetId, eventId, eventSequenceLength, inputIdGenerator, nrOfEventsInThisTest, identifier, comparisonValue1,
  comparisonValue2) {
  override protected val if1Serial = 96
  override protected val if2Serial = 107
}

abstract class SimulatedUserEventWithTwoIfStatements(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int,
  identifier: String,
  comparisonValue1: Int,
  comparisonValue2: Int
) extends SimulatedUserEventTraverseMultipleTimes {

  protected val if1Serial: Int
  protected val if2Serial: Int

  protected def makeExecutionState(serial: Int, eventSequence: List[(Int, Int)], stackLength: Int): ExecutionState = {
    val (processId, targetId) = eventSequence.last
    CodeLocationExecutionState(SerialCodePosition("", serial), FunctionStack.empty, Some((eventSequence.length - 1, processId, targetId)), stackLength)
  }

  def traverse(store: Store[Int], eventsInvoked: List[(Int, Int)]): Iterable[TreeTraversalResultWithES[Int]] = {
    //    var <identifier>;
    //    if (i_<id> == <comparisonValue1>) {
    //      <identifier> = <comparisonValue2>;
    //    } else {
    //      <identifier> = <comparisonValue2> + 1;
    //    }
    //    if [<secondIfExecutionState>](<identifier> == <comparisonValue2>) {
    //      <nothing>
    //    } else {
    //      <nothing>
    //    }
    //
    val firstIfExecutionState = makeExecutionState(if1Serial, eventsInvoked, 1)
    val secondIfExecutionState = makeExecutionState(if2Serial, eventsInvoked, 1)

    val symValueInput = Util.input(firstIfExecutionState, 0, 0)

    val concPred1 = randomBoolGenerator.nextBoolean()
    val symPred1 = RelationalExpression(symValueInput, IntEqual, i(comparisonValue1))
    val bc1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred1, Nil), firstIfExecutionState)
    val pcElement1 = ConstraintWithStoreUpdate(bc1, concPred1)

    val (assignments2, store2) =
      if (concPred1) {
        println(Console.GREEN + s"Event ${this.targetId} true branch taken" + Console.RESET)
        val concValue_T = comparisonValue2
        val symValue_T = i(concValue_T)

        val assignment_T = AssignmentsStoreUpdate(List(identifier -> symValue_T))
        val storeT = store + (identifier -> (symValue_T, concValue_T))
        (assignment_T, storeT)
      } else {
        println(Console.GREEN + s"Event ${this.targetId} false branch taken" + Console.RESET)
        val concValue_F = comparisonValue2 + 1
        val symValue_F = i(concValue_F)

        val assignment_F = AssignmentsStoreUpdate(List(identifier -> symValue_F))
        val store_F = store + (identifier -> (symValue_F, concValue_F))
        (assignment_F, store_F)
      }

    val concPred2 = store2(identifier)._2 == comparisonValue2
    val symPred2 = RelationalExpression(store2(identifier)._1.replaceIdentifier(Some(identifier)), IntEqual, i(comparisonValue2))
    val bc2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred2, Nil), secondIfExecutionState)
    val pcElement2 = ConstraintWithStoreUpdate(bc2, concPred2)

    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val storeUpdateElement: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignments2)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val pc1 = List(tcElement, pcElement1)
    val pcElement1IsTrue = if (concPred1) "T" else "E"
    val path1: Path = targetIdToPath(targetId) + pcElement1IsTrue

    val pc2 = pc1 ++ List(storeUpdateElement, pcElement2)
    val pcElement2IsTrue = if (concPred2) "T" else "E"
    val path2: Path = path1 + pcElement2IsTrue

    val pc3 = pc2 :+ sttElement
    val path3: Path = path2 + (if (isLastEventInSequence) "T" else "E")

    val traversalResult1: TreeTraversalResultWithES[Int] = (pc1, store, path1, firstIfExecutionState)
    // Use store2 because no changes have been made to the store after the second if-statement
    val traversalResult2: TreeTraversalResultWithES[Int] = (pc2, store2, path2, secondIfExecutionState)
    val traversalResult3: TreeTraversalResultWithES[Int] = (pc3, store2, path3, TargetEndExecutionState(eventId))

    List(traversalResult1, traversalResult2, traversalResult3)

  }
}
