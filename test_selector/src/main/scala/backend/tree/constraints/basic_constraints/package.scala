package backend.tree.constraints

import backend.communication.{BasicConstraintJSONParsing, JSONParsing}
import backend.expression._
import backend.json.BasicConstraintJsonReaderWriter
import backend.tree.{ConstantChecker, UsesIdentifier}
import spray.json.{JsonReader, JsonWriter}

package object basic_constraints {

  val branchConstraintNegater: ConstraintNegater[BranchConstraint] = (bc: BranchConstraint) =>
    BranchConstraint(bc.exp.negate, bc.storeUpdates)

  implicit val basicConstraintNegater: ConstraintNegater[BasicConstraint] = {
    case bc: BranchConstraint => branchConstraintNegater.negate(bc)
  }

  implicit object BasicConstraintOptimizer extends Optimizer[BasicConstraint] {
    override def optimize(constraint: BasicConstraint): BasicConstraint = constraint match {
      case BranchConstraint(exp, storeUpdates) =>
        val optimizedExp = SymbolicExpressionOptimizer.optimizeBoolExp(exp)
        BranchConstraint(optimizedExp, storeUpdates)
    }
  }
  object BasicConstraintNoIdentifierConstantChecker extends ConstantChecker[BasicConstraint] {

    /* TODO finish this */
    def isConstant(constraint: BasicConstraint): Boolean = constraint match {
      case BranchConstraint(exp, _) =>
        exp match {
          case e if isExpAtomicConstant(e) => true
          case RelationalExpression(left, _, right, None) =>
            (left, right) match {
              case (SymbolicInt(_, None), SymbolicInt(_, None)) => true
              case (SymbolicString("object", None), SymbolicString("object", None)) => true
              case _ => false
            }
          case _ => false
        }
      case _ => false
    }
    private def isExpAtomicConstant(exp: SymbolicExpression): Boolean = exp match {
      case bool: SymbolicBool if bool.identifier.isEmpty => true
      case int: SymbolicInt if int.identifier.isEmpty => true
      case _ => false
    }
  }

  implicit object BasicConstraintConstantChecker extends ConstantChecker[BasicConstraint] {
    /* TODO finish this */
    def isConstant(constraint: BasicConstraint): Boolean = BasicConstraintOptimizer.optimize(constraint) match {
      case BranchConstraint(exp, _) =>
        exp match {
          case e if isExpAtomicConstant(e) => true
          case RelationalExpression(left, _, right, _) =>
            (left, right) match {
              case (SymbolicInt(_, _), SymbolicInt(_, _)) => true
              case (SymbolicString("object", _), SymbolicString("object", _)) => true
              case _ => false
            }
          case _ => false
        }
      case _ => false
    }
  }

  implicit val basicConstraintCreator: ConstraintCreator[BasicConstraint] = (exp: BooleanExpression) => {
    BranchConstraint(exp, Nil)
  }

  implicit val basicConstraintHasJSONParsing: HasJSONParsing[BasicConstraint] = new HasJSONParsing[BasicConstraint] {
    override def getJSONParsing: JSONParsing[BasicConstraint] = BasicConstraintJSONParsing
  }

  implicit val basicConstraintsToBasicConstraints: ToBasic[BasicConstraint] = new ToBasic[BasicConstraint] {
    override def toBasicConstraints(constraints: List[BasicConstraint]): List[BasicConstraint] = constraints
    override def toOptBasic(bc: BasicConstraint): Option[BasicConstraint] = Some(bc)
  }

  implicit val basicConstraintToBooleanExpression: ToBooleanExpression[BasicConstraint] = {
    case BranchConstraint(exp, _) => Some(exp)
  }

  implicit val basicConstraintUsesIdentifier: UsesIdentifier[BasicConstraint] = {
    case bc: BranchConstraint => SymbolicExpressionUsesIdentifier.usesIdentifier(bc.exp)
    case _ => false
  }

  implicit object BasicConstraintAllInOne extends ConstraintAllInOne[BasicConstraint] {
    implicit val constraintNegater: ConstraintNegater[BasicConstraint] = basicConstraintNegater
    implicit val optimizer: Optimizer[BasicConstraint] = BasicConstraintOptimizer
    implicit val constantChecker: ConstantChecker[BasicConstraint] = BasicConstraintConstantChecker
    implicit val constraintCreator: ConstraintCreator[BasicConstraint] = basicConstraintCreator
    implicit val hasJSONParsing: HasJSONParsing[BasicConstraint] = basicConstraintHasJSONParsing
    implicit val toBasicConstraints: ToBasic[BasicConstraint] = basicConstraintsToBasicConstraints
    implicit val usesIdentifier: UsesIdentifier[BasicConstraint] = basicConstraintUsesIdentifier
    implicit val jsonReader: JsonReader[BasicConstraint] = new BasicConstraintJsonReaderWriter
    implicit val jsonWriter: JsonWriter[BasicConstraint] = new BasicConstraintJsonReaderWriter
  }

}
