package backend.modes

import backend.communication._
import backend.reporters.{EventConstraintPCReporter, IncludeSingleTCPCReporter, Reporter}
import backend.solvers._
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.event_constraints._
import backend.tree.search_strategy._
import backend.{PathConstraint, RegularPCElement}

import scala.collection.mutable.{Map => MMap}

object ExploreTreeWithPrefix {

  def apply(
    prefix: List[EventConstraint],
    prescribeEvents: Boolean,
    treeOutputPath: String,
    processId: Int,
    nrOfEnabledEvents: Int,
    sanitizer: Sanitizer,
    optimizer: Optimizer[RegularPCElement[EventConstraint]],
    constantChecker: ConstantChecker[RegularPCElement[EventConstraint]]
  ): ExploreTreeWithPrefix = {
    new ExploreTreeWithPrefix(
      prefix,
      prescribeEvents,
      treeOutputPath,
      processId,
      nrOfEnabledEvents,
      (root: SymbolicNode[EventConstraint]) => new BreadthFirstSearchCached[EventConstraint](root),
      makeReporter(prescribeEvents, processId, nrOfEnabledEvents),
      sanitizer,
      optimizer,
      constantChecker
    )
  }

  protected def makeReporter(
    prescribeEvents: Boolean,
    processId: Int,
    nrOfEnabledEvents: Int
  ): Reporter[PathConstraint[EventConstraint], EventConstraint] = {
    if (prescribeEvents) {
      new IncludeSingleTCPCReporter(processId, nrOfEnabledEvents, FunctionSummariesMode)
    } else {
      new EventConstraintPCReporter(FunctionSummariesMode)
    }
  }

}

class ExploreTreeWithPrefix(
  val prefix: List[EventConstraint],
  prescribeEvents: Boolean,
  treeOutputPath: String,
  processId: Int,
  nrOfEnabledEvents: Int,
  createSearchStrategy: SymbolicNode[EventConstraint] => SearchStrategyCached[EventConstraint],
  reporter: Reporter[PathConstraint[EventConstraint], EventConstraint],
  sanitizer: Sanitizer,
  optimizer: Optimizer[RegularPCElement[EventConstraint]],
  constantChecker: ConstantChecker[RegularPCElement[EventConstraint]]
)
  extends ExploreTree(
    prescribeEvents, treeOutputPath, createSearchStrategy, reporter, EventConstraintAllInOne, sanitizer, optimizer,
    constantChecker) {}

