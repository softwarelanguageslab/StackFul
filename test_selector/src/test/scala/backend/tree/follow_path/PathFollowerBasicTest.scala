package backend.tree.follow_path

import backend._
import backend.expression._
import backend.modes._
import backend.reporters.SolveModePCReporter
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints._
import backend.tree.constraints.event_constraints._
import backend.tree.path._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterEach

class PathFollowerBasicTest extends AnyFunSuite with BeforeAndAfterEach {

  val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne
  implicit val mode: Mode = SolvePathsMode
  implicit val startingProcess: Int = 0
  type ConstraintsPerTargetTriggered = (TargetTriggered, List[(SymbolicInput, Boolean)])
  implicit val negater: ConstraintNegater[EventConstraint] = allInOne.constraintNegater
  implicit val optimizer: Optimizer[EventConstraint] = allInOne.optimizer
  type PartialPathConstraint = (PathConstraint[EventConstraint], Seq[ConstraintsPerTargetTriggered], String)
  val seed: Long = 0
  val random = new scala.util.Random(seed)
  val pathFollower = new EventConstraintPathFollower
  val reporter = new SolveModePCReporter

  override def beforeEach(): Unit = {
    super.beforeEach()
    reporter.deleteSymbolicTree()
  }

  import scala.language.implicitConversions

  implicit private def boolToChar(bool: Boolean): Char = if (bool) 'T' else 'F'

  protected def makeTree(nrOfEvents: Int): Unit = {
    makeGenericTree(nrOfEvents, true)
  }
  protected def makeRandomTree(nrOfEvents: Int): Seq[Seq[ConstraintsPerTargetTriggered]] = {
    makeGenericTree(nrOfEvents, randomTrueOrFalse)
  }
  protected def makeGenericTree(
    nrOfEvents: Int,
    makeBool: => Boolean
  ): Seq[Seq[ConstraintsPerTargetTriggered]] = {
    // We need to cache inputs alongside a path (represented as a string), so we don't generate distinct inputs for each node
    var inputsCache: Map[String, SymbolicInput] = Map()
    val nrOfProcesses = 2
    val initialEventsAlreadyChosen: Map[Int, Set[Int]] =
      0.until(nrOfProcesses).map(idx => (idx, Set[Int]())).toMap
    val processesInfo: List[Int] = 1.to(nrOfProcesses).toList
    def makeInput(string: String): SymbolicInput = inputsCache.get(string) match {
      case Some(input) => input
      case None =>
        val newInput = IdGenerator.newSymbolicInputInt
        inputsCache += (string -> newInput)
        newInput
    }
    def makeSymExp(string: String): (RelationalExpression, SymbolicInput) = {
      val newInput = makeInput(string)
      (RelationalExpression(newInput, IntEqual, SymbolicInt(0)),
        newInput)
    }
    def makePCElement(string: String): (RegularPCElement[EventConstraint], SymbolicInput) = {
      val (constraint, input) = makeSymExp(string)
      (RegularPCElement(BranchConstraint(constraint, Nil): EventConstraint, makeBool), input)
    }
    def makePathConstraintForTarget(
      processId: Int,
      targetId: Int,
      string: String
    ): (PathConstraint[EventConstraint], Seq[(SymbolicInput, Boolean)], String) = {
      val nrOfBranches = processId + targetId + 1 // Make 1 constraint for target <0, 0>, 2 for <1, 0> and 3 for <1, 1>
      val tupled = 1
        .to(nrOfBranches)
        .foldLeft[(List[(RegularPCElement[EventConstraint], SymbolicInput)], String)]((Nil, string))({
          case ((tuples, string), _) =>
            val (pcElement, input) = makePCElement(string)
            (tuples :+ (pcElement, input), string + pcElement.isTrue)
        })
      val (pc, constraintsPerTT) = tupled._1
        .map({
          case (RegularPCElement(constraint, isTrue), input) =>
            (RegularPCElement(constraint, isTrue), (input, isTrue))
        })
        .unzip
      (pc, constraintsPerTT, tupled._2)
    }
    def makeTC(id: Int, int: Int): TargetChosen = {
      if (int % 3 == 0)
        event_constraints.TargetChosen(
          id, 0, 0, initialEventsAlreadyChosen, ProcessesInfo(processesInfo, true), startingProcess, Nil)
      else if (int % 3 == 1)
        event_constraints.TargetChosen(
          id, 1, 0, initialEventsAlreadyChosen, ProcessesInfo(processesInfo, true), startingProcess, Nil)
      else event_constraints.TargetChosen(
        id, 1, 1, initialEventsAlreadyChosen, ProcessesInfo(processesInfo, true), startingProcess, Nil)
    }
    val allStrings = 1.to(nrOfEvents).flatMap(possibleStrings(_, Seq('0', '1', '2')))
    allStrings.map(string => {
      val (pc, targetsInputBooleanTuples, _) =
        string.toCharArray.toList.zipWithIndex.toList.foldLeft[PartialPathConstraint]((Nil, Nil, ""))({
          case ((previousPCPart, targetsInputBooleanTuples, previousString), (char, index)) =>
            val tc = makeTC(index, char.asDigit)
            val eventAddedToString = previousString + char
            val tt = TargetTriggered(tc.processIdChosen, tc.targetIdChosen)
            val (branchesPC, inputBoolTuples, branchesAddedToString) =
              makePathConstraintForTarget(tc.processIdChosen, tc.targetIdChosen, eventAddedToString)
            (previousPCPart ++ (RegularPCElement(tc: EventConstraint, true) +: branchesPC),
              targetsInputBooleanTuples :+ (tt, inputBoolTuples.toList),
              branchesAddedToString)
        })
      reporter.addExploredPath(pc, true)
      targetsInputBooleanTuples
    })
  }
  // Adapted from: https://codereview.stackexchange.com/questions/41510/calculate-all-possible-combinations-of-given-characters
  private def possibleStrings(maxLength: Int, alphabet: Seq[Char]): Set[String] = {
    var curr: String = ""
    var allStrings: Set[String] = Set()
    def loop(): Unit = {
      if (curr.length == maxLength) {
        allStrings += curr
      } else {
        alphabet.foreach(char => {
          val oldCurr: String = curr
          curr = char + curr
          loop()
          curr = oldCurr
        })
      }
    }
    loop()
    allStrings
  }
  private def randomTrueOrFalse: Boolean = {
    random.nextBoolean()
  }

  protected def assertPathDefined(state: SymJSState[EventConstraint]): Unit = {
    val optPath = pathFollower.followPath(reporter.getRoot.get, state)
    assert(optPath.isDefined, s"State $state could not be found")
  }

}
