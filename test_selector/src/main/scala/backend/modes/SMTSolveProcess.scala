package backend.modes

import backend.communication._
import backend.solvers.SolverResult
import backend.tree.SymbolicTreeDotWriter
import backend.tree.constraints._

case object SuccessfullyTerminatedException extends Exception

trait SMTSolveProcess[C <: Constraint] {
  type Input
  val treeDotWriter: SymbolicTreeDotWriter[C] = new SymbolicTreeDotWriter[C]()(constraintHasStoreUpdates)
  val allInOne: ConstraintAllInOne[C]
  def handleJSONInput(input: Input): SolverResult
  @throws[SuccessfullyTerminatedException.type]
  final def solve(parsedJSON: ParsedJSON): SolverResult = {
    parsedJSON.parseOptMetaInput match {
      case Some(TerminationRequest) => throw SuccessfullyTerminatedException
      case None => handleJSONInput(parseInput(parsedJSON))
    }
  }
  protected def constraintHasStoreUpdates: HasStoreUpdates[C]
  protected def parseInput(parsedJSON: ParsedJSON): Input
}
