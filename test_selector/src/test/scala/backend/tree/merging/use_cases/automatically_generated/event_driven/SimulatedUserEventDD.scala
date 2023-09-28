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

case class SimulatedUserEventDD(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {
  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
//    if (i_<id> == 10) {
//      d = c + 10
//    } else if (i_<id> == 20) {
//      d = c + 20
//    } else if (i_<id> == 30)
//      d = c + 30
//    } else {
//      d = c + 40
//    }

    val concValueInput = 10 * (1 + Random.nextInt(4))
    val symValueInput = input(newInputId())

    val concPred1 = concValueInput == 10
    val symPred1 = RelationalExpression(symValueInput, IntEqual, i(10))

    val concPred2 = concValueInput == 20
    val symPred2 = RelationalExpression(symValueInput, IntEqual, i(20))

    val concPred3 = concValueInput == 30
    val symPred3 = RelationalExpression(symValueInput, IntEqual, i(30))

    val bc1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred1, Nil), dummyExecutionState)
    val pc1 = ConstraintWithStoreUpdate(bc1, concPred1)

    val bc2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred2, Nil), dummyExecutionState)
    val pc2 = ConstraintWithStoreUpdate(bc2, concPred2)

    val bc3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred3, Nil), dummyExecutionState)
    val pc3 = ConstraintWithStoreUpdate(bc3, concPred3)

    val valueC = store("c")
    val concValueC = valueC._2
    val symValueC = valueC._1.replaceIdentifier(Some("c"))

    val (assignments, store2, constraints: List[ConstraintWithStoreUpdate[ConstraintES]]) =
      if (concPred1) { // if (i_<id> == 10)
         Logger.n(Console.GREEN + "Event DD, if (i_<id> == 10) true" + Console.RESET)
        // d = c + 10
        val concValueD2 = concValueC + 10
        val symValueD2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC, i(10)))

        val assignments = AssignmentsStoreUpdate(List("d" -> symValueD2))
        val store2 = store + ("d" -> (symValueD2, concValueD2))
        (assignments, store2, List(pc1))
    } else if (concPred2) { // else (i_<id> == 20)
        Logger.n(Console.GREEN + "Event DD, else if (i_<id> == 20) true" + Console.RESET)
        // d = c + 20
        val concValueD2 = concValueC + 20
        val symValueD2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC, i(20)))

        val assignments = AssignmentsStoreUpdate(List("d" -> symValueD2))
        val store2 = store + ("d" -> (symValueD2, concValueD2))
        (assignments, store2, List(pc1, pc2))
    } else if (concPred3) { // else if (i_<id> == 30)
        Logger.n(Console.GREEN + "Event DD, if (i_<id> == 30) true" + Console.RESET)
        // d = c + 30
        val concValueD2 = concValueC + 30
        val symValueD2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC, i(30)))

        val assignments = AssignmentsStoreUpdate(List("d" -> symValueD2))
        val store2 = store + ("d" -> (symValueD2, concValueD2))
        (assignments, store2, List(pc1, pc2, pc3))
    } else { // else
        Logger.n(Console.GREEN + "Event DD, else" + Console.RESET)
        // d = c + 40
        val concValueD2 = concValueC + 40
        val symValueD2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC, i(40)))

        val assignments = AssignmentsStoreUpdate(List("d" -> symValueD2))
        val store2 = store + ("d" -> (symValueD2, concValueD2))
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
