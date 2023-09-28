package backend.tree.merging.use_cases.automatically_generated

import backend.TestConfigs.treeLogger
import backend._
import backend.execution_state._
import backend.execution_state.store._
import backend.expression.Util._
import backend.expression._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.checks.NoDuplicateExecutionStatesChecker
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state.ConstraintESAllInOne._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.follow_path._
import backend.tree.merging.{ConstraintESNodeMerger, RandomlyGeneratedTests}

import scala.annotation.tailrec
import scala.util.Random

class AutomaticallyGeneratedUseCases extends SetUseDebugToTrueTester with RandomlyGeneratedTests {

  protected def updateStoreAndPath(
    updates: Option[GenerateAssignmentsStoreUpdate[Int]],
    path: PathConstraintWithStoreUpdates[ConstraintES],
    store: Store[Int]
  ): (PathConstraintWithStoreUpdates[ConstraintES], Store[Int]) = {
    val optTuple = updates.map(_.generate(store))
    optTuple match {
      case None => (path, store)
      case Some((assignmentsStoreUpdates, newStore)) =>
        val storeConstraint = StoreUpdatePCElement[ConstraintES](assignmentsStoreUpdates)
        val newPath: PathConstraintWithStoreUpdates[ConstraintES] = path :+ storeConstraint
        (newPath, newStore)
    }
  }

  @tailrec
  final protected def randomlyTraverseTree(
    tree: GeneratedTreeNode,
    programConstraint: PathConstraintWithStoreUpdates[ConstraintES],
    store: Store[Int],
    path: Path
  ): TreeTraversalResult[Int] = tree match {
    case LeafNode(_) => (programConstraint, store, path)
    case BranchNode(input, inputValue, To(left, leftUpdates), To(right, rightUpdates)) =>
      val goLeft = Random.nextBoolean()
      val bc = BranchConstraint(RelationalExpression(input, IntEqual, i(inputValue)), Nil)
      val c: ConstraintES = EventConstraintWithExecutionState(bc, dummyExecutionState)
      val pcElement = ConstraintWithStoreUpdate(c, goLeft)
      val bcAddedPath = programConstraint :+ pcElement
      val (nextNode, newPath, newStore, updatedPath) = if (goLeft) {
        val (newPath, newStore) = updateStoreAndPath(leftUpdates, bcAddedPath, store)
        (left, newPath, newStore, path :+ ThenDirection)
      } else {
        val (newPath, newStore) = updateStoreAndPath(rightUpdates, bcAddedPath, store)
        (right, newPath, newStore, path :+ ElseDirection)
      }
      randomlyTraverseTree(nextNode, newPath, newStore, updatedPath)
  }

