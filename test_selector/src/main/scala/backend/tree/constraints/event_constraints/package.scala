package backend.tree.constraints

import backend.communication.{EventConstraintJSONParsing, JSONParsing}
import backend.expression._
import backend.json.EventConstraintJsonReaderWriter
import backend.tree.{ConstantChecker, UsesIdentifier}
import backend.tree.constraints.basic_constraints._
import spray.json.{JsonReader, JsonWriter}

package object event_constraints {

  implicit object EventConstraintNegater extends ConstraintNegater[EventConstraint] {
    override def negate(constraint: EventConstraint): EventConstraint = constraint match {
      case bc: BranchConstraint => basic_constraints.branchConstraintNegater.negate(bc)
      case sc: StopTestingTargets => negateStopTestingTargets(sc)
      case tc: TargetChosen => TargetChosen.negate(tc)
    }
  }

  private def negateStopTestingTargets(sc: StopTestingTargets): TargetChosen = {
    TargetChosen.makeDummyTargetChosen(sc.id + 1, sc.processesInfo, sc.startingProcess) match {
      case None => throw AllProcessesExplored
      case Some(newTC) => newTC
    }
  }

  implicit object EventConstraintOptimizer extends Optimizer[EventConstraint] {
    override def optimize(constraint: EventConstraint): EventConstraint = constraint match {
      case BranchConstraint(exp, storeUpdates) =>
        val optimizedExp = SymbolicExpressionOptimizer.optimizeBoolExp(exp)
        BranchConstraint(optimizedExp, storeUpdates)
      case other => other
    }
  }

  object EventConstraintNoIdentifierConstantChecker extends ConstantChecker[EventConstraint] {
    override def isConstant(constraint: EventConstraint): Boolean = constraint match {
      case bc: BasicConstraint => BasicConstraintNoIdentifierConstantChecker.isConstant(bc)
      case _ => false
    }
  }

  implicit object EventConstraintConstantChecker extends ConstantChecker[EventConstraint] {
    override def isConstant(constraint: EventConstraint): Boolean = constraint match {
      case bc: BasicConstraint => BasicConstraintConstantChecker.isConstant(bc)
      case _ => false
    }
  }

  implicit val eventConstraintCreator: ConstraintCreator[EventConstraint] = (exp: BooleanExpression) => {
    BranchConstraint(exp, Nil)
  }

  implicit val eventConstraintHasJSONParsing: HasJSONParsing[EventConstraint] = new HasJSONParsing[EventConstraint] {
    override def getJSONParsing: JSONParsing[EventConstraint] = new EventConstraintJSONParsing
  }

  implicit val eventConstraintsToBasicConstraints: ToBasic[EventConstraint] = new ToBasic[EventConstraint] {
    override def toBasicConstraints(constraints: List[EventConstraint]): List[BasicConstraint] = {
      constraints.foldLeft(Nil: List[BasicConstraint])((acc, constraint) => constraint match {
        case bc: BranchConstraint => acc :+ bc
        case tc: TargetChosen =>
          val optTargetChosenExp = implicitly[ToBooleanExpression[EventConstraint]].toBoolExp(tc)
          val optBranchConstraint = optTargetChosenExp.map(BranchConstraint(_, Nil))
          optBranchConstraint.foldLeft(acc)((acc, bc) => acc :+ bc)
        case _ => acc
      })
    }
    override def toOptBasic(constraint: EventConstraint): Option[BasicConstraint] = constraint match {
      case bc: BranchConstraint => Some(bc)
      case _ => None
    }
  }

  implicit val eventConstraintToBooleanExpression: ToBooleanExpression[EventConstraint] = {
    case tc: TargetChosen =>
      val input = SymbolicInputEvent(RegularId(tc.id, tc.processIdChosen))
      Some(SymbolicEventChosenExpression(input, EventChosenOperator, tc.targetIdChosen))
    case tc: TargetNotChosen =>
      val input = SymbolicInputEvent(RegularId(tc.id, tc.processIdChosen))
      Some(SymbolicEventChosenExpression(input, EventNotChosenOperator, tc.targetIdChosen))
    case ec => eventConstraintsToBasicConstraints.toOptBasic(ec).flatMap(basicConstraintToBooleanExpression.toBoolExp)
  }

  implicit val eventConstraintUsesIdentifier: UsesIdentifier[EventConstraint] = {
    case bc: BranchConstraint => basicConstraintUsesIdentifier.usesIdentifier(bc)
    case _ => false
  }

  implicit object EventConstraintAllInOne extends ConstraintAllInOne[EventConstraint] {
    implicit val constraintNegater: ConstraintNegater[EventConstraint] = EventConstraintNegater
    implicit val optimizer: Optimizer[EventConstraint] = EventConstraintOptimizer
    implicit val constantChecker: ConstantChecker[EventConstraint] = EventConstraintConstantChecker
    implicit val constraintCreator: ConstraintCreator[EventConstraint] = eventConstraintCreator
    implicit val hasJSONParsing: HasJSONParsing[EventConstraint] = eventConstraintHasJSONParsing
    implicit val toBasicConstraints: ToBasic[EventConstraint] = eventConstraintsToBasicConstraints
    implicit val usesIdentifier: UsesIdentifier[EventConstraint] = eventConstraintUsesIdentifier
    implicit val jsonReader: JsonReader[EventConstraint] = new EventConstraintJsonReaderWriter(
      eventConstraintHasJSONParsing.getJSONParsing)
    implicit val jsonWriter: JsonWriter[EventConstraint] = new EventConstraintJsonReaderWriter(
      eventConstraintHasJSONParsing.getJSONParsing)
  }
}
