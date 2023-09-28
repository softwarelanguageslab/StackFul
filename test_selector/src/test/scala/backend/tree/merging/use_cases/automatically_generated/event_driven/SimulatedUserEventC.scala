package backend.tree.merging.use_cases.automatically_generated.event_driven
import backend._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.expression.Util._
import backend.logging.Logger
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}
import backend.tree.merging.use_cases.automatically_generated.{Store, TreeTraversalResult}

import scala.util.Random

case class SimulatedUserEventC(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {
  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
//    d++
//    if (a >= 2) {
//      b = 1
//      c++
//      b = c
//      b++
//    } else if (i_<id> == 5) {
//      a++
//    } else if (b == 123) {
//      e += 2
//    } else {
//      c = 321
//    }
//    d++

    val valueD = store("d")
    val concValueD1 = valueD._2
    val symValueD1 = valueD._1.replaceIdentifier(Some("d"))
    // d++
    val concValueD2 = concValueD1 + 1
    val symValueD2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD1, i(1)))

    val assignmentD2 = AssignmentsStoreUpdate(List("d" -> symValueD2))
    val store1 = store + ("d" -> (symValueD2, concValueD2))

    val valueA = store1("a")
    val concValueA1 = valueA._2
    val symValueA1 = valueA._1.replaceIdentifier(Some("a"))

    val valueB = store1("b")
    val concValueB = valueB._2
    val symValueB = valueB._1.replaceIdentifier(Some("b"))

    val concPred1 = concValueA1 >= 2
    val symPred1 = RelationalExpression(symValueA1, IntGreaterThanEqual, i(2))
    val bc1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred1, Nil), dummyExecutionState)
    val pc1 = ConstraintWithStoreUpdate(bc1, concPred1)

    val concPred2 = Random.nextBoolean()
    lazy val symPred2 = RelationalExpression(input(newInputId()), IntEqual, i(5))
    lazy val bc2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred2, Nil), dummyExecutionState)
    lazy val pc2 = ConstraintWithStoreUpdate(bc2, concPred2)

    val concPred3 = concValueB == 123
    val symPred3 = RelationalExpression(symValueB, IntEqual, i(123))
    val bc3: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred3, Nil), dummyExecutionState)
    val pc3 = ConstraintWithStoreUpdate(bc3, concPred3)

    val (assignments, store3, constraints: List[ConstraintWithStoreUpdate[ConstraintES]]) =
   // if (a >= 2)
      if (concPred1) {
        Logger.n(Console.GREEN + "Event C, if (a >= 2) true" + Console.RESET)
      // b = 1
      // c++
      // b = c
      // b++
      val valueB = store1("b")
      val valueC = store1("c")
      val concValueB1 = valueB._2
      val symValueB1 = valueB._1.replaceIdentifier(Some("b"))
      val concValueC1 = valueC._2
      val symValueC1 = valueC._1.replaceIdentifier(Some("c"))

      // b = 1
      val concValueB2 = 1
      val symValueB2 = i(concValueB2)

      // c++
      val concValueC2 = concValueC1 + 1
      val symValueC2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC1, i(1)))

      // b = c
      val concValueB3 = concValueC2
      val symValueB3 = symValueC2.replaceIdentifier(Some("c"))

      // b++
      val concValueB4 = concValueB3 + 1
      val symValueB4 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueB3.replaceIdentifier(Some("b")), i(1)))

      val assignments = AssignmentsStoreUpdate(List("b" -> symValueB2, "c" -> symValueC2, "b" -> symValueB3, "b" -> symValueB4))
      val store2 = store1 + ("b" -> (symValueB4, concValueB4), "c" -> (symValueC2, concValueC2))
      (assignments, store2, List(pc1))
    } else if (concPred2) { // else if (i_<id> == 5)
        Logger.n(Console.GREEN + "Event C, else if (i_<id> == 5) true" + Console.RESET)
      // a++

      val concValueA2 = concValueA1 + 1
      val symValueA2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueA1, i(1)))

      val assignments = AssignmentsStoreUpdate(List("a" -> symValueA2))
      val store2 = store1 + ("a" -> (symValueA2, concValueA2))
      (assignments, store2, List(pc1, pc2))
    } else if (concPred3) {
        Logger.n(Console.GREEN + "Event C, else if (b = 123) true" + Console.RESET)
      // e += 2
      val valueE = store1("e")
      val concValueE1 = valueE._2
      val symValueE1 = valueE._1.replaceIdentifier(Some("e"))

      val concValueE2 = concValueE1 + 2
      val symValueE2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueE1, i(2)))

      val assignments = AssignmentsStoreUpdate(List("e" -> symValueE2))
      val store2 = store1 + ("e" -> (symValueE2, concValueE2))
      (assignments, store2, List(pc1, pc2, pc3))
    } else {
        Logger.n(Console.GREEN + "Event C, if (a >= 2) else if (i_<id> == 5) else if (b = 123) else" + Console.RESET)
      // c = 321
      val concValueC2 = 321
      val symValueC2 = i(concValueC2)
      val assignments = AssignmentsStoreUpdate(List("c" -> symValueC2))
      val store2 = store1 + ("c" -> (symValueC2, concValueC2))
      (assignments, store2, List(pc1, pc2, pc3))
    }

    val valueD3 = store3("d")
    val concValueD3 = valueD3._2
    val symValueD3 = valueD3._1.replaceIdentifier(Some("d"))

    // d++
    val concValueD4 = concValueD3 + 1
    val symValueD4 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueD3, i(1)))

    val assignmentD4 = AssignmentsStoreUpdate(List("d" -> symValueD4))
    val store4 = store3 + ("d" -> (symValueD4, concValueD4))

    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val storeUpdateElement1: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignmentD2)
    val storeUpdateElement2: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignments)
    val storeUpdateElement3: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignmentD4)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val pc = tcElement :: storeUpdateElement1 :: constraints ++ List(storeUpdateElement2, storeUpdateElement3, sttElement)

    val predsAreTrue = constraints.map((c: ConstraintWithStoreUpdate[ConstraintES]) => if (c.isTrue) "T" else "E").mkString
    val path: Path = targetIdToPath(targetId) + predsAreTrue + (if (isLastEventInSequence) "T" else "E")
    (pc, store4, path)

  }
}
