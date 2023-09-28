package backend.tree.constraints

import backend.communication.{ConstraintESJSONParsing, JSONParsing}
import backend.execution_state._
import backend.execution_state.store._
import backend.expression._
import backend.json.ConstraintESJsonReaderWriter
import backend.tree.{ConstantChecker, UsesIdentifier}
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.event_constraints._
import spray.json.{JsonReader, JsonWriter}

package object constraint_with_execution_state {

  type ConstraintES = ConstraintWithExecutionState
  type BasicConstraintES = EventConstraintWithExecutionState

  import scala.language.implicitConversions

  implicit def toConstraintES(ec: EventConstraint): BasicConstraintES =
    EventConstraintWithExecutionState(ec, CodeLocationExecutionState.dummyExecutionState)

  implicit object ConstraintESNegater extends ConstraintNegater[ConstraintES] {
    override def negate(c: ConstraintES): ConstraintES = c match {
      case EventConstraintWithExecutionState(ec, es) =>
        val negatedEc = EventConstraintNegater.negate(ec)
        EventConstraintWithExecutionState(ec = negatedEc, es)
    }
  }
  class ConstraintESCreator(state: ExecutionState) extends ConstraintCreator[ConstraintES] {
    override def createBooleanConstraint(boolExp: BooleanExpression): ConstraintES = {
      val ec = eventConstraintCreator.createBooleanConstraint(boolExp)
      EventConstraintWithExecutionState(ec, state)
    }
  }

  implicit object ConstraintESOptimizer extends Optimizer[ConstraintES] {
    override def optimize(constraint: ConstraintES): ConstraintES = constraint match {
      case EventConstraintWithExecutionState(ec, es) =>
        val optimizedEc = EventConstraintNoIdentifierOptimizer.optimize(ec)
        EventConstraintWithExecutionState(optimizedEc, es)
      case ac: EnterScopeUpdate => ac
      case ec: ExitScopeUpdate => ec
      case ac: AssignmentsStoreUpdate => ac
    }
  }

  implicit object ConstraintESConstantChecker extends ConstantChecker[ConstraintES] {
    override def isConstant(constraint: ConstraintES): Boolean = constraint match {
      case c: EventConstraintWithExecutionState => EventConstraintNoIdentifierConstantChecker.isConstant(c.ec)
      case _: EnterScopeUpdate => false
      case _: ExitScopeUpdate => false
      case _: AssignmentsStoreUpdate => false
    }
  }
  object EventConstraintNoIdentifierOptimizer extends Optimizer[EventConstraint] {
    override def optimize(constraint: EventConstraint): EventConstraint = constraint match {
      case BranchConstraint(exp, storeUpdates) =>
        val optimizedExp = SymbolicExpressionNoIdentifierOptimizer.optimizeBoolExp(exp)
        BranchConstraint(optimizedExp, storeUpdates)
      case other => other
    }
  }

  implicit val constraintESCreator: ConstraintCreator[ConstraintES] =
    (exp: BooleanExpression) => {
      EventConstraintWithExecutionState(
        eventConstraintCreator.createBooleanConstraint(exp),
        CodeLocationExecutionState.dummyExecutionState)
    }

  implicit val constraintESHasJSONParsing: HasJSONParsing[ConstraintES] = new HasJSONParsing[ConstraintES] {
    override def getJSONParsing: JSONParsing[ConstraintES] = new ConstraintESJSONParsing
  }

  implicit val constraintsESToBasicConstraints: ToBasic[ConstraintES] = new ToBasic[ConstraintES] {
    override def toBasicConstraints(constraints: List[ConstraintES]): List[BasicConstraint] = {
      eventConstraintsToBasicConstraints.toBasicConstraints(constraints.map({
        case ec: EventConstraintWithExecutionState => ec.ec
        case ac: AssignmentsStoreUpdate => ac
        case ec: EnterScopeUpdate => ec
        case ec: ExitScopeUpdate => ec
      }))
    }
    override def toOptBasic(constraint: ConstraintES): Option[BasicConstraint] = constraint match {
      case EventConstraintWithExecutionState(ec, _) => eventConstraintsToBasicConstraints.toOptBasic(ec)
    }
  }

  implicit def constraintESToEventConstraint(constraint: ConstraintES): EventConstraint = constraint match {
    case c: EventConstraintWithExecutionState => c.ec
  }

  implicit val constraintESUsesIdentifier: UsesIdentifier[ConstraintES] = {
    case bc: BranchConstraint => SymbolicExpressionUsesIdentifier.usesIdentifier(bc.exp)
    case EventConstraintWithExecutionState(bc: BranchConstraint, _) => SymbolicExpressionUsesIdentifier.usesIdentifier(
      bc.exp)
    case _ => false
  }

  implicit object ConstraintESAllInOne extends ConstraintAllInOne[ConstraintES] {
    implicit val constraintNegater: ConstraintNegater[ConstraintES] = ConstraintESNegater
    implicit val optimizer: Optimizer[ConstraintES] = ConstraintESOptimizer
    implicit val constantChecker: ConstantChecker[ConstraintES] = ConstraintESConstantChecker
    implicit val constraintCreator: ConstraintCreator[ConstraintES] = constraintESCreator
    implicit val hasJSONParsing: HasJSONParsing[ConstraintES] = constraintESHasJSONParsing
    implicit val toBasicConstraints: ToBasic[ConstraintES] = constraintsESToBasicConstraints
    implicit val usesIdentifier: UsesIdentifier[ConstraintES] = constraintESUsesIdentifier
    implicit val jsonReader: JsonReader[ConstraintES] = new ConstraintESJsonReaderWriter(
      eventConstraintHasJSONParsing.getJSONParsing)
    implicit val jsonWriter: JsonWriter[ConstraintES] = new ConstraintESJsonReaderWriter(
      eventConstraintHasJSONParsing.getJSONParsing)
    implicit override val toBooleanExpression: ToBooleanExpression[ConstraintES] = (c: ConstraintES) => {
      implicitly[ToBooleanExpression[EventConstraint]].toBoolExp(constraintESToEventConstraint(c))
    }
  }
}
