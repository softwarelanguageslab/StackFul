package backend.modes

import backend.SMTSolveProcesses
import backend.modes._
import backend.solvers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterEach

abstract class SolvePathTest extends AnyFunSuite with BeforeAndAfterEach {

  implicit val mode: Mode

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  protected def newExploreInputExpected(
    exploreTree: SMTSolveProcesses,
    jsonInputString: String
  ): SolverResult = {
    newInputExpected(
      exploreTree,
      jsonInputString, {
        case InputAndEventSequence(_, _, _) => assert(true)
        case other =>
          assert(false, s"Expected a result of type InputAndEventSequence, got $other instead")
      }
    )
  }

  protected def newInputExpected(
    solveProcesses: SMTSolveProcesses,
    jsonInputString: String,
    partialFunction: PartialFunction[SolverResult, Unit]
  ): SolverResult = {
    val result = solveProcesses.solve(jsonInputString)
    partialFunction(result)
    result
  }

  protected def newSolveInputExpected(
    solvePath: SMTSolveProcesses,
    jsonInputString: String
  ): SolverResult = {
    newInputExpected(
      solvePath, jsonInputString, {
        case NewInput(_, _) => assert(true)
        case other => assert(false, s"Expected a result of type NewInput, got $other instead")
      })
  }

  protected def invalidPathExpected(
    solveProcess: SMTSolveProcesses,
    jsonInputString: String
  ): SolverResult = {
    val result = solveProcess.solve(jsonInputString)
    result match {
      case InvalidPath => result
      case _ =>
        assert(false, s"Expected a result of type InvalidPath, got $result instead")
        result
    }
  }
  protected def unsatisfiablePathExpected(
    solveProcess: SMTSolveProcesses,
    jsonInputString: String
  ): SolverResult = {
    val result = solveProcess.solve(jsonInputString)
    result match {
      case UnsatisfiablePath => result
      case _ =>
        assert(false, s"Expected a result of type InvalidPath, got $result instead")
        result
    }
  }

}
