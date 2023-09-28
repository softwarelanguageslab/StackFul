package backend.tree.constraints

trait ToBasic[C] {
  def toBasicConstraints(constraints: List[C]): List[BasicConstraint]
  def toOptBasic(c: C): Option[BasicConstraint]
}
