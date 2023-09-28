package backend.modes

import backend.communication._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.reporters.EventConstraintPCReporter
import backend.solvers.Z3Solver._
import backend.solvers._
import backend.tree.{ConstantChecker, OnlyDistinctSanitizer, Sanitizer, SymbolicNode}
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.event_constraints.{eventConstraintCreator => _, _}
import backend.tree.search_strategy.{BreadthFirstSearchCached, SearchStrategyCached}
import backend.{PathConstraint, PathConstraintWith, RegularPCElement, RegularPCElementConstantChecker, RegularPCElementOptimizer}

class VerifyIntra(
  val treeOutputPath: String,
  val exploreTree: ExploreTree[EventConstraint, RegularPCElement[EventConstraint]]
)
  extends SMTSolveProcess[EventConstraint] {

  override type Input = VerifyIntraInput

  override protected implicit val constraintHasStoreUpdates: HasStoreUpdates[EventConstraint] = implicitly[HasStoreUpdates[EventConstraint]]
  override val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne

  import allInOne._
  val verifyIntraJsonParsing: VerifyIntraJsonParsing = new VerifyIntraJsonParsing
  // Remove syncing constraints
  def filterPath(path: PathConstraint[EventConstraint]): PathConstraint[EventConstraint] = {
    path.filter((element: RegularPCElement[EventConstraint]) => !isSyncConstraint(element.constraint))
  }
  override def handleJSONInput(input: Input): SolverResult = input match {
    case exploreModeInput: VIExploreModeInput[EventConstraint@unchecked] => explore(exploreModeInput)
    case input: VIConnectPaths[EventConstraint@unchecked] => connectPaths(input)
  }
  override protected def parseInput(parsedJSON: ParsedJSON): Input = {
    verifyIntraJsonParsing.parse(parsedJSON)
  }

  private def isSyncConstraint(constraint: Constraint): Boolean = constraint match {
    case BranchConstraint(RelationalExpression(_, op, input: SymbolicMessageInput, _), _) =>
      op == input.equalsToOperator
    case _ => false
  }

  private def syncCorrespondingMessageInputs(
    pc1: PathConstraint[EventConstraint],
    pc2: PathConstraint[EventConstraint]
  ): List[SyncedExpressions] = {
    type SyncMap = Map[SymbolicMessageInput, (SymbolicExpression, SymbolicExpression)]
    val firstMessageInputsAdded: Map[SymbolicMessageInput, SymbolicExpression] =
      pc1.foldLeft(Map[SymbolicMessageInput, SymbolicExpression]())((map, element) =>
        element.constraint match {
          case BranchConstraint(RelationalExpression(left, op, input: SymbolicMessageInput, _), _)
            if isSyncConstraint(element.constraint) =>
            map + (input -> left)
          case _ => map
        })
    def addToMap(map: SyncMap, left: SymbolicExpression, input: SymbolicMessageInput): SyncMap = {
      val optOtherExp = firstMessageInputsAdded.get(input)
      optOtherExp match {
        case Some(otherExp) => map + (input -> (otherExp, left))
        case None => map
      }
    }

    val secondInputsAdded = pc2.foldLeft(Map(): SyncMap)((map, element) =>
      element.constraint match {
        case BranchConstraint(RelationalExpression(left, op, input: SymbolicMessageInput, _), _)
          if op == input.equalsToOperator =>
          addToMap(map, left, input)
        case _ => map
      })
    secondInputsAdded.toList.map((tuple) => SyncedExpressions(tuple._2._1, tuple._2._2, tuple._1))
  }

  private def explore(exploreModeInput: VIExploreModeInput[EventConstraint]): SolverResult = {
    val i: ExploreModeInput[EventConstraint, RegularPCElement[EventConstraint]] = exploreModeInput.exploreModeInput
    val filteredPC: PathConstraintWith[EventConstraint, RegularPCElement[EventConstraint]] = filterPath(
      i.pathConstraint)
    exploreTree.handleJSONInput(i.copy(pathConstraint = filteredPC))
  }

  private def eventConstraintsToBasicConstraints(constraints: List[EventConstraint]): List[BasicConstraint] = {
    constraints.foldLeft(Nil: List[BasicConstraint])((acc, constraint) => constraint match {
      case bc: BasicConstraint => acc :+ bc
      case _ => acc
    })
  }

  private def connectPaths(input: VIConnectPaths[EventConstraint]): SolverResult = {
    println(s"connectPaths. input.pc1 = ${input.pc1}")
    println(s"connectPaths. input.pc2 = ${input.pc2}")

    val syncedList: List[SyncedExpressions] = syncCorrespondingMessageInputs(input.pc1, input.pc2)
    val connection: PathConstraint[EventConstraint] =
      syncedList.map(
        (synced: SyncedExpressions) => RegularPCElement[EventConstraint](synced.toBranchConstraint, isTrue = true))
    val filteredPC1: PathConstraint[EventConstraint] = filterPath(input.pc1)
    val filteredPC2: PathConstraint[EventConstraint] = filterPath(input.pc2)
    val entirePath: PathConstraint[EventConstraint] = filteredPC1 ++ connection ++ filteredPC2

    println(s"VerifyIntra.connectPaths, connection = $connection")
    println(s"VerifyIntra.connectPaths, entirePath = $entirePath")
    val constraints: List[EventConstraint] = entirePath.map({
      case element@RegularPCElement(_: BranchConstraint, _) => element.toConstraint
      case RegularPCElement(c, _) => c
    })
    val filteredConstraints = constraints.filter(c => c match {
      case _: AssignmentsStoreUpdate => false
      case _ => true
    })
    val basicConstraints = eventConstraintsToBasicConstraints(filteredConstraints)
    val result = Z3Solver.Z3.solve(basicConstraints, ???)
    result match {
      case Satisfiable(solution) => NewInput(solution, None)
      case Unsatisfiable => UnsatisfiablePath
      case _: SomeZ3Error => UnsatisfiablePath
    }
  }

  case class SyncedExpressions(
    exp1: SymbolicExpression,
    exp2: SymbolicExpression,
    messageInput: SymbolicMessageInput
  ) {
    def toBranchConstraint: EventConstraint = {
      val op = messageInput.equalsToOperator
      implicitly[ConstraintCreator[EventConstraint]].createBooleanConstraint(RelationalExpression(exp1, op, exp2))
    }
  }
}

object VerifyIntraFactory extends ExplorationModeFactory[EventConstraint, RegularPCElement[EventConstraint]] {

  import EventConstraintAllInOne._

  def makeSanitizer: Sanitizer = OnlyDistinctSanitizer
  def makeOptimizer: Optimizer[RegularPCElement[EventConstraint]] = RegularPCElementOptimizer[EventConstraint]()
  def makeConstantChecker: ConstantChecker[RegularPCElement[EventConstraint]] = RegularPCElementConstantChecker[EventConstraint]()
  def makeSearchStrategy(root: SymbolicNode[EventConstraint]): SearchStrategyCached[EventConstraint] = {
    new BreadthFirstSearchCached[EventConstraint](root)
  }
}

object VerifyIntra {
  def apply(treeOutputPath: String): VerifyIntra = {
    val exploreTree = ExploreTree[EventConstraint, RegularPCElement[EventConstraint]](
      prescribeEvents = true, treeOutputPath, EventConstraintAllInOne, new EventConstraintPCReporter(VerifyIntraMode),
      VerifyIntraFactory)

    new VerifyIntra(treeOutputPath, exploreTree)
  }
}
