package backend.full

import backend._
import backend.expression._
import backend.modes._
import backend.reporters.EventConstraintPCReporter
import backend.solvers._
import backend.solvers.solve_events.SolverWithEvents
import backend.tree._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.{EventConstraint, _}
import backend.tree.constraints.event_constraints._
import backend.tree.search_strategy._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterEach

class SolvingTest extends AnyFunSuite with BeforeAndAfterEach {

  val startingProcess: Int = 0
  val reporter = new EventConstraintPCReporter(ExploreTreeMode)

  val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne

  private def createSearchStrategy(root: SymbolicNode[EventConstraint]): SearchStrategyCached[EventConstraint] = {
    new BreadthFirstSearchCached[EventConstraint](root)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reporter.deleteSymbolicTree()
  }

  def verifyTreeStructure(
    node: SymbolicNode[EventConstraint],
    parentNode: Option[SymbolicNode[EventConstraint]],
    thenBranchTaken: Boolean
  ): Unit = node match {
    case BranchSymbolicNode(tc: TargetChosen, thenBranch, elseBranch) =>
      if (tc.lastPossibleTargetForThisId(SolvePathsMode)) {
        assert(elseBranch.to == UnsatisfiableNode())
      }
      thenBranch.to match {
        case BranchSymbolicNode(stt: StopTestingTargets, _, _) => assert(stt.id == tc.id)
        case _ =>
          assert(
            false,
            "Then-branch of a TargetChosen-constraint should alwats be a StopTestingTargets-constraint")
      }

      parentNode match {
        case Some(BranchSymbolicNode(stt: StopTestingTargets, _, _)) =>
          assert(!thenBranchTaken)
          assert(tc.id == stt.id + 1)
        case Some(BranchSymbolicNode(tc2: TargetChosen, _, _)) =>
          assert(!thenBranchTaken)
          assert(tc.id == tc2.id)
        case Some(_) =>
          assert(
            false,
            "Parent-node of a StopTestingTarget-constraint should be a TargetChosen-constraint")
        case None =>
      }
      verifyTreeStructure(thenBranch.to, Some(node), true)
      verifyTreeStructure(elseBranch.to, Some(node), false)
    case BranchSymbolicNode(stt: StopTestingTargets, thenBranch, elseBranch) =>
      parentNode match {
        case Some(BranchSymbolicNode(tc: TargetChosen, _, _)) =>
          assert(tc.id == stt.id)
          assert(thenBranchTaken)
        case Some(_) =>
          assert(
            false,
            "Parent-node of a StopTestingTarget-constraint should be a TargetChosen-constraint")
        case None => assert(false)
      }
      verifyTreeStructure(thenBranch.to, Some(node), true)
      verifyTreeStructure(elseBranch.to, Some(node), false)
    case BranchSymbolicNode(_, thenBranch, elseBranch) =>
      verifyTreeStructure(thenBranch.to, Some(node), true)
      verifyTreeStructure(elseBranch.to, Some(node), false)
    case _ =>
  }

  private def addToTreeAndSolve(
    pc: PathConstraint[EventConstraint],
    expectedEventSequence: List[(Int, Int)]
  ): Unit = {
    reporter.addExploredPath(pc, true)
    reporter.getRoot match {
      case Some(root) =>
        makeSolver(root, Nil).solve() match {
          case NewInput(_, _) => assert(false, "Should not produce a NewInput object")
          case InputAndEventSequence(_, eventSequence, _) =>
            assert(eventSequence.size == expectedEventSequence.size)
            eventSequence
              .zip(expectedEventSequence)
              .foreach(elem => {
                // processId equals expected processId
                assert(elem._1._1 == elem._2._1)
                // targetId equals expected targetId
                assert(elem._1._2 == elem._2._2)
              })
          case SymbolicTreeFullyExplored => assert(false, "New event sequence should be available")
          case _ =>
            val message = "Should not happen"
            assert(false, message)
            throw new Exception(message)
        }
      case None => assert(false, "Tree-root should not be empty")
    }
  }
  private def makeSolver(root: SymbolicNode[EventConstraint], processesInfo: List[Int]) =
    new SolverWithEvents[EventConstraint](
      root, createSearchStrategy(root), processesInfo, None, None)(
      allInOne.constraintNegater, allInOne.toBasicConstraints, allInOne.usesIdentifier, allInOne.toBooleanExpression)
  private def constructSymbolicTree(
    nrOfPaths: Int,
    makeTC: (Int, Int, Int) => TargetChosen,
    initialPC: PathConstraint[EventConstraint],
    treeFileName: Option[String] = None,
    makeSuffixPC: Map[SymbolicInput, ComputedValue] => PathConstraint[EventConstraint] = _ => Nil,
    verifyInputs: Map[SymbolicInput, ComputedValue] => Unit = _ => ()
  ): Unit = {
    lazy val writer = new SymbolicTreeDotWriter[EventConstraint]
    var pcsAdded = Set[PathConstraint[EventConstraint]]()
    1.to(nrOfPaths)
      .foldLeft((initialPC, Map[SymbolicInput, ComputedValue]()))((tuple, _) => {
        val (pc, prevInputs) = tuple
        val pcToAdd = pc ++ makeSuffixPC(prevInputs)
        assert(!pcsAdded.contains(pcToAdd))
        pcsAdded += pcToAdd
        reporter.addExploredPath(pcToAdd, true)
        treeFileName.foreach(writer.writeTree(reporter.getRoot.get, _))
        makeSolver(reporter.getRoot.get, Nil).solve() match {
          case NewInput(_, _) =>
            val message = "Should not produce a NewInput object"
            assert(false, message)
            throw new Exception(message)
          case InputAndEventSequence(NewInput(inputs, _), events, _) =>
            verifyInputs(inputs)
            (events.zipWithIndex.map({
              case ((processId, targetId), id) =>
                RegularPCElement(makeTC(id, processId, targetId): EventConstraint, true)
            }), inputs)
          case SymbolicTreeFullyExplored =>
            val message = "New event sequence should be available"
            assert(false, message)
            throw new Exception(message)
          case _ =>
            val message = "Should not happen"
            assert(false, message)
            throw new Exception(message)
        }
      })
  }

