package backend.tree.merging.use_cases.automatically_generated.event_driven

import backend.TestConfigs.treeLogger
import backend._
import backend.execution_state.TargetEndExecutionState
import backend.execution_state.store.{AssignmentsStoreUpdate, EnterScopeUpdate}
import backend.expression.Util._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.checks.NoDuplicateExecutionStatesChecker
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path._
import backend.tree.merging._
import backend.tree.merging.use_cases.automatically_generated._

import scala.annotation.tailrec
import scala.collection.immutable.StringOps
import scala.util.Random

class AutomaticallyGeneratedEventDrivenTests
  extends SetUseDebugToTrueTester
  with RandomlyGeneratedTests
  with UsesRandomInt {

  import ConstraintESAllInOne._

  protected val identifiers: Set[String] = Set("a", "b", "c", unassigned)

  type MakeSimulatedUserEvent = (Int, Int) => SimulatedUserEventTraverseOnce
  type MakeSimulatedUserEventMultipleTraversals = (Int, Int) => SimulatedUserEventTraverseMultipleTimes
  def randomlyTraverse(userEvents: List[MakeSimulatedUserEvent], eventSequenceLength: Int): TreeTraversalResult[Int] = {
    val nrOfEvents = userEvents.length
    val initial: TreeTraversalResult[Int] = (Nil, Map(), Nil)
    1.to(eventSequenceLength).foldLeft(initial)((acc, eventId) => {
      val (pc, store, path) = acc
      val randomEvent = userEvents(Random.nextInt(nrOfEvents))(eventId, eventSequenceLength)
      val (traversedPc, newStore, traversedPath) = randomEvent.traverse(store)
      (pc ++ traversedPc, newStore, path ++ traversedPath)
    })
  }

  def makePermutationsOfLength(numberOfEvents: Int, length: Int): List[List[Int]] = {
    val listOfNumbers = 0.until(numberOfEvents).toList
    val start = listOfNumbers.map(List(_))
    @tailrec
    def loop(acc: List[List[Int]], counter: Int): List[List[Int]] = {
      if (counter >= length) {
        acc
      } else {
        val newAcc = acc.flatMap(lst => listOfNumbers.map(number => number :: lst))
        loop(newAcc, counter + 1)
      }
    }
    loop(start, 1)
  }

  def nrOfEventSequencesOfLength(nrOfEvents: Int, length: Int): Int = {
    Math.pow(nrOfEvents, length).toInt
  }

  def generateRandomEventSequencesOfLength(nrOfEvents: Int, eventSequenceLength: Int, permutationsToTake: Int): List[List[Int]] = {
    val permutations = makePermutationsOfLength(nrOfEvents, eventSequenceLength)
    permutations.take(permutationsToTake)
  }

  def generateAllEventSequences(nrOfEvents: Int): List[List[Int]] = {
    @tailrec
    def loop(acc: List[List[Int]], size: Int, eventSequenceLength: Int): List[List[Int]] = {
      if (size > pathsToTraverse) {
        acc.take(pathsToTraverse)
      } else {
        val nrOfEventSequences = nrOfEventSequencesOfLength(nrOfEvents, eventSequenceLength)
        val eventSequencesOfLength = generateRandomEventSequencesOfLength(nrOfEvents, eventSequenceLength, nrOfEventSequences)
        loop(acc ++ eventSequencesOfLength, size + nrOfEventSequences, eventSequenceLength + 1)
      }
    }
    loop(Nil, 0, 1)
  }

  def doTestWithSeed(seed: Int, userEvents: List[MakeSimulatedUserEvent], initialMap: Store[Int]): Unit = {
    scala.util.Random.setSeed(seed)


    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    val pathFollower = new ConstraintESNoEventsPathFollowerSkipStopTestingTargets()(SpecialConstraintESNegater)
    val toCheck = new CheckRandomOldPaths[Int](pathFollower)

    val preamble = List(
      StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(identifiers, 0)),
      StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList))
    )
    val nrOfEventsInThisTest = userEvents.length
    val processesInfo = makeProcessesInfo(nrOfEventsInThisTest).processesInfo

    val eventSequences = generateAllEventSequences(userEvents.length)
    eventSequences.zipWithIndex.foreach(eventSequenceIdxTuple => {
      val (eventSequence, idx) = eventSequenceIdxTuple
      val iteration = idx + 1
      printIteration(iteration)
      println(s"Event sequence = $eventSequence")
      val initial: TreeTraversalResult[Int] = (preamble, initialMap, Nil)

      val eventSequenceLength = eventSequence.length
      eventSequence.zipWithIndex.foldLeft(initial)((acc, eventIdTargetIdTuple) => {
        val (targetId, eventId) = eventIdTargetIdTuple
//        if (seed == 2) {
//          treeLogger.setPath(s"output/entire_store_true/true_s2_it${iteration}_e${eventId}.dot")
//        }
        val simulatedEvent = userEvents(targetId)(eventId, eventSequenceLength)
        val (pc, store, path) = acc
        val (newPCPart, newStore, newPathPart) = simulatedEvent.traverse(store)
        val completePC = pc ++ newPCPart
        val completePath = path ++ newPathPart

        val isLastEvent = eventId >= eventSequenceLength - 1
        val treePathAdded = reporter.addExploredPath(completePC, false)
        var root = reporter.getRoot.get
        treeLogger.d(root)
        val symbolicStore = storeToSymStore(newStore)
        nodeMerger.mergeWorkListStates(root, treePathAdded.init, TargetEndExecutionState(eventId))
        root = reporter.getRoot.get
        treeLogger.d(root)
        toCheck.addToChecks(completePath, "a", newStore("a")._2)
        toCheck.addToChecks(completePath, "b", newStore("b")._2)
        toCheck.addToChecks(completePath, "c", newStore("c")._2)
        if (initialMap.contains("d")) {
          toCheck.addToChecks(completePath, "d", newStore("d")._2)
        }
        if (initialMap.contains("e")) {
          toCheck.addToChecks(completePath, "e", newStore("e")._2)
        }
        toCheck.addToChecks(completePath, unassigned, valueUnassigned)
        toCheck.doAllChecks(root, processesInfo)

        if (isLastEvent) {
          val (valueA, comparisonA) = createComparisonExp(newStore, "a", -97)
          val (valueB, comparisonB) = createComparisonExp(newStore, "b", -98)
          val (valueC, comparisonC) = createComparisonExp(newStore, "c", -99)
          val (_, comparisonUnassigned) = createComparisonExp(newStore, unassigned, -200)
          val optDUsed = initialMap.get("d")
          val (optValueD, optValueE, comparisonD: BooleanExpression) = optDUsed match {
            case Some(_) =>
              val (valueD, comparisonD) = createComparisonExp(newStore, "d", -100)
              val (valueE, comparisonE) = createComparisonExp(newStore, "e", -101)
              (Some(valueD), Some(valueE), LogicalBinaryExpression(
                comparisonC, LogicalAnd, LogicalBinaryExpression(
                  comparisonD, LogicalAnd,
                  LogicalBinaryExpression(comparisonE, LogicalAnd, comparisonUnassigned))))
            case None =>
              (None, None, comparisonUnassigned)
          }

          val comparisonExp1 = LogicalBinaryExpression(
            comparisonA,
            LogicalAnd,
            LogicalBinaryExpression(
              comparisonB,
              LogicalAnd,
              LogicalBinaryExpression(comparisonC, LogicalAnd, comparisonD)))
          val comparisonBc1 = BranchConstraint(comparisonExp1, Nil)
          val comparisonConstraint1: ConstraintES =
            EventConstraintWithExecutionState(comparisonBc1, dummyExecutionState)
          val comparisonPC1 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint1, true)
          val completeProgramConstraint1 = completePC :+ comparisonPC1
          reporter.addExploredPath(completeProgramConstraint1, true)
          root = reporter.getRoot.get
          treeLogger.e(root)

          checkValueOfIdentifier(pathFollower, completePath, valueA, input(-97), root, processesInfo)
          checkValueOfIdentifier(pathFollower, completePath, valueB, input(-98), root, processesInfo)
          checkValueOfIdentifier(pathFollower, completePath, valueC, input(-99), root, processesInfo)
          optValueD.foreach(valueD => checkValueOfIdentifier(pathFollower, completePath, valueD, input(-100), root, processesInfo))
          optValueE.foreach(valueE => checkValueOfIdentifier(pathFollower, completePath, valueE, input(-101), root, processesInfo))
          checkValueOfIdentifier(pathFollower, completePath, valueUnassigned, input(-200), root, processesInfo)
        }

        (completePC, newStore, completePath)
      })
      treeLogger.e(reporter.getRoot.get)
      NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
    })
  }

  test("Randomly generate 20 event-driven trees, consisting of events A and B, and traverse them") {
    val nrOfEvents = 2
    val makeUserEvents = List(
      SimulatedUserEventA(0, _, _, EventAInputIdGenerator, nrOfEvents),
      SimulatedUserEventB(1, _, _, EventBInputIdGenerator, nrOfEvents))
    val initialMap = Map(
      "a" -> (i(0), 0),
      "b" -> (i(0), 0),
      "c" -> (i(0), 0),
      unassigned -> (i(valueUnassigned, "unassigned"), 0)
    )
    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed, makeUserEvents, initialMap)
      })
  }

  test("Randomly generate 20 event-driven trees, consisting of events A and B, have inputs use the same process id") {
    val nrOfEvents = 2
    val makeUserEvents = List(
      SimulatedUserEventA(0, _, _, NeutralEventInputIdGenerator, nrOfEvents),
      SimulatedUserEventB(1, _, _, NeutralEventInputIdGenerator, nrOfEvents))
    val initialMap = Map(
      "a" -> (i(0), 0),
      "b" -> (i(0), 0),
      "c" -> (i(0), 0),
      unassigned -> (i(valueUnassigned, "unassigned"), 0)
    )
    (nrOfTreesToCreate + 1).to(nrOfTreesToCreate * 2)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed, makeUserEvents, initialMap)
      })
  }

  test("Randomly generate 20 event-driven trees, consisting of events A, B, and C, and traverse them") {
    val nrOfEvents = 3
    val makeUserEvents = List(
      SimulatedUserEventA(0, _, _, EventAInputIdGenerator, nrOfEvents),
      SimulatedUserEventB(1, _, _, EventBInputIdGenerator, nrOfEvents),
      SimulatedUserEventC(2, _, _, EventCInputIdGenerator, nrOfEvents))
    val initialMap = Map(
      "a" -> (i(0), 0),
      "b" -> (i(0), 0),
      "c" -> (i(0), 0),
      "d" -> (i(0), 0),
      "e" -> (i(0), 0),
      unassigned -> (i(valueUnassigned, "unassigned"), 0)
    )
    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed, makeUserEvents, initialMap)
      })
  }

  test("Randomly generate 20 event-driven trees, consisting of events A, B, C, and D, and traverse them") {
    val nrOfEvents = 4
    val makeUserEvents = List(
      SimulatedUserEventA(0, _, _, EventAInputIdGenerator, nrOfEvents),
      SimulatedUserEventB(1, _, _, EventBInputIdGenerator, nrOfEvents),
      SimulatedUserEventC(2, _, _, EventCInputIdGenerator, nrOfEvents),
      SimulatedUserEventD(3, _, _, EventDInputIdGenerator, nrOfEvents))
    val initialMap = Map(
      "a" -> (i(0), 0),
      "b" -> (i(0), 0),
      "c" -> (i(0), 0),
      "d" -> (i(0), 0),
      "e" -> (i(0), 0),
      unassigned -> (i(valueUnassigned, "unassigned"), 0)
    )
    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed, makeUserEvents, initialMap)
      })
  }



  def doTestWithSeedSmall(seed: Int, userEvents: List[MakeSimulatedUserEvent], initialMap: Store[Int]): Unit = {
    scala.util.Random.setSeed(seed)


    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    val pathFollower = new ConstraintESNoEventsPathFollowerSkipStopTestingTargets[Unit]()(SpecialConstraintESNegater)
    val toCheck = new CheckRandomOldPaths[Int](pathFollower)

    val preamble = List(
      StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(identifiers, 0)),
      StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList))
    )
    val nrOfEventsInThisTest = userEvents.length
    val processesInfo = makeProcessesInfo(nrOfEventsInThisTest).processesInfo

    val eventSequences = generateAllEventSequences(userEvents.length)
    eventSequences.zipWithIndex.foreach(eventSequenceIdxTuple => {
      val (eventSequence, idx) = eventSequenceIdxTuple
      val iteration = idx + 1
      printIteration(iteration)
      println(s"Event sequence = $eventSequence")
      val initial: TreeTraversalResult[Int] = (preamble, initialMap, Nil)

      val eventSequenceLength = eventSequence.length
      eventSequence.zipWithIndex.foldLeft(initial)((acc, eventIdTargetIdTuple) => {
        val (targetId, eventId) = eventIdTargetIdTuple
        val simulatedEvent = userEvents(targetId)(eventId, eventSequenceLength)
        val (pc, store, path) = acc
        val (newPCPart, newStore, newPathPart) = simulatedEvent.traverse(store)
        val completePC = pc ++ newPCPart
        val completePath = path ++ newPathPart

        val isLastEvent = eventId >= eventSequenceLength - 1
        val treePathAdded = reporter.addExploredPath(completePC, false)
        var root = reporter.getRoot.get
        treeLogger.d(root)
        val symbolicStore = storeToSymStore(newStore)
        nodeMerger.mergeWorkListStates(root, treePathAdded.init, TargetEndExecutionState(eventId))
        root = reporter.getRoot.get
        treeLogger.d(root)
        toCheck.addToChecks(completePath, "c", newStore("c")._2)
        toCheck.addToChecks(completePath, "d", newStore("d")._2)
        toCheck.addToChecks(completePath, unassigned, valueUnassigned)
        toCheck.doAllChecks(root, processesInfo)

        if (isLastEvent) {
          val (valueC, comparisonC) = createComparisonExp(newStore, "c", -99)
          val (valueD, comparisonD) = createComparisonExp(newStore, "d", -100)
          val (_, comparisonUnassigned) = createComparisonExp(newStore, unassigned, -200)

          val comparisonExp1 = LogicalBinaryExpression(
            comparisonC,
            LogicalAnd,
            LogicalBinaryExpression(
              comparisonD,
              LogicalAnd,
              comparisonUnassigned))
          val comparisonBc1 = BranchConstraint(comparisonExp1, Nil)
          val comparisonConstraint1: ConstraintES =
            EventConstraintWithExecutionState(comparisonBc1, dummyExecutionState)
          val comparisonPC1 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint1, true)
          val completeProgramConstraint1 = completePC :+ comparisonPC1
          reporter.addExploredPath(completeProgramConstraint1, true)
          root = reporter.getRoot.get
          treeLogger.e(root)

          checkValueOfIdentifier(pathFollower, completePath, valueC, input(-99), root, processesInfo)
          checkValueOfIdentifier(pathFollower, completePath, valueD, input(-100), root, processesInfo)
          checkValueOfIdentifier(pathFollower, completePath, valueUnassigned, input(-200), root, processesInfo)
        }

        (completePC, newStore, completePath)
      })
      treeLogger.e(reporter.getRoot.get)
      NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
    })
  }

  test("Small test with events CC and DD") {
    val nrOfEvents = 2
    val makeUserEvents = List(
      SimulatedUserEventCC(0, _, _, EventCInputIdGenerator, nrOfEvents),
      SimulatedUserEventDD(1, _, _, EventDInputIdGenerator, nrOfEvents))
    val initialMap = Map(
      "c" -> (i(0), 0),
      "d" -> (i(0), 0),
      unassigned -> (i(valueUnassigned, "unassigned"), 0)
    )
    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeedSmall(seed, makeUserEvents, initialMap)
      })
  }

  def doTestWithMultipleTraversals(
    seed: Int,
    userEvents: List[MakeSimulatedUserEventMultipleTraversals],
    initialMap: Store[Int],
    identifiers: Set[String]
  ): Unit = {
    scala.util.Random.setSeed(seed)


    val reporter = new ConstraintESReporter(MergingMode, None)
    val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
    val pathFollower = new ConstraintESNoEventsPathFollowerSkipStopTestingTargets[Unit](true)(SpecialConstraintESNegater)
    val toCheck = new OnlyNewPathsChecked[Int](pathFollower)

    val preamble = List(
      StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(identifiers, 0)),
      StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList))
    )
    val nrOfEventsInThisTest = userEvents.length
    val processesInfo = makeProcessesInfo(nrOfEventsInThisTest).processesInfo

    val eventSequences = generateAllEventSequences(userEvents.length)
    eventSequences.zipWithIndex.foreach(eventSequenceIdxTuple => {
      val (eventSequence, idx) = eventSequenceIdxTuple
      val iteration = idx + 1
      printIteration(iteration)
      println(s"Event sequence = $eventSequence")
      val initial: TreeTraversalResult[Int] = (preamble, initialMap, Nil)

      val eventSequenceLength = eventSequence.length
      eventSequence.zipWithIndex.foldLeft(initial)((acc, eventIdTargetIdTuple) => {
        val (targetId, eventId) = eventIdTargetIdTuple
        val simulatedEvent = userEvents(targetId)(eventId, eventSequenceLength)
        val (pc, store, path) = acc
        val traversalInfos = simulatedEvent.traverse(store, eventSequence.take(eventId + 1).map(eventId => (0, eventId)))
        val nrOfTraversalInfos = traversalInfos.size
        val result = traversalInfos.zipWithIndex.foldLeft(acc)((_, traversalInfoTuple) => {
          val traversalInfo = traversalInfoTuple._1
          val idx = traversalInfoTuple._2
          val (newPCPart, newStore, newPathPart, executionState) = traversalInfo

          val completePC = pc ++ newPCPart
          val completePath = path ++ newPathPart

          val isLastEvent = eventId >= eventSequenceLength - 1
          val treePathAdded = reporter.addExploredPath(completePC, false)
          var root = reporter.getRoot.get
          treeLogger.d(root)
          nodeMerger.mergeWorkListStates(root, treePathAdded.init, executionState)
          root = reporter.getRoot.get
          treeLogger.d(root)
          identifiers.foreach(identifier => {
            toCheck.addToChecks(completePath, identifier, newStore(identifier)._2)
          })
          toCheck.doAllChecks(root, processesInfo)

          if (isLastEvent && idx == nrOfTraversalInfos - 1) {
            type IdentifierAcc = (BooleanExpression, List[(SymbolicInt, Int)])
            val (comparisonExp1, identifiersValues) = identifiers.foldLeft[IdentifierAcc]((SymbolicBool(true), Nil))((acc, identifier) => {
              val inputId = identifierToInputId(identifier)
              val (identifierValue: SymbolicInt, identifierComparison) = createComparisonExp(newStore, identifier, inputId)
              val newAccExp = LogicalBinaryExpression(identifierComparison, LogicalAnd, acc._1)
              (newAccExp, acc._2 :+ (identifierValue, inputId))
            })
            val comparisonBc1 = BranchConstraint(comparisonExp1, Nil)
            val comparisonConstraint1: ConstraintES = EventConstraintWithExecutionState(comparisonBc1, dummyExecutionState)
            val comparisonPC1 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint1, true)
            val completeProgramConstraint1 = completePC :+ comparisonPC1
            reporter.addExploredPath(completeProgramConstraint1, true)
            root = reporter.getRoot.get
            treeLogger.e(root)
            NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))

            identifiersValues.foreach(tuple => {
              checkValueOfIdentifier(pathFollower, completePath, tuple._1, input(tuple._2), root, processesInfo)
            })
          }
          (completePC, newStore, completePath)
        })
        result
      })
      treeLogger.e(reporter.getRoot.get)
    })
  }

  def identifierToInputId(identifier: String): Int = {
    -(identifier: StringOps).headOption.map(_.toInt).getOrElse(1000)
  }
  test("Test two small events, with almost identical branches in both") {
    val nrOfEvents = 2
    val makeUserEvents = List(
      SimulatedUserEventWithTwoIfStatements1(0, _, _, EventCInputIdGenerator, nrOfEvents, "x", 1, 10),
      SimulatedUserEventWithTwoIfStatements2(1, _, _, EventDInputIdGenerator, nrOfEvents, "y", 2, 20))
    val initialMap = Map(
      "x" -> (i(0), 0),
      "y" -> (i(0), 0)
    )
    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithMultipleTraversals(seed, makeUserEvents, initialMap, Set("x", "y"))
      })
  }

  test("Test three small events, with almost identical branches in both") {
    val nrOfEvents = 3
    val makeUserEvents = List(
      SimulatedUserEventWithTwoIfStatements1(0, _, _, NeutralEventInputIdGenerator, nrOfEvents, "x", 1, 10),
      SimulatedUserEventWithTwoIfStatements2(1, _, _, NeutralEventInputIdGenerator, nrOfEvents, "y", 2, 20),
      SimulatedUserEventWithTwoIfStatements3(2, _, _, NeutralEventInputIdGenerator, nrOfEvents, "z", 3, 30))
    val initialMap = Map(
      "x" -> (i(0), 0),
      "y" -> (i(0), 0),
      "z" -> (i(0), 0)
    )
    (nrOfTreesToCreate + 1).to(nrOfTreesToCreate * 2)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithMultipleTraversals(seed, makeUserEvents, initialMap, Set("x", "y", "z"))
      })
  }

}
