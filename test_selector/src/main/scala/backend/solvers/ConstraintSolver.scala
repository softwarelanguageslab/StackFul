package backend.solvers

trait ConstraintSolver[C] {
  type SolverResult
  def solve(constraints: List[C], processesInfo: List[Int]): SolverResult
}