class FunctionSummaries(val treeOutputPath: String)
  extends SMTSolveProcess[EventConstraint] {

  type Input = FunctionSummaryInput

  override protected implicit val constraintHasStoreUpdates: HasStoreUpdates[EventConstraint] = implicitly[HasStoreUpdates[EventConstraint]]
  override val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne
  val functionSummariesJsonParsing: FunctionSummariesJsonParsing = new FunctionSummariesJsonParsing
  private val exploreTrees = MMap[Int, ExploreTreeWithPrefix]()
  private val solvePath = new SolvePath(makeSolvePathTreeOutputPath)
  def handleJSONInput(input: FunctionSummaryInput): SolverResult = {
    ???
//    input match {
//      case computePrefixInput: FSComputePrefix => handleComputePrefixInput(computePrefixInput)
//      case exploreFunctionInput: FSExploreFunction =>
//        handleExploreFunctionInput(exploreFunctionInput)
//      case checkSatisfiesInput: FSCheckSatisfiesFormula =>
//        handleCheckSatisfiesFormulaInput(checkSatisfiesInput)
//    }
  }
//  private def handleComputePrefixInput(input: FSComputePrefix): SolverResult = {
//    val syncedSymbolicParameters =
//      syncSymbolicParameters(input.values.map(_._2), input.correspondingInputs)
//    println(s"syncSymbolicParameters = $syncedSymbolicParameters")
//    val syncedSymbolicParametersConstraints: PathConstraint[EventConstraint] =
//      syncedSymbolicParameters.map(constraint => RegularPCElement(constraint, true)(allInOne.constraintNegater))
//    //    val prefixSolveModeInput = SolveModeInput(input.prefixPath ++ syncedSymbolicParametersConstraints, Nil, boolsToDirections((input.prefixPath ++ syncedSymbolicParametersConstraints).map(_._2)))
//    //    solvePath.handleJSONInput(prefixSolveModeInput)
//    val regularPCElementOptimizer: Optimizer[RegularPCElement[EventConstraint]] = RegularPCElementOptimizer[EventConstraint]()(
//      allInOne.constraintNegater, EventConstraintOptimizer)
//    val regularPCElementConstantChecker: ConstantChecker[RegularPCElement[EventConstraint]] = backend.RegularPCElementConstantChecker[EventConstraint]()(
//      allInOne.constraintNegater, EventConstraintConstantChecker)
//    val allConstraints: PathConstraint[EventConstraint] = input.prefixPath ++ syncedSymbolicParametersConstraints
//    val sanitizedConstraints = OnlyDistinctSanitizer.sanitize(allConstraints)(
//      regularPCElementOptimizer, regularPCElementConstantChecker)
//    println(s"handleCheckSatisfiesFormulaInput. syncSymbolicParameters = $syncedSymbolicParameters")
//    println(s"handleCheckSatisfiesFormulaInput. allConstraints = $sanitizedConstraints")
//
//    val regularPCElements: PathConstraint[BasicConstraint] = toBasicConstraints(sanitizedConstraints)
//    Z3.solveTuples(regularPCElements, Nil) match {
//      case Satisfiable(solution) => NewInput(solution, None)
//      case Unsatisfiable => UnsatisfiablePath
//      case _: SomeZ3Error => UnsatisfiablePath
//    }
//  }
//  private def toBasicConstraints(sanitizedConstraints: List[RegularPCElement[EventConstraint]]): PathConstraint[BasicConstraint] = {
//    val regularPCElements: PathConstraint[BasicConstraint] = sanitizedConstraints.foldLeft(
//      Nil: PathConstraint[BasicConstraint])((acc, constraint) => constraint match {
//      case RegularPCElement(bc: BranchConstraint, isTrue) => acc :+ RegularPCElement(bc: BasicConstraint, isTrue)
//      case RegularPCElement(fc: FixedConstraint, isTrue) => acc :+ RegularPCElement(fc: BasicConstraint, isTrue)
//      case _ => acc
//    })
//    regularPCElements
//  }
//  private def syncSymbolicParameters(
//    symbolicValues: List[SymbolicExpression],
//    correspondingInputs: List[SymbolicInput]
//  ): List[EventConstraint] = {
//    println(s"syncSymbolicParameters: correspondingInputs = $correspondingInputs")
//    val extraConstraints: List[EventConstraint] = symbolicValues
//      .zip(correspondingInputs)
//      .map((tuple) => {
//        FixedConstraint(RelationalExpression(tuple._1, tuple._2.equalsToOperator, tuple._2))
//      })
//    extraConstraints
//  }
//  private def handleExploreFunctionInput(input: FSExploreFunction): SolverResult = {
//    val syncedSymbolicParameters1: List[EventConstraint] = (input.suffixPath.flatMap(_.constraint match {
//      case fc: FixedConstraint => List(fc)
//      case _ => Nil
//    }) ++ input.prefixPath.map((element) => element.toConstraint)).filter({
//      case _: FixedConstraint => true
//      case _ => false
//    })
//    val syncedSymbolicParameters2: List[EventConstraint] =
//      syncSymbolicParameters(input.values.map(_._2), input.correspondingInputs)
//    println(s"syncedSymbolicParameters1 = $syncedSymbolicParameters1")
//    println(s"syncedSymbFSExploreFunctionolicParameters2 = $syncedSymbolicParameters2")
//    val syncedSymbolicParameters: PathConstraint[EventConstraint] =
//      (syncedSymbolicParameters1 ++ syncedSymbolicParameters2).map((constraint: EventConstraint) =>
//        RegularPCElement(constraint, true)(allInOne.constraintNegater))
//    println(s"syncSymbolicParameters = $syncedSymbolicParameters")
//    val suffixPath = syncedSymbolicParameters ++ input.suffixPath
//    val suffixExploreModeInput = ExploreModeInput[EventConstraint, RegularPCElement[EventConstraint]](
//      input.iteration, suffixPath, Nil, true)(allInOne.constraintNegater, allInOne.optimizer)
//    println(s"handleExploreFunctionInput, suffix = $suffixPath")
//    val exploreTree = getExploreTree(
//      input.prefixPath.map(element => element.toConstraint),
//      input.functionId,
//      input.processId,
//      input.nrOfEnabledEvents)
//    val eventSequenceTaken = input.prefixPath.foldLeft(Nil: List[TargetChosen])({
//      case (acc, RegularPCElement(tc: TargetChosen, _)) => acc :+ tc
//      case (acc, _) => acc
//    })
//    val suffixResult = exploreTree.handleJSONInput(suffixExploreModeInput)
//    val (addPath, result) = mapExploreTreeResult(
//      suffixResult,
//      input.functionId,
//      eventSequenceTaken.map((tc) => (tc.processIdChosen, tc.targetIdChosen)))
//    //    if (addPath) {
//    //      val eventSeq = eventSequenceTaken.map((tc) => RegularPCElement(tc, true))
//    //      println(s"Adding path to exploreTree of function ${input.functionId}, eventSeq = $eventSeq")
//    //      val toAdd = suffixPath ++ eventSeq
//    //      println(s"Adding path to exploreTree of function ${input.functionId}, path = $toAdd")
//    //      exploreTree.reporter.addExploredPath(toAdd)
//    //    }
//    result
//  }
//  private def getExploreTree(
//    prefix: List[EventConstraint],
//    functionId: Int,
//    processId: Int,
//    nrOfEnabledEvents: Int
//  ): ExploreTreeWithPrefix = {
//    val sanitizedPrefix = OnlyDistinctSanitizer.sanitize(prefix)
//    import allInOne._
//    val exploreTree = ExploreTreeWithPrefix(
//      sanitizedPrefix, true, makeExploreTreeOutputPath(functionId),
//      processId, nrOfEnabledEvents, OnlyDistinctSanitizer, RegularPCElementOptimizer[EventConstraint](),
//      RegularPCElementConstantChecker[EventConstraint]())
//    exploreTrees.getOrElseUpdate(
//      functionId,
//      exploreTree)
//  }
//  private def makeExploreTreeOutputPath(functionId: Int): String = {
//    val parts = treeOutputPath.split('.')
//    val prefix = parts.init.mkString("")
//    val extension = if (parts.length > 1) parts.last else ""
//    prefix + "_" + functionId + "." + extension
//  }
//  private def mapExploreTreeResult(
//    result: SolverResult,
//    functionId: Int,
//    prefixEvents: List[(Int, Int)]
//  ): (Boolean, SolverResult) = result match {
//    case SymbolicTreeFullyExplored => (false, FunctionFullyExplored(functionId))
//    case NewInput(inputs, optPath) if prefixEvents.nonEmpty =>
//      (false, InputAndEventSequence(NewInput(inputs, optPath), prefixEvents, Nil))
//    case InputAndEventSequence(inputs, proposedEvents, path) =>
//      println(s"Exploretree proposes events $proposedEvents")
//      (proposedEvents.nonEmpty, InputAndEventSequence(inputs, prefixEvents ++ proposedEvents, path))
//    case other => (false, other)
//  }
//  private def handleCheckSatisfiesFormulaInput(
//    input: FSCheckSatisfiesFormula
//  ): SolverResult = {
//    assert(input.values.length == input.correspondingInputs.length)
//    val (concreteValues: List[ConcreteValue], symbolicValues: List[SymbolicExpression]) =
//      input.values.unzip
//    val syncSymbolicParameters: List[FixedConstraint] = symbolicValues
//      .zip(input.correspondingInputs)
//      .map((tuple) => {
//        basic_constraints.FixedConstraint(
//          RelationalExpression(tuple._1, tuple._2.equalsToOperator, tuple._2))
//      })
//    val assignConcreteValues: List[FixedConstraint] = concreteValues
//      .zip(input.correspondingInputs)
//      .map({
//        case (concreteValue, symbolicInput) =>
//          basic_constraints.FixedConstraint(
//            RelationalExpression(
//              concreteValue,
//              symbolicInput.equalsToOperator,
//              symbolicInput))
//      })
//    println(s"handleCheckSatisfiesFormulaInput. input.preConditions = ${input.preConditions}")
//    println(s"handleCheckSatisfiesFormulaInput. input.pathConditions = ${input.pathConditions}")
//    val path: List[BranchConstraint] =
//      (input.preConditions ++ input.pathConditions).map((boolExp) => basic_constraints.BranchConstraint(boolExp, Nil))
//    val globalSymbolicInputs: List[FixedConstraint] = input.globalSymbolicInputs.map((tuple) =>
//      basic_constraints.FixedConstraint(
//        RelationalExpression(tuple._1, tuple._1.equalsToOperator, tuple._2)))
//    val allConstraints
//    : List[EventConstraint] = syncSymbolicParameters ++ assignConcreteValues ++ globalSymbolicInputs ++ path
//    val regularPCElementOptimizer: Optimizer[RegularPCElement[EventConstraint]] = RegularPCElementOptimizer[EventConstraint]()(
//      allInOne.constraintNegater, EventConstraintOptimizer)
//    val regularPCElementConstantChecker: ConstantChecker[RegularPCElement[EventConstraint]] = backend.RegularPCElementConstantChecker[EventConstraint]()(
//      allInOne.constraintNegater, EventConstraintConstantChecker)
//    //    val sanitizedConstraints: PathConstraint[EventConstraint] = Sanitizer.sanitize(allConstraints)(regularPCElementOptimizer, regularPCElementConstantChecker)
//    val sanitizedConstraints: List[EventConstraint] = OnlyDistinctSanitizer.sanitize(allConstraints)(
//      allInOne.optimizer, allInOne.constantChecker)
//    println(s"handleCheckSatisfiesFormulaInput. syncSymbolicParameters = $syncSymbolicParameters")
//    println(s"handleCheckSatisfiesFormulaInput. allConstraints = $sanitizedConstraints")
//
//    val basicConstraints: List[BasicConstraint] = allInOne.toBasicConstraints.toBasicConstraints(sanitizedConstraints)
//    Z3.solve(basicConstraints, Nil) match {
//      case Satisfiable(solution) => NewInput(solution, None)
//      case Unsatisfiable => UnsatisfiablePath
//      case error: SomeZ3Error => throw SolveError(error.ex)
//    }
//  }
  protected def parseInput(parsedJSON: ParsedJSON): FunctionSummaryInput = {
    functionSummariesJsonParsing.parse(parsedJSON)
  }

  private def makeSolvePathTreeOutputPath: String = {
    "prefix_" + treeOutputPath
  }
}
