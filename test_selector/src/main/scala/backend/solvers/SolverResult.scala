package backend.solvers

import backend.Path
import backend.expression.SymbolicInput
import backend.tree.constraints.Constraint
import backend.tree.search_strategy.TreePath

sealed trait ComputedValue
case class ComputedBool(bool: Boolean) extends ComputedValue
case class ComputedFloat(float: Float) extends ComputedValue
case class ComputedInt(int: Int) extends ComputedValue
case class ComputedString(string: String) extends ComputedValue
case class ComputedEvent(eventId: Int, processId: Int, targetId: Int) extends ComputedValue

sealed trait SolverResult
case object SymbolicTreeFullyExplored extends SolverResult
case class FunctionFullyExplored(functionId: Int) extends SolverResult
case class NewInput[C <: Constraint](
  input: Map[SymbolicInput, ComputedValue],
  treePath: Option[TreePath[C]]
) extends SolverResult
case class InputAndEventSequence[C <: Constraint](
  newInput: NewInput[C],
  events: List[(Int, Int)],
  path: Path
) extends SolverResult
case object InvalidPath extends SolverResult
case object UnsatisfiablePath extends SolverResult
case object ActionFailed extends SolverResult
case class ActionNotApplied(reason: String) extends SolverResult
case object ActionSuccessful extends SolverResult