  test("Randomly generate 20 trees and randomly walk + merge through 50 paths, using 1 identifier") {
    def doTestWithSeed(seed: Int): Unit = {
      var pathsTraversed = Set[PathConstraintWithStoreUpdates[ConstraintES]]()
      val reporter = new ConstraintESReporter(MergingMode, None)
      val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
      val pathFollower = new ConstraintESPathFollower
      val toCheck = new CheckRandomOldPaths[Int](pathFollower)

      val identifiers = Set("x")
      val initialMap: Map[String, (SymbolicExpression, Int)] =
        identifiers.toList.zipWithIndex.foldLeft[Map[String, (SymbolicExpression, Int)]](Map())(
          (map, tuple) => map + (tuple._1 -> (i(tuple._2, tuple._1), tuple._2)))
      val mergeExecutionState =
        CodeLocationExecutionState(SerialCodePosition("file", 1), FunctionStack.empty, None, 0)
      val randomTreeConstructor = new RandomTreeConstructor(new RandomExpAssignmentGenerator(identifiers))

      scala.util.Random.setSeed(seed)
      val randomTree = randomTreeConstructor.constructRandomTree(3, 5)

      1.to(pathsToTraverse)
        .foreach(iteration => {
          printIteration(iteration)
          val (programConstraint, store, path) = randomlyTraverseTree(randomTree, Nil, initialMap, Nil)
          val symStore = storeToSymStore(store)
          if (!pathsTraversed.contains(programConstraint)) {
            pathsTraversed += programConstraint
            val preamble = StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(identifiers, 0))
            val (value, comparison) = createComparisonExp(store, "x", 120)
            val comparisonBc = BranchConstraint(comparison, Nil)
            val comparisonC: ConstraintES = EventConstraintWithExecutionState(comparisonBc, mergeExecutionState)
            val comparisonPC = ConstraintWithStoreUpdate[ConstraintES](comparisonC, true)
            val completePath: PathConstraintWithStoreUpdates[ConstraintES] = preamble :: (programConstraint :+ comparisonPC)
            val treePathAdded = reporter.addExploredPath(completePath, true)
            var root = reporter.getRoot.get
            treeLogger.d(root)
            nodeMerger.mergeWorkListStates(root, treePathAdded.init, mergeExecutionState)
            root = reporter.getRoot.get
            treeLogger.d(root)
            toCheck.addToChecks(path, "x", value.i)
            toCheck.doAllChecks(root)
          }
        })
      treeLogger.e(reporter.getRoot.get)
      NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
    }

    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed)
      })

  }

  test(
    "Randomly generate 20 trees and randomly walk + merge through 50 paths, using 3 merged identifiers and 1 unmerged identifier")
  {
    def doTestWithSeed(seed: Int): Unit = {
      var pathsTraversed = Set[PathConstraintWithStoreUpdates[ConstraintES]]()
      val reporter = new ConstraintESReporter(MergingMode, None)
      val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
      val pathFollower = new ConstraintESPathFollower
      val toCheck = new CheckRandomOldPaths[Int](pathFollower)
      val valueUnassigned = 42
      val unassigned = "unassigned"
      val identifiers = Set("x", "y", "z")
      val initialMap = Map(
        "x" -> (i(0, "x"), 0),
        "y" -> (i(0, "y"), 0),
        "z" -> (i(0, "z"), 0),
        unassigned -> (i(valueUnassigned, "unassigned"), valueUnassigned))
      val mergeExecutionState =
        CodeLocationExecutionState(SerialCodePosition("file", 1), FunctionStack.empty, None, 0)
      val randomTreeConstructor = new RandomTreeConstructor(new RandomExpAssignmentGenerator(identifiers))

      scala.util.Random.setSeed(seed)
      val randomTree = randomTreeConstructor.constructRandomTree(3, 5)


      1.to(pathsToTraverse)
        .foreach(iteration => {
          printIteration(iteration)
          val (programConstraint, store, path) = randomlyTraverseTree(randomTree, Nil, initialMap, Nil)
          println(s"Path followed this iteration: ${pathToString(path)}")
          val symStore = storeToSymStore(store)
          if (!pathsTraversed.contains(programConstraint)) {
            pathsTraversed += programConstraint
            val preamble = List(
              StoreUpdatePCElement[ConstraintES](EnterScopeUpdate(identifiers + unassigned, 0)),
              StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList)))

            val (valueX, comparisonX) = createComparisonExp(store, "x", 120)
            val (valueY, comparisonY) = createComparisonExp(store, "y", 121)
            val (valueZ, comparisonZ) = createComparisonExp(store, "z", 122)
            val (_, comparisonUnassigned) = createComparisonExp(store, unassigned, 200)

            val comparisonExp = LogicalBinaryExpression(
              comparisonX,
              LogicalAnd,
              LogicalBinaryExpression(
                comparisonY,
                LogicalAnd,
                LogicalBinaryExpression(comparisonZ, LogicalAnd, comparisonUnassigned)))
            val comparisonBc = BranchConstraint(comparisonExp, Nil)
            val comparisonC: ConstraintES =
              EventConstraintWithExecutionState(comparisonBc, mergeExecutionState)
            val comparisonPC = ConstraintWithStoreUpdate[ConstraintES](comparisonC, true)
            val completePath = preamble ++ programConstraint :+ comparisonPC
            val treePathAdded = reporter.addExploredPath(completePath, true)
            var root = reporter.getRoot.get
            nodeMerger.mergeWorkListStates(root, treePathAdded.init, mergeExecutionState)
            root = reporter.getRoot.get

            toCheck.addToChecks(path, "x", valueX.i)
            toCheck.addToChecks(path, "y", valueY.i)
            toCheck.addToChecks(path, "z", valueZ.i)
            toCheck.addToChecks(path, unassigned, valueUnassigned.i)
            toCheck.doAllChecks(root)
          }
        })
      treeLogger.e(reporter.getRoot.get)
      NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
    }

    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed)
      })

  }

  protected def runTwoStageTreeTest(
    tree1MaxInputId: Int,
    tree1MaxValue: Int,
    tree2MaxInputId: Int,
    tree2MaxValue: Int,
    identifiersInSecondStage: Set[String]
  ): Unit = {

    def doTestWithSeed(seed: Int): Unit = {
      scala.util.Random.setSeed(seed)

      var paths1Traversed = Set[Path]()
      var paths2Traversed = Set[Path]()
      val reporter = new ConstraintESReporter(MergingMode, None)
      val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
      val pathFollower = new ConstraintESPathFollower
      val toCheck = new CheckRandomOldPaths[Int](pathFollower)

      val valueUnassigned = 42
      val unassigned = "unassigned"
      val identifiers1 = Set("x", "y", "z")

      val initialMap = Map(
        "x" -> (i(0, "x"), 0),
        "y" -> (i(0, "y"), 0),
        "z" -> (i(0, "z"), 0),
        "a" -> (i(0, "a"), 0),
        "b" -> (i(0, "b"), 0),
        "c" -> (i(0, "c"), 0),
        "d" -> (i(0, "d"), 0),
        "e" -> (i(0, "e"), 0),
        unassigned -> (i(valueUnassigned, "unassigned"), 0)
      )

      val mergeExecutionState1 =
        CodeLocationExecutionState(SerialCodePosition("file", 1), FunctionStack.empty, None, 0)
      val randomTreeConstructor1 = new RandomTreeConstructor(new RandomExpAssignmentGenerator(identifiers1))

      val identifiersInSecondStage = Set("x", "y", "a", "b", "c", "d", "e")
      val mergeExecutionState2 =
        CodeLocationExecutionState(SerialCodePosition("file", 2), FunctionStack.empty, None, 0)
      val randomTreeConstructor2 = new RandomTreeConstructor(
        new RandomExpAssignmentGenerator(identifiersInSecondStage), tree1MaxInputId + 1)

      val randomTree1 = randomTreeConstructor1.constructRandomTree(tree1MaxInputId, tree1MaxValue)
      val randomTree2 = randomTreeConstructor2.constructRandomTree(tree2MaxInputId, tree2MaxValue)

      val maxUnsucessfulIterations = 30
      @tailrec
      def loopUntilEnoughPathsTraversed(pathsTraversed: Int, iterationsSinceLastTraversal: Int): Unit = {
        if (iterationsSinceLastTraversal > maxUnsucessfulIterations) {
          println(Console.RED + s"Stopping this tree traversal because of too many unsuccesful path traversals" + Console.RESET)
        } else if (pathsTraversed <= pathsToTraverse) {
          val (programConstraint1, store1, path1) = randomlyTraverseTree(randomTree1, Nil, initialMap, Nil)
          val symStore1 = storeToSymStore(store1)
          if (paths1Traversed.contains(path1)) {
            loopUntilEnoughPathsTraversed(pathsTraversed, iterationsSinceLastTraversal + 1)
          } else {
            printIteration(pathsTraversed)
            paths1Traversed += path1
            println(s"Follows path 1 $path1")
            val preamble = List(
              StoreUpdatePCElement[ConstraintES](
                EnterScopeUpdate(identifiers1 ++ identifiersInSecondStage + unassigned, 0)),
              StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList))
            )

            val (valueX, comparisonX) = createComparisonExp(store1, "x", 120)
            val (valueY, comparisonY) = createComparisonExp(store1, "y", 121)
            val (valueZ, comparisonZ) = createComparisonExp(store1, "z", 122)
            val (_, comparisonUnassigned) = createComparisonExp(store1, unassigned, 200)

            val comparisonExp1 = LogicalBinaryExpression(
              comparisonX,
              LogicalAnd,
              LogicalBinaryExpression(
                comparisonY,
                LogicalAnd,
                LogicalBinaryExpression(comparisonZ, LogicalAnd, comparisonUnassigned)))
            val comparisonBc1 = BranchConstraint(comparisonExp1, Nil)
            val comparisonConstraint1: ConstraintES =
              EventConstraintWithExecutionState(comparisonBc1, mergeExecutionState1)
            val comparisonPC1 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint1, true)
            val completeProgramConstraint1 = preamble ++ programConstraint1 :+ comparisonPC1
            val treePathAdded1 = reporter.addExploredPath(completeProgramConstraint1, false)
            var root = reporter.getRoot.get
            treeLogger.d(root)
            nodeMerger.mergeWorkListStates(root, treePathAdded1.init, mergeExecutionState1)
            root = reporter.getRoot.get
            treeLogger.d(root)

            toCheck.addToChecks(path1, "x", valueX.i)
            toCheck.addToChecks(path1, "y", valueY.i)
            toCheck.addToChecks(path1, "z", valueZ.i)
            toCheck.addToChecks(path1, unassigned, valueUnassigned.i)
            toCheck.doAllChecks(root)


            checkValueOfIdentifier(pathFollower, path1, valueX, input(120), root)
            checkValueOfIdentifier(pathFollower, path1, valueY, input(121), root)
            checkValueOfIdentifier(pathFollower, path1, valueZ, input(122), root)

            val (partialProgramConstraint2, store2, partialPath2) = randomlyTraverseTree(randomTree2, Nil, store1, Nil)
            if (paths2Traversed.contains(partialPath2)) {
              loopUntilEnoughPathsTraversed(pathsTraversed + 1, iterationsSinceLastTraversal + 1)
            } else {
              paths2Traversed += partialPath2
              val symStore2 = storeToSymStore(store2)
              println(s"Follows path 2 $partialPath2")

              val (valueX2, comparisonX2) = createComparisonExp(store2, "x", 1200)
              val (valueY2, comparisonY2) = createComparisonExp(store2, "y", 1210)

              val (valueA, comparisonA) = createComparisonExp(store2, "a", 97)
              val (valueB, comparisonB) = createComparisonExp(store2, "b", 98)
              val (valueC, comparisonC) = createComparisonExp(store2, "c", 99)
              val (valueD, comparisonD) = createComparisonExp(store2, "d", 100)
              val (valueE, comparisonE) = createComparisonExp(store2, "e", 101)

              val comparisonExp2 = LogicalBinaryExpression(
                comparisonX2,
                LogicalAnd,
                LogicalBinaryExpression(
                  comparisonY2,
                  LogicalAnd,
                  LogicalBinaryExpression(
                    comparisonZ,
                    LogicalAnd,
                    LogicalBinaryExpression(
                      comparisonA,
                      LogicalAnd,
                      LogicalBinaryExpression(
                        comparisonB,
                        LogicalAnd,
                        LogicalBinaryExpression(
                          comparisonC,
                          LogicalAnd,
                          LogicalBinaryExpression(
                            comparisonD,
                            LogicalAnd,
                            LogicalBinaryExpression(
                              comparisonE,
                              LogicalAnd,
                              comparisonUnassigned)))
                      )
                    )
                  )
                )
              )
              val comparisonBc2 = BranchConstraint(comparisonExp2, Nil)
              val comparisonConstraint2: ConstraintES =
                EventConstraintWithExecutionState(comparisonBc2, mergeExecutionState2)
              val comparisonPC2 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint2, true)

              val completeProgramConstraint2 = completeProgramConstraint1 ++ partialProgramConstraint2 :+ comparisonPC2
              treeLogger.d(root)
              val treePathAdded2 = reporter.addExploredPath(completeProgramConstraint2, true)
              root = reporter.getRoot.get
              treeLogger.d(root)
              nodeMerger.mergeWorkListStates(root, treePathAdded2.init, mergeExecutionState2)
              root = reporter.getRoot.get
              treeLogger.d(root)
              val path2 = (path1 :+ ThenDirection) ++ partialPath2

              toCheck.addToChecks(path2, "x", valueX2.i)
              toCheck.addToChecks(path2, "y", valueY2.i)
              toCheck.addToChecks(path2, "z", valueZ.i)
              toCheck.addToChecks(path2, unassigned, valueUnassigned.i)
              toCheck.addToChecks(path2, "a", valueA.i)
              toCheck.addToChecks(path2, "b", valueB.i)
              toCheck.addToChecks(path2, "c", valueC.i)
              toCheck.addToChecks(path2, "d", valueD.i)
              toCheck.addToChecks(path2, "e", valueE.i)
              toCheck.doAllChecks(root)

              checkValueOfIdentifier(pathFollower, path2, valueX2, input(1200), root)
              checkValueOfIdentifier(pathFollower, path2, valueY2, input(1210), root)
              checkValueOfIdentifier(pathFollower, path2, valueA, input(97), root)
              checkValueOfIdentifier(pathFollower, path2, valueB, input(98), root)
              checkValueOfIdentifier(pathFollower, path2, valueC, input(99), root)
              checkValueOfIdentifier(pathFollower, path2, valueD, input(100), root)
              checkValueOfIdentifier(pathFollower, path2, valueE, input(101), root)

              loopUntilEnoughPathsTraversed(pathsTraversed + 1, 0)
            }
          }
        } else {
          treeLogger.e(reporter.getRoot.get)
          NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
        }
      }
      loopUntilEnoughPathsTraversed(1, 0)
    }

    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed)
      })
  }

  protected val abcde = Set("a", "b", "c", "d", "e")
  test("Randomly generate 20 two-stage trees (medium + small trees) and traverse them") {
    runTwoStageTreeTest(3, 5, 4, 1, abcde)
  }

  test("Randomly generate 20 two-stage trees (medium + medium) and traverse them") {
    runTwoStageTreeTest(3, 5, 6, 4, abcde)
  }

  protected val abcdexy = Set("x", "y", "a", "b", "c", "d", "e")
  test("Randomly generate 20 two-stage trees with duplicate assignments (medium + small trees) and traverse them") {
    runTwoStageTreeTest(3, 5, 4, 1, abcdexy)
  }

  test("Randomly generate 20 two-stage trees with duplicate assignments (medium + medium) and traverse them") {
    runTwoStageTreeTest(3, 5, 6, 4, abcdexy)
  }

  protected def runThreeStageTreeTestWithDuplicateAssignments(
    tree1MaxInputId: Int,
    tree1MaxValue: Int,
    tree2MaxInputId: Int,
    tree2MaxValue: Int,
    tree3MaxInputId: Int,
    tree3MaxValue: Int
  ): Unit = {

    def doTestWithSeed(seed: Int): Unit = {
      scala.util.Random.setSeed(seed)

      var paths1Traversed = Set[Path]()
      var paths2Traversed = Set[Path]()
      var paths3Traversed = Set[Path]()
      val reporter = new ConstraintESReporter(MergingMode, None)
      val nodeMerger = new ConstraintESNodeMerger(treeLogger, reporter)
      val pathFollower = new ConstraintESPathFollower[Unit]
      val toCheck = new CheckRandomOldPaths[Int](pathFollower)

      val valueUnassigned = 42
      val unassigned = "unassigned"
      val identifiers1 = Set("x", "y", "z")

      val initialMap = Map(
        "x" -> (i(0, "x"), 0),
        "y" -> (i(0, "y"), 0),
        "z" -> (i(0, "z"), 0),

        "a" -> (i(0, "a"), 0),
        "b" -> (i(0, "b"), 0),
        "c" -> (i(0, "c"), 0),
        "d" -> (i(0, "d"), 0),
        "e" -> (i(0, "e"), 0),

        "n" -> (i(0, "n"), 0),
        "o" -> (i(0, "o"), 0),
        "p" -> (i(0, "p"), 0),
        unassigned -> (i(valueUnassigned, "unassigned"), 0)
      )

      val mergeExecutionState1 = CodeLocationExecutionState(SerialCodePosition("file", 1), FunctionStack.empty, None, 0)
      val randomTreeConstructor1 = new RandomTreeConstructor(new RandomExpAssignmentGenerator(identifiers1))

      val identifiers2 = Set("x", "y", "a", "b", "c", "d", "e")
      val mergeExecutionState2 = CodeLocationExecutionState(SerialCodePosition("file", 2), FunctionStack.empty, None, 0)
      val randomTreeConstructor2 = new RandomTreeConstructor(
        new RandomExpAssignmentGenerator(identifiers2), tree1MaxInputId + 1)

      val identifiers3 = Set("x", "y", "a", "b", "c", "n", "o", "p")
      val mergeExecutionState3 = CodeLocationExecutionState(SerialCodePosition("file", 3), FunctionStack.empty, None, 0)
      val randomTreeConstructor3 = new RandomTreeConstructor(
        new RandomExpAssignmentGenerator(identifiers3), tree2MaxInputId + 1)

      val randomTree1 = randomTreeConstructor1.constructRandomTree(tree1MaxInputId, tree1MaxValue)
      val randomTree2 = randomTreeConstructor2.constructRandomTree(tree2MaxInputId, tree2MaxValue)
      val randomTree3 = randomTreeConstructor3.constructRandomTree(tree3MaxInputId, tree3MaxValue)

      val maxUnsucessfulIterations = 30
      @tailrec
      def loopUntilEnoughPathsTraversed(pathsTraversed: Int, iterationsSinceLastTraversal: Int): Unit = {
        if (iterationsSinceLastTraversal > maxUnsucessfulIterations) {
          println(
            Console.RED + s"Stopping this tree traversal because of too many unsuccesful path traversals" + Console.RESET)
        } else if (pathsTraversed <= pathsToTraverse) {
          val (programConstraint1, store1, path1) = randomlyTraverseTree(randomTree1, Nil, initialMap, Nil)
          val symStore1 = storeToSymStore(store1)
          if (paths1Traversed.contains(path1)) {
            loopUntilEnoughPathsTraversed(pathsTraversed, iterationsSinceLastTraversal + 1)
          } else {
            printIteration(pathsTraversed)
            paths1Traversed += path1
            println(s"Follows path 1 $path1")
            val preamble = List(
              StoreUpdatePCElement[ConstraintES](
                EnterScopeUpdate(identifiers1 ++ identifiers2 + unassigned, 0)),
              StoreUpdatePCElement[ConstraintES](AssignmentsStoreUpdate(storeToSymStore(initialMap).map.toList))
            )

            val (valueX, comparisonX) = createComparisonExp(store1, "x", 120)
            val (valueY, comparisonY) = createComparisonExp(store1, "y", 121)
            val (valueZ, comparisonZ) = createComparisonExp(store1, "z", 122)
            val (_, comparisonUnassigned) = createComparisonExp(store1, unassigned, 200)

            val comparisonExp1 = LogicalBinaryExpression(
              comparisonX,
              LogicalAnd,
              LogicalBinaryExpression(
                comparisonY,
                LogicalAnd,
                LogicalBinaryExpression(comparisonZ, LogicalAnd, comparisonUnassigned)))
            val comparisonBc1 = BranchConstraint(comparisonExp1, Nil)
            val comparisonConstraint1: ConstraintES =
              EventConstraintWithExecutionState(comparisonBc1, mergeExecutionState1)
            val comparisonPC1 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint1, true)
            val completeProgramConstraint1 = preamble ++ programConstraint1 :+ comparisonPC1
            val treePathAdded1 = reporter.addExploredPath(completeProgramConstraint1, false)
            var root = reporter.getRoot.get
            treeLogger.d(root)
            nodeMerger.mergeWorkListStates(root, treePathAdded1.init, mergeExecutionState1)
            root = reporter.getRoot.get
            treeLogger.d(root)

            toCheck.addToChecks(path1, "x", valueX.i)
            toCheck.addToChecks(path1, "y", valueY.i)
            toCheck.addToChecks(path1, "z", valueZ.i)
            toCheck.addToChecks(path1, unassigned, valueUnassigned.i)
            toCheck.doAllChecks(root)


            checkValueOfIdentifier(pathFollower, path1, valueX, input(120), root)
            checkValueOfIdentifier(pathFollower, path1, valueY, input(121), root)
            checkValueOfIdentifier(pathFollower, path1, valueZ, input(122), root)

            val (partialProgramConstraint2, store2, partialPath2) = randomlyTraverseTree(randomTree2, Nil, store1, Nil)
            if (paths2Traversed.contains(partialPath2)) {
              loopUntilEnoughPathsTraversed(pathsTraversed + 1, iterationsSinceLastTraversal + 1)
            } else {
              paths2Traversed += partialPath2
              val symStore2 = storeToSymStore(store2)
              println(s"Follows path 2 $partialPath2")

              val (valueX2, comparisonX2) = createComparisonExp(store2, "x", 1200)
              val (valueY2, comparisonY2) = createComparisonExp(store2, "y", 1210)

              val (valueA, comparisonA) = createComparisonExp(store2, "a", 97)
              val (valueB, comparisonB) = createComparisonExp(store2, "b", 98)
              val (valueC, comparisonC) = createComparisonExp(store2, "c", 99)
              val (valueD, comparisonD) = createComparisonExp(store2, "d", 100)
              val (valueE, comparisonE) = createComparisonExp(store2, "e", 101)

              val comparisonExp2 = LogicalBinaryExpression(
                comparisonX2,
                LogicalAnd,
                LogicalBinaryExpression(
                  comparisonY2,
                  LogicalAnd,
                  LogicalBinaryExpression(
                    comparisonZ,
                    LogicalAnd,
                    LogicalBinaryExpression(
                      comparisonA,
                      LogicalAnd,
                      LogicalBinaryExpression(
                        comparisonB,
                        LogicalAnd,
                        LogicalBinaryExpression(
                          comparisonC,
                          LogicalAnd,
                          LogicalBinaryExpression(
                            comparisonD,
                            LogicalAnd,
                            LogicalBinaryExpression(
                              comparisonE,
                              LogicalAnd,
                              comparisonUnassigned)))
                      )
                    )
                  )
                )
              )
              val comparisonBc2 = BranchConstraint(comparisonExp2, Nil)
              val comparisonConstraint2: ConstraintES =
                EventConstraintWithExecutionState(comparisonBc2, mergeExecutionState2)
              val comparisonPC2 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint2, true)

              val completeProgramConstraint2 = completeProgramConstraint1 ++ partialProgramConstraint2 :+ comparisonPC2
              treeLogger.d(root)
              val treePathAdded2 = reporter.addExploredPath(completeProgramConstraint2, false)
              root = reporter.getRoot.get
              treeLogger.d(root)
              nodeMerger.mergeWorkListStates(root, treePathAdded2.init, mergeExecutionState2)
              root = reporter.getRoot.get
              treeLogger.d(root)
              val path2 = (path1 :+ ThenDirection) ++ partialPath2

              toCheck.addToChecks(path2, "x", valueX2.i)
              toCheck.addToChecks(path2, "y", valueY2.i)
              toCheck.addToChecks(path2, "z", valueZ.i)
              toCheck.addToChecks(path2, unassigned, valueUnassigned.i)
              toCheck.addToChecks(path2, "a", valueA.i)
              toCheck.addToChecks(path2, "b", valueB.i)
              toCheck.addToChecks(path2, "c", valueC.i)
              toCheck.addToChecks(path2, "d", valueD.i)
              toCheck.addToChecks(path2, "e", valueE.i)
              toCheck.doAllChecks(root)

              checkValueOfIdentifier(pathFollower, path2, valueX2, input(1200), root)
              checkValueOfIdentifier(pathFollower, path2, valueY2, input(1210), root)
              checkValueOfIdentifier(pathFollower, path2, valueA, input(97), root)
              checkValueOfIdentifier(pathFollower, path2, valueB, input(98), root)
              checkValueOfIdentifier(pathFollower, path2, valueC, input(99), root)
              checkValueOfIdentifier(pathFollower, path2, valueD, input(100), root)
              checkValueOfIdentifier(pathFollower, path2, valueE, input(101), root)

              val (partialProgramConstraint3, store3, partialPath3) = randomlyTraverseTree(
                randomTree3, Nil, store2, Nil)
              if (paths3Traversed.contains(partialPath3)) {
                loopUntilEnoughPathsTraversed(pathsTraversed + 1, iterationsSinceLastTraversal + 1)
              } else {
                paths3Traversed += partialPath3
                val symStore3 = storeToSymStore(store3)
                println(s"Follows path 3 $partialPath3")

                val (valueX3, comparisonX3) = createComparisonExp(store3, "x", 12000)
                val (valueY3, comparisonY3) = createComparisonExp(store3, "y", 12100)

                val (valueA2, comparisonA2) = createComparisonExp(store3, "a", 9700)
                val (valueB2, comparisonB2) = createComparisonExp(store3, "b", 9800)
                val (valueC2, comparisonC2) = createComparisonExp(store3, "c", 9900)

                val (valueN, comparisonN) = createComparisonExp(store3, "n", 110)
                val (valueO, comparisonO) = createComparisonExp(store3, "o", 111)
                val (valueP, comparisonP) = createComparisonExp(store3, "p", 112)

                val comparisonExp3 = LogicalBinaryExpression(
                  comparisonX3,
                  LogicalAnd,
                  LogicalBinaryExpression(
                    comparisonY3,
                    LogicalAnd,
                    LogicalBinaryExpression(
                      comparisonZ,
                      LogicalAnd,
                      LogicalBinaryExpression(
                        comparisonA2,
                        LogicalAnd,
                        LogicalBinaryExpression(
                          comparisonB2,
                          LogicalAnd,
                          LogicalBinaryExpression(
                            comparisonC2,
                            LogicalAnd,
                            LogicalBinaryExpression(
                              comparisonD,
                              LogicalAnd,
                              LogicalBinaryExpression(
                                comparisonE,
                                LogicalAnd,
                                LogicalBinaryExpression(
                                  comparisonN,
                                  LogicalAnd,
                                  LogicalBinaryExpression(
                                    comparisonO,
                                    LogicalAnd,
                                    LogicalBinaryExpression(
                                      comparisonP,
                                      LogicalAnd,
                                      comparisonUnassigned)))))))))))
                val comparisonBc3 = BranchConstraint(comparisonExp3, Nil)
                val comparisonConstraint3: ConstraintES =
                  EventConstraintWithExecutionState(comparisonBc3, mergeExecutionState3)
                val comparisonPC3 = ConstraintWithStoreUpdate[ConstraintES](comparisonConstraint3, true)

                val completeProgramConstraint3 = completeProgramConstraint2 ++ partialProgramConstraint3 :+ comparisonPC3
                treeLogger.d(root)
                val treePathAdded3 = reporter.addExploredPath(completeProgramConstraint3, true)
                root = reporter.getRoot.get
                treeLogger.d(root)
                nodeMerger.mergeWorkListStates(root, treePathAdded3.init, mergeExecutionState3)
                root = reporter.getRoot.get
                treeLogger.d(root)
                val path3 = (path2 :+ ThenDirection) ++ partialPath3

                toCheck.addToChecks(path3, "x", valueX3.i)
                toCheck.addToChecks(path3, "y", valueY3.i)
                toCheck.addToChecks(path3, "z", valueZ.i)
                toCheck.addToChecks(path3, unassigned, valueUnassigned.i)
                toCheck.addToChecks(path3, "a", valueA2.i)
                toCheck.addToChecks(path3, "b", valueB2.i)
                toCheck.addToChecks(path3, "c", valueC2.i)
                toCheck.addToChecks(path3, "d", valueD.i)
                toCheck.addToChecks(path3, "e", valueE.i)
                toCheck.addToChecks(path3, "n", valueN.i)
                toCheck.addToChecks(path3, "o", valueO.i)
                toCheck.addToChecks(path3, "p", valueP.i)
                toCheck.doAllChecks(root)

                checkValueOfIdentifier(pathFollower, path3, valueX3, input(12000), root)
                checkValueOfIdentifier(pathFollower, path3, valueY3, input(12100), root)
                checkValueOfIdentifier(pathFollower, path3, valueA2, input(9700), root)
                checkValueOfIdentifier(pathFollower, path3, valueB2, input(9800), root)
                checkValueOfIdentifier(pathFollower, path3, valueC2, input(9900), root)
                checkValueOfIdentifier(pathFollower, path3, valueD, input(100), root)
                checkValueOfIdentifier(pathFollower, path3, valueE, input(101), root)

                checkValueOfIdentifier(pathFollower, path3, valueN, input(110), root)
                checkValueOfIdentifier(pathFollower, path3, valueO, input(111), root)
                checkValueOfIdentifier(pathFollower, path3, valueP, input(112), root)

                loopUntilEnoughPathsTraversed(pathsTraversed + 1, 0)
              }
            }
          }
        } else {
          treeLogger.e(reporter.getRoot.get)
          NoDuplicateExecutionStatesChecker.check(reporter.getRoot.get, Set(dummyExecutionState))
        }
      }
      loopUntilEnoughPathsTraversed(1, 0)
    }

    1.to(nrOfTreesToCreate)
      .foreach(seed => {
        this.beforeEach()
        printSeed(seed)
        doTestWithSeed(seed)
      })
  }

  test("Randomly generate 20 three-stage trees with duplicate assignments (medium + small + small trees) and traverse them") {
    runThreeStageTreeTestWithDuplicateAssignments(3, 5, 4, 1, 5, 1)
  }

  test("Randomly generate 20 three-stage trees with duplicate assignments (medium + medium + medium) and traverse them") {
    runThreeStageTreeTestWithDuplicateAssignments(3, 5, 6, 4, 9, 4)
  }

}
