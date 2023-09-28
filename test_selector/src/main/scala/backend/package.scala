import backend.execution_state.store.StoreUpdate
import backend.expression.BooleanExpression
import backend.path_filtering.PartialRegexMatcher
import backend.tree._
import backend.tree.constraints.{BasicConstraint, Constraint, ConstraintNegater, Optimizer, ToBasic}
import backend.tree.follow_path._

package object backend {

  import scala.language.implicitConversions

  implicit def pathConstraintWithMatchersToPathConstraint[C <: Constraint : ConstraintNegater](
    pathConstraint: PathConstraintWithMatchers[C]
  ): PathConstraint[C] = {
    pathConstraint.map(element => RegularPCElement(element.constraint, element.isTrue))
  }

  type Path = List[Direction]
  type Formula = List[BooleanExpression]
  type PathConstraintWith[C, +Element <: PCElement[C]] = List[Element]
  /** A list of constraints that were visited, coupled with whether or not that constraint was true. */
  type PathConstraint[C <: Constraint] = PathConstraintWith[C, RegularPCElement[C]]
  type PathConstraintWithMatchers[C <: Constraint] = PathConstraintWith[C, PartialMatcherPCElement[C]]
  type PathConstraintWithStoreUpdates[C <: Constraint] = PathConstraintWith[C, PCElementWithStoreUpdate[C]]
  def maybeInvertConstraint[C <: Constraint](constraint: C, isTrue: Boolean)
    (implicit negater: ConstraintNegater[C]): C = {
    if (isTrue) {
      constraint
    } else {
      negater.negate(constraint)
    }
  }

  def pathConstraintToPath[C <: Constraint](pc: PathConstraint[C]): Path = {
    pc.map(pcElement => if (pcElement.isTrue) ThenDirection else ElseDirection)
  }

  implicit def pathToString(path: Path): String = {
    path
      .map({
        case ElseDirection => "e"
        case ThenDirection => "t"
      })
      .mkString("")
  }
  trait PCElement[C] {
    type Result
    implicit def toConstraint: Result
  }

  implicit def stringToPath(string: String): Path = SecretImplicit.stringToPath(string)
  trait PCElementWithStoreUpdate[C <: Constraint] extends PCElement[C] {
    type Result = Option[C]
  }
  case class InvalidPathException(attemptedPath: Path) extends Exception
  case class RegularPCElement[C <: Constraint : ConstraintNegater](
    constraint: C,
    isTrue: Boolean
  ) extends PCElement[C] {
    type Result = C
    override def toConstraint: Result = {
      maybeInvertConstraint(constraint, isTrue)
    }
  }
  case class ConstraintWithStoreUpdate[C <: Constraint : ConstraintNegater](
    constraint: C,
    isTrue: Boolean
  ) extends PCElementWithStoreUpdate[C] {
    override def toConstraint: Result = {
      Some(maybeInvertConstraint(constraint, isTrue))
    }
  }
  case class StoreUpdatePCElement[C <: Constraint](storeUpdate: StoreUpdate) extends AnyRef
    with PCElementWithStoreUpdate[C] {
    override implicit def toConstraint: Result = None
  }
  /**
    * Similar to old PathConstraint, but optionally includes the [[PartialRegexMatcher]] that was generated
    * during a run-time static analysis triggered by the evaluation of the constraint.
    */
  case class PartialMatcherPCElement[C <: Constraint : ConstraintNegater](
    constraint: C,
    isTrue: Boolean,
    optMatcher: Option[PartialRegexMatcher]
  )
    extends PCElement[C] {
    type Result = C
    override def toConstraint: Result = {
      maybeInvertConstraint(constraint, isTrue)
    }
  }
  case class RegularPCElementOptimizer[C <: Constraint : ConstraintNegater : Optimizer]()
    extends Optimizer[RegularPCElement[C]] {
    def optimize(t: RegularPCElement[C]): RegularPCElement[C] = {
      RegularPCElement(implicitly[Optimizer[C]].optimize(t.constraint), t.isTrue)
    }
  }
  case class RegularPCElementConstantChecker[C <: Constraint : ConstraintNegater : ConstantChecker]()
    extends ConstantChecker[RegularPCElement[C]] {
    def isConstant(t: RegularPCElement[C]): Boolean = {
      implicitly[ConstantChecker[C]].isConstant(t.constraint)
    }
  }
  case class RegularPCElementToBasic[C <: Constraint : ConstraintNegater : ToBasic]()
    extends ToBasic[RegularPCElement[C]] {
    val constraintToBasic: ToBasic[C] = implicitly[ToBasic[C]]
    def toBasicConstraints(constraints: List[RegularPCElement[C]]): List[BasicConstraint] = {
      val onlyConstraints: List[C] = constraints.map(_.constraint)
      constraintToBasic.toBasicConstraints(onlyConstraints)
    }
    def toOptBasic(element: RegularPCElement[C]): Option[BasicConstraint] = {
      constraintToBasic.toOptBasic(element.constraint)
    }
  }
  case class PCElementWithStoreUpdateOptimizer[C <: Constraint : ConstraintNegater](optimizer: Optimizer[C])
    extends Optimizer[PCElementWithStoreUpdate[C]] {
    def optimize(element: PCElementWithStoreUpdate[C]): PCElementWithStoreUpdate[C] = element match {
      case update: StoreUpdatePCElement[C] => update
      case constraint: ConstraintWithStoreUpdate[C] =>
        constraint.copy(constraint = optimizer.optimize(constraint.constraint))
    }
  }
  case class PCElementWithStoreUpdateConstantChecker[C <: Constraint : ConstraintNegater](constantChecker: ConstantChecker[C])
    extends ConstantChecker[PCElementWithStoreUpdate[C]] {
    def isConstant(element: PCElementWithStoreUpdate[C]): Boolean = element match {
      case _: StoreUpdatePCElement[C] => false
      case constraint: ConstraintWithStoreUpdate[C] =>
        constantChecker.isConstant(constraint.constraint)
    }
  }
  private object SecretImplicit {
    // Put the string-to-path conversion in one scope and define the implicit conversion function in another.
    // Otherwise, the compiler complains about the ambiguous .toList method called on string, as the implicit
    // conversion option is also available.
    def stringToPath(string: String): Path = {
      string.toList.map({
        case 'e' | 'E' => ElseDirection
        case 't' | 'T' => ThenDirection
      })
    }
  }

  implicit def regularPCElementOptimizer[C <: Constraint : ConstraintNegater : Optimizer]: RegularPCElementOptimizer[C] = RegularPCElementOptimizer[C]()
  implicit def regularPCElementConstantChecker[C <: Constraint : ConstraintNegater : ConstantChecker]: RegularPCElementConstantChecker[C] = RegularPCElementConstantChecker[C]()
}