  test("Solve TargetChosen constraints") {

    val initialEventsAlreadyChosen: Map[Int, Set[Int]] = Map(0 -> Set(), 1 -> Set(), 2 -> Set())
    val processesInfo = List(0, 3, 20)

    val pc1: PathConstraint[EventConstraint] = List(
      RegularPCElement(
        TargetChosen(
          0, 1, 0, initialEventsAlreadyChosen, ProcessesInfo(processesInfo, false), startingProcess,
          Nil): EventConstraint,
        true))
    addToTreeAndSolve(pc1, List((1, 1)))

    val pc2: PathConstraint[EventConstraint] = List(
      RegularPCElement(
        TargetChosen(
          0, 1, 1, initialEventsAlreadyChosen, ProcessesInfo(processesInfo, false), startingProcess,
          Nil): EventConstraint,
        true))
    addToTreeAndSolve(pc2, List((1, 0), (1, 0)))
  }

  test("Test TargetChosen constraints with two targets") {
    val initialEventsAlreadyChosen: Map[Int, Set[Int]] = Map(0 -> Set(), 1 -> Set())
    val processesInfo = List(1, 2)
    def makeTC(id: Int, processId: Int, targetId: Int): TargetChosen =
      TargetChosen(
        id,
        processId,
        targetId,
        initialEventsAlreadyChosen,
        ProcessesInfo(processesInfo, true),
        startingProcess,
        Nil)
    val initialPC: PathConstraint[EventConstraint] =
      List(
        RegularPCElement(makeTC(0, 0, 0): EventConstraint, true),
        RegularPCElement(makeTC(1, 0, 0): EventConstraint, true))
    constructSymbolicTree(100, makeTC, initialPC, Some(TestConfigs.symbolicTreePath))

    verifyTreeStructure(reporter.getRoot.get, None, false)
  }

  test("Test TargetChosen constraints with regular constraints included") {
    val initialEventsAlreadyChosen: Map[Int, Set[Int]] = Map(0 -> Set(), 1 -> Set())
    val processesInfo = List(1, 2)
    def makeTC(id: Int, processId: Int, targetId: Int): TargetChosen =
      TargetChosen(
        id,
        processId,
        targetId,
        initialEventsAlreadyChosen,
        ProcessesInfo(processesInfo, true),
        startingProcess,
        Nil)
    val initialPC: PathConstraint[EventConstraint] =
      List(
        RegularPCElement(makeTC(0, 0, 0): EventConstraint, true),
        RegularPCElement(makeTC(1, 0, 0): EventConstraint, true))
    val input = SymbolicInputInt(RegularId(0, 0))
    def makeSuffixPC(isTrue: Boolean): PathConstraint[EventConstraint] =
      List(
        RegularPCElement(
          BranchConstraint(
            RelationalExpression(
              input,
              IntEqual,
              SymbolicInt(10)), Nil): EventConstraint,
          isTrue))
    constructSymbolicTree(
      100,
      makeTC,
      initialPC,
      Some(TestConfigs.symbolicTreePath),
      inputs => {
        makeSuffixPC(inputs.get(input).exists({ case ComputedInt(10) => true; case _ => false }))
      },
      inputs => {
        assert(inputs.size == 1 || inputs.isEmpty)
        assert(inputs.get(input) match {
          case None | Some(ComputedInt(10)) | Some(ComputedInt(11)) => true
          case _ => false
        })
      }
    )
    verifyTreeStructure(reporter.getRoot.get, None, false)
  }
}
