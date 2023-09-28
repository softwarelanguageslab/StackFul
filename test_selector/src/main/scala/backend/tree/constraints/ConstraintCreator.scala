package backend.tree.constraints

import backend.expression.BooleanExpression

trait ConstraintCreator[C <: Constraint] {
  def createBooleanConstraint(boolExp: BooleanExpression): C
}
