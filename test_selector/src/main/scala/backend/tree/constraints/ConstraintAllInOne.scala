package backend.tree.constraints

import spray.json.{JsonReader, JsonWriter}
import backend.tree.{ConstantChecker, UsesIdentifier}
import backend.tree.constraints.basic_constraints.basicConstraintToBooleanExpression

trait ConstraintAllInOne[C <: Constraint] {
  implicit val constraintNegater: ConstraintNegater[C]
  implicit val optimizer: Optimizer[C]
  implicit val constantChecker: ConstantChecker[C]
  implicit val constraintCreator: ConstraintCreator[C]
  implicit val hasJSONParsing: HasJSONParsing[C]
  implicit val toBasicConstraints: ToBasic[C]
  implicit val toBooleanExpression: ToBooleanExpression[C] = (c: C) => {
    toBasicConstraints.toOptBasic(c).flatMap(basicConstraintToBooleanExpression.toBoolExp)
  }
  implicit val usesIdentifier: UsesIdentifier[C]
  implicit val jsonReader: JsonReader[C]
  implicit val jsonWriter: JsonWriter[C]
}
