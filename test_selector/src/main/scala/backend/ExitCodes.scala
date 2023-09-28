package backend

import backend.communication.{CommunicationException, JSONParsingException}
import backend.modes.SuccessfullyTerminatedException
import backend.solvers.SolveError

import scala.language.implicitConversions

trait HasExitCode[T] {
  def exitCode(e: T): Int
}

object ExitCodes {
  val noError: Int = 0
  val unknownError: Int = 1
  val parseError: Int = 2
  val solveError: Int = 3
  val communicationError: Int = 4

  implicit val convertCommunicationException: HasExitCode[CommunicationException.type] =
    new HasExitCode[CommunicationException.type] {
      override def exitCode(e: CommunicationException.type): Int = ExitCodes.communicationError
    }

  implicit val convertJSONParsingException: HasExitCode[JSONParsingException] =
    new HasExitCode[JSONParsingException] {
      override def exitCode(e: JSONParsingException): Int = ExitCodes.parseError
    }

  implicit val convertSolveError: HasExitCode[SolveError] = new HasExitCode[SolveError] {
    override def exitCode(e: SolveError): Int = ExitCodes.solveError
  }

  implicit val convertSuccessfulyTerminatedException
  : HasExitCode[SuccessfullyTerminatedException.type] =
    new HasExitCode[SuccessfullyTerminatedException.type] {
      override def exitCode(e: SuccessfullyTerminatedException.type): Int = ExitCodes.noError
    }

  implicit val convertError: HasExitCode[Error] = new HasExitCode[Error] {
    override def exitCode(e: Error): Int = ExitCodes.unknownError
  }

  implicit val convertException: HasExitCode[Exception] = new HasExitCode[Exception] {
    override def exitCode(e: Exception): Int = ExitCodes.unknownError
  }
}
