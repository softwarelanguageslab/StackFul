package backend.tree.constraints

import backend._

trait Optimizer[T] {
  def optimize(t: T): T
}

case class ReportOptimizer[C <: Constraint : ConstraintNegater : Optimizer]() extends Optimizer[PathConstraintWithMatchers[C]] {

  def optimize(report: PathConstraintWithMatchers[C]): PathConstraintWithMatchers[C] = {
    report.map({
      case PartialMatcherPCElement(b, constraintIsTrue, maybePartialMatcher) =>
        /* We only optimize actual BranchConstraints */
        val optimizedConstraint = implicitly[Optimizer[C]].optimize(b)
        PartialMatcherPCElement(optimizedConstraint, constraintIsTrue, maybePartialMatcher)
      case element => element
    })
  }
}
