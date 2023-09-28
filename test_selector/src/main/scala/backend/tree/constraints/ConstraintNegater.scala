package backend.tree.constraints

trait ConstraintNegater[C <: Constraint] {
  def negate(constraint: C): C
}
