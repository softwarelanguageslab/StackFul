package backend.solvers

import backend.tree._
import backend.tree.constraints._
import backend.tree.follow_path.PathFollower
import backend.tree.path.SymJSState

class PathSolver[C <: Constraint : ConstraintNegater : ToBasic](
  root: SymbolicNode[C],
  selectedState: SymJSState[C],
  val pathFollower: PathFollower[C]
) extends Solver[C] {

  def solve(): SolverResult = {
    pathFollower.followPath(root, selectedState) match {
      case None => InvalidPath
      case Some(treePath) =>
        val optSolution = doOneSolveIteration(getUsableConstraints(treePath), Nil)
        optSolution match {
          case Some(solution) => NewInput(solution, Some(treePath))
          case None => UnsatisfiablePath
        }
    }
  }

  def toBasicConstraints: ToBasic[C] = implicitly[ToBasic[C]]
}
