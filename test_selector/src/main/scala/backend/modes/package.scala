package backend

import backend.SMTSolveProcesses.SMTSolveProcessId
import backend.reporters.EventConstraintPCReporter
import backend.tree.constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints._

package object modes {

  def createSMTSolveProcess(
    mode: Mode,
    treeOutputPath: String,
    id: SMTSolveProcessId,
    config: StartupConfiguration
  ): SMTSolveProcess[_ <: Constraint] = mode match {
    case ExploreTreeMode => ExploreTreeMode.createSMTSolveProcess(treeOutputPath, id, config)
    case SolvePathsMode => SolvePathsMode.createSMTSolveProcess(treeOutputPath, id, config)
    case FunctionSummariesMode => FunctionSummariesMode.createSMTSolveProcess(treeOutputPath, id, config)
    case VerifyIntraMode => VerifyIntraMode.createSMTSolveProcess(treeOutputPath, id, config)
    case MergingMode => MergingMode.createSMTSolveProcess(treeOutputPath, id, config)
  }
  sealed trait Mode
  case object ExploreTreeMode extends Mode {
    def createSMTSolveProcess(
      treeOutputPath: String,
      id: SMTSolveProcessId,
      startupConfiguration: StartupConfiguration
    ): SMTSolveProcess[EventConstraint] = {
      ExploreTree[EventConstraint, RegularPCElement[EventConstraint]](
        prescribeEvents = true, treeOutputPath, EventConstraintAllInOne, new EventConstraintPCReporter(this),
        ExploreTreeFactory)
    }
  }
  case object FunctionSummariesMode extends Mode {
    def createSMTSolveProcess(
      treeOutputPath: String,
      id: SMTSolveProcessId,
      startupConfiguration: StartupConfiguration
    ): SMTSolveProcess[EventConstraint] = {
      new FunctionSummaries(treeOutputPath)
    }
  }
  case object SolvePathsMode extends Mode {
    def createSMTSolveProcess(
      treeOutputPath: String,
      id: SMTSolveProcessId,
      startupConfiguration: StartupConfiguration
    ): SMTSolveProcess[EventConstraint] = {
      new SolvePath(treeOutputPath)
    }
  }
  case object VerifyIntraMode extends Mode {
    def createSMTSolveProcess(
      treeOutputPath: String,
      id: SMTSolveProcessId,
      startupConfiguration: StartupConfiguration
    ): SMTSolveProcess[EventConstraint] = {
      VerifyIntra(treeOutputPath)
    }
  }
  case object MergingMode extends Mode {
    def createSMTSolveProcess(
      treeOutputPath: String,
      id: SMTSolveProcessId,
      startupConfiguration: StartupConfiguration
    ):
    SMTSolveProcess[ConstraintES] = {
      new ExploreTreeMerge(treeOutputPath, startupConfiguration)
    }
  }

}
