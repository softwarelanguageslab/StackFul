package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression.Util._
import backend.expression._
import backend.logging.Logger
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.merging.use_cases.automatically_generated.{Store, TreeTraversalResult}

import scala.util.Random

case class SimulatedUserEventCC(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {
  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
//    if (i_<id> == 1) {
//      c = d + 1
//    } else if (i_<id> == 2) {
//      c = d + 2
//    } else if (i_<id> == 3)
//      c = d + 3
//    } else {
//      c = d + 4
//    }

    val concValueInput = 1 + Random.nextInt(4)
    val symValueInput = input(newInputId())

    val concPred1 = concValueInput == 1
    val symPred1 = RelationalExpression(symValueInput, IntEqual, i(1))

    val concPred2 = concValueInput == 2
    val symPred2 = RelationalExpression(symValueInput, IntEqual, i(2))

    val concPred3 = concValueInput == 3
    val symPred3 = RelationalExpression(symValueInput, IntEqual, i(3))

    val bc1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred1, Nil), dummyExecutionState)
    val pc1 = ConstraintWithStoreUpdate(bc1, concPred1)

    val bc2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred2, Nil), dummyExecutionState)
    val pc2 = ConstraintWithStoreUpdate(bc2, concPred2)

    val bc3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred3, Nil), dummyExecutionState)
    val pc3 = ConstraintWithStoreUpdate(bc3, concPred3)

    val valueD = store("d")
    val concValueD = valueD._2
    val symValueD = valueD._1.replaceIdentifier(Some("d"))

    val (assignments, store2, constraints: List[ConstraintWithStoreUpdate[ConstraintES]]) =
      if (concPred1) { // if (i_<id> == 1)
         Logger.n(Console.GREEN + "Event CC, if (i_<id> == 1) true" + Console.RESET)
        // c = d + 1
        val concValueC2 = concValueD + 1
        val symValueC2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD, i(1)))

        val assignments = AssignmentsStoreUpdate(List("c" -> symValueC2))
        val store2 = store + ("c" -> (symValueC2, concValueC2))
        (assignments, store2, List(pc1))
    } else if (concPred2) { // else (i_<id> == 2)
        Logger.n(Console.GREEN + "Event CC, else if (i_<id> == 2) true" + Console.RESET)
        // c = d + 2
        val concValueC2 = concValueD + 2
        val symValueC2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD, i(2)))

        val assignments = AssignmentsStoreUpdate(List("c" -> symValueC2))
        val store2 = store + ("c" -> (symValueC2, concValueC2))
        (assignments, store2, List(pc1, pc2))
    } else if (concPred3) { // else if (i_<id> == 3)
        Logger.n(Console.GREEN + "Event CC, if (i_<id> == 3) true" + Console.RESET)
        // c = d + 3
        val concValueC2 = concValueD + 3
        val symValueC2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD, i(3)))

        val assignments = AssignmentsStoreUpdate(List("c" -> symValueC2))
        val store2 = store + ("c" -> (symValueC2, concValueC2))
        (assignments, store2, List(pc1, pc2, pc3))
    } else { // else
        Logger.n(Console.GREEN + "Event CC, else" + Console.RESET)
        // c = d + 4
        val concValueC2 = concValueD + 4
        val symValueC2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD, i(4)))

        val assignments = AssignmentsStoreUpdate(List("c" -> symValueC2))
        val store2 = store + ("c" -> (symValueC2, concValueC2))
        (assignments, store2, List(pc1, pc2, pc3))
    }

    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val storeUpdateElement: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignments)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val pc = tcElement :: constraints ++ List(storeUpdateElement, sttElement)

    val predsAreTrue = constraints.map((c: ConstraintWithStoreUpdate[ConstraintES]) => if (c.isTrue) "T" else "E").mkString
    val path: Path = targetIdToPath(targetId) + predsAreTrue + (if (isLastEventInSequence) "T" else "E")
    (pc, store2, path)
  }
}
