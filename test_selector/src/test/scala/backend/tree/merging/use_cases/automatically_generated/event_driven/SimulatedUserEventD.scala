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

case class SimulatedUserEventD(
  targetId: Int,
  eventId: Int,
  eventSequenceLength: Int,
  inputIdGenerator: InputIdGenerator,
  nrOfEventsInThisTest: Int
) extends SimulatedUserEventTraverseOnce {
  def traverse(store: Store[Int]): TreeTraversalResult[Int] = {
    // if (i_<id> == 4) {
    //   d *= 2
    //   if (i_<id> == 41) {
    //     if (d >= a) {
    //       d = 0
    //     } else if (c >= a) {
    //       c -= 10
    //     }
    //     c++
    //     if (i_<id> == 42) {
    //       a += 2
    //     } else {
    //       a += 4
    //     }
    //   }
    // } else if (b >= c) {
    //   e = 10
    // } else {
    //   b = 456
    // }
    // e++
    //
    val concPred_1 = Random.nextBoolean()
    lazy val symPred_1 = RelationalExpression(input(newInputId(), targetId), IntEqual, i(4))
    lazy val bc_1: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred_1, Nil), dummyExecutionState)
    lazy val pc_1 = ConstraintWithStoreUpdate(bc_1, concPred_1) // if (i_<id> == 4)

    val valueB_0 = store("b")
    val concValueB_0 = valueB_0._2
    val symValueB_0 = valueB_0._1.replaceIdentifier(Some("b"))

    val valueC_0 = store("c")
    val concValueC_0 = valueC_0._2
    val symValueC_0 = valueC_0._1.replaceIdentifier(Some("c"))

    val concPred_2 = concValueB_0 >= concValueC_0
    val symPred_2 = RelationalExpression(symValueB_0, IntGreaterThanEqual, symValueC_0)
    val bc_2: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred_2, Nil), dummyExecutionState)
    val pc_2 = ConstraintWithStoreUpdate(bc_2, concPred_2) // else if (b >= c)

    val (store2: Store[Int], constraints: List[ConstraintWithStoreUpdate[ConstraintES]]) =
   // if (i_<id> == 4) {
      if (concPred_1) {
        Logger.n(Console.GREEN + "Event D, if (i_<id> == 4) true" + Console.RESET)
        val valueD_T = store("d")
        val concValueD_T = valueD_T._2
        val symValueD_T = valueD_T._1.replaceIdentifier(Some("d"))

        // d *= 2
        val concValueD_T2 = concValueD_T * 2
        val symValueD_T2 = ArithmeticalVariadicOperationExpression(IntTimes, List(symValueD_T, i(2)))
        val assignmentD_T2 = AssignmentsStoreUpdate(List("d" -> symValueD_T2))
        val store_T2 = store + ("d" -> (symValueD_T2, concValueD_T2))
        val pc_assignmentD_T2 = StoreUpdatePCElement(assignmentD_T2) // d *= 2

        // i_<id> == 41
        val concPred_T = Random.nextBoolean()
        val symPred_T = RelationalExpression(input(newInputId(), targetId), IntEqual, i(41))
        val bc_T: ConstraintES = EventConstraintWithExecutionState(
          BranchConstraint(symPred_T, Nil), dummyExecutionState)
        val pc_T = ConstraintWithStoreUpdate(bc_T, concPred_T) // if (i_<id> == 41)

        // constraints_T_3 == if (i_<id> == 41) {...} including all assignments within
        val (store_T_3: Store[Int], constraints_T_3: List[ConstraintWithStoreUpdate[ConstraintES]]) =
          if (concPred_T) { // if (i_<id> == 41)
            Logger.n(Console.GREEN + "Event D, if (i_<id> == 41) true" + Console.RESET)
            val valueD_T_T = store_T2("d")
            val concValueD_T_T = valueD_T_T._2
            val symValueD_T_T = valueD_T_T._1.replaceIdentifier(Some("d"))

            val valueA_T_T = store_T2("a")
            val concValueA_T_T = valueA_T_T._2
            val symValueA_T_T = valueA_T_T._1.replaceIdentifier(Some("a"))

            // d >= a
            val concPred_T_T = concValueD_T_T >= concValueA_T_T
            val symPred_T_T = RelationalExpression(symValueD_T_T, IntGreaterThanEqual, symValueA_T_T)
            val bc1_T_T: ConstraintES = EventConstraintWithExecutionState(
              BranchConstraint(symPred_T_T, Nil), dummyExecutionState)
            val pc1_T_T = ConstraintWithStoreUpdate(bc1_T_T, concPred_T_T)

            val valueC_T_T = store_T2("c")
            val concValueC_T_T = valueC_T_T._2
            val symValueC_T_T = valueC_T_T._1.replaceIdentifier(Some("c"))

            // c >= a
            val concPred_T_T2 = concValueC_T_T >= concValueA_T_T
            val symPred_T_T2 = RelationalExpression(symValueC_T_T, IntGreaterThanEqual, symValueA_T_T)
            val bc_T_T2: ConstraintES = EventConstraintWithExecutionState(
              BranchConstraint(symPred_T_T2, Nil), dummyExecutionState)
            val pc_T_T2 = ConstraintWithStoreUpdate(bc_T_T2, concPred_T_T2)

            // constraints_T_T2 == if (d >= a) {} else if (c >= a) {} else {} including all assignments within
            val (store_T_T2: Store[Int], constraints_T_T2: List[ConstraintWithStoreUpdate[ConstraintES]]) =
              if (concPred_T_T) { // if (d >= a)
                Logger.n(Console.GREEN + "Event D, if (d >= a) true" + Console.RESET)
                // d = 0
                val concValueD_T_T_T = 0
                val symValueD_T_T_T = i(concValueD_T_T_T)
                val assignment_T_T_T = AssignmentsStoreUpdate(List("d" -> symValueD_T_T_T))
                val store_T_T_T = store + ("d" -> (symValueD_T_T_T, concValueD_T_T_T))
                val pc_T_T_T = StoreUpdatePCElement(assignment_T_T_T)
                (store_T_T_T, List(pc1_T_T, pc_T_T_T))
              } else if (concPred_T_T2) { // else if (c >= a)
                Logger.n(Console.GREEN + "Event D, else if (c >= a) true" + Console.RESET)
                // c -= 10
                val concValueC_T_T_F_T = concValueC_T_T - 10
                val symValueC_T_T_F_T = ArithmeticalVariadicOperationExpression(IntMinus, List(symValueC_T_T, i(10)))
                val assignment_T_T_F_T = AssignmentsStoreUpdate(List("c" -> symValueC_T_T_F_T))
                val store_T_T_F_T = store + ("c" -> (symValueC_T_T_F_T, concValueC_T_T_F_T))
                val pc_T_T_F_T = StoreUpdatePCElement(assignment_T_T_F_T)
                (store_T_T_F_T, List(pc1_T_T, pc_T_T2, pc_T_T_F_T))
              } else {
                Logger.n(Console.GREEN + "Event D, if (d >= a) else if (c >= a) false" + Console.RESET)
                (store_T2, List(pc1_T_T, pc_T_T2))
              }

            val valueC_T_T3 = store_T_T2("c")
            val concValueC_T_T3 = valueC_T_T3._2
            val symValueC_T_T3 = valueC_T_T3._1.replaceIdentifier(Some("c"))

            // c++
            val concValueC_T_T4 = concValueC_T_T3 + 1
            val symValueC_T_T4 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueC_T_T3, i(1)))
            val assignment_T_T4 = AssignmentsStoreUpdate(List("c" -> symValueC_T_T4))
            val store_T_T4 = store_T_T2 + ("c" -> (symValueC_T_T4, concValueC_T_T4))
            val pc_T_T4 = StoreUpdatePCElement(assignment_T_T4) // c++

            // i_<id> == 42
            val concPred_T_T5 = Random.nextBoolean()
            val symPred_T_T5 = RelationalExpression(input(newInputId(), targetId), IntEqual, i(42))
            val bc_T_T5: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(symPred_T_T5, Nil), dummyExecutionState)
            val pc_T_T5 = ConstraintWithStoreUpdate(bc_T_T5, concPred_T_T5) // if (i_<id> == 42)
            val (pc_T_T6, store_T_T6) = // assignments in if (i_<id> == 42) {...} else {...}
              if (concPred_T_T5) { // if (i_<id> == 42)
                Logger.n(Console.GREEN + "Event D, if (i_<id> == 42) true" + Console.RESET)
                val valueA_T_T3_T = store_T_T4("a")
                val concValueA_T_T3_T = valueA_T_T3_T._2
                val symValueA_T_T3_T = valueA_T_T3_T._1.replaceIdentifier(Some("a"))

                // a += 2
                val concValueA_T_T3_T2 = concValueA_T_T3_T + 2
                val symValueA_T_T3_T2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueA_T_T3_T, i(2)))
                val assignment_T_T3_T2 = AssignmentsStoreUpdate(List("a" -> symValueA_T_T3_T2))
                val store_T_T3_T2 = store_T_T4 + ("a" -> (symValueA_T_T3_T2, concValueA_T_T3_T2))
                val pc_T_T3_T2 = StoreUpdatePCElement(assignment_T_T3_T2)
                (pc_T_T3_T2, store_T_T3_T2)
              } else {
                Logger.n(Console.GREEN + "Event D, if (i_<id> == 42) false" + Console.RESET)
                val valueA_T_T3_F = store_T_T4("a")
                val concValueA_T_T3_F = valueA_T_T3_F._2
                val symValueA_T_T3_F = valueA_T_T3_F._1.replaceIdentifier(Some("a"))

                // a += 4
                val concValueA_T_T3_F2 = concValueA_T_T3_F + 4
                val symValueA_T_T3_F2 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueA_T_T3_F, i(4)))
                val assignment_T_T3_F2 = AssignmentsStoreUpdate(List("a" -> symValueA_T_T3_F2))
                val store_T_T3_F2 = store_T_T4 + ("a" -> (symValueA_T_T3_F2, concValueA_T_T3_F2))
                val pc_T_T3_F2 = StoreUpdatePCElement(assignment_T_T3_F2)
                (pc_T_T3_F2, store_T_T3_F2)
              }
            (store_T_T6, pc_T :: constraints_T_T2 ++ List(pc_T_T4, pc_T_T5, pc_T_T6))
          } else {
            Logger.n(Console.GREEN + "Event D, if (i_<id> == 41) false" + Console.RESET)
            (store_T2, List(pc_T))
          }
        (store_T_3, pc_1 :: pc_assignmentD_T2 :: constraints_T_3)
      } else if (concPred_2) { // else if (b >= c)
        Logger.n(Console.GREEN + "Event D, else if (b >= c)" + Console.RESET)
        // e = 10
        val concValueE_F_T = 10
        val symValueE_F_T = i(concValueE_F_T)
        val assignment_F_T = AssignmentsStoreUpdate(List("e" -> symValueE_F_T))
        val store_F_T = store + ("e" -> (symValueE_F_T, concValueE_F_T))
        val pc_F_T = StoreUpdatePCElement(assignment_F_T)
        (store_F_T, List(pc_1, pc_2, pc_F_T))
      } else { // else
        Logger.n(Console.GREEN + "Event D, if (i_<id> == 4) else if (b >= c) else " + Console.RESET)
        // b = 456
        val concValueB_F_F_T = 456
        val symValueB_F_F_T = i(concValueB_F_F_T)
        val assignment_F_F_T = AssignmentsStoreUpdate(List("b" -> symValueB_F_F_T))
        val store_F_F_T = store + ("b" -> (symValueB_F_F_T, concValueB_F_F_T))
        val pc_F_F_T = StoreUpdatePCElement(assignment_F_F_T)
        (store_F_F_T, List(pc_1, pc_2, pc_F_F_T))
      }

    val valueE_2 = store2("e")
    val concValueE_2 = valueE_2._2
    val symValueE_2 = valueE_2._1.replaceIdentifier(Some("e"))

    // e++
    val concValueE_3 = concValueE_2 + 1
    val symValueE_3 = ArithmeticalVariadicOperationExpression(IntPlus, List(symValueE_2, i(1)))
    val assignment_3 = AssignmentsStoreUpdate(List("e" -> symValueE_3))
    val store_3 = store2 + ("e" -> (symValueE_3, concValueE_3))
    val pc_3: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(assignment_3)



    val eventsAlreadyChosen = Map(processId -> Set(targetId))

    val tc = EventConstraintWithExecutionState(
      TargetChosen(
        eventId, processId, targetId, eventsAlreadyChosen, makeProcessesInfo(nrOfEventsInThisTest), processId, Nil),
      dummyExecutionState)
    val stt = EventConstraintWithExecutionState(
      StopTestingTargets(eventId, makeProcessesInfo(nrOfEventsInThisTest).processesInfo, Nil, processId),
      targetEndExecutionState)

    val tcElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(tc, true)
    val sttElement: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(stt, isLastEventInSequence)

    val intraEventConstraints = constraints :+ pc_3
    val allConstraints = (tcElement :: intraEventConstraints) :+ sttElement

    val predsAreTrue = intraEventConstraints.map({
      case _: StoreUpdatePCElement[ConstraintES] => ""
      case c: ConstraintWithStoreUpdate[ConstraintES] => if (c.isTrue) "T" else "E"
    }).mkString
    val path: Path = targetIdToPath(targetId) + predsAreTrue + (if (isLastEventInSequence) "T" else "E")
    (allConstraints, store_3, path)

  }
}
