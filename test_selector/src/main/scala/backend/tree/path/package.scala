package backend.tree

import backend.Path
import backend.tree.constraints.Constraint
import backend.tree.follow_path._

package object path {

  import scala.language.implicitConversions

  implicit def boolToDirection(boolean: Boolean): BinaryDirection = {
    if (boolean) ThenDirection
    else ElseDirection
  }
  implicit def boolsToDirections(booleans: List[Boolean]): List[BinaryDirection] =
    booleans.map(boolToDirection)

  case class SymJSState[C <: Constraint](
    eventSequence: List[TargetTriggered],
    branchSequence: List[EdgeWithoutTo[C]]
  ) {
    def isEmpty: Boolean = eventSequenceEmpty && branchSequenceEmpty

    def eventSequenceEmpty: Boolean = eventSequence.isEmpty
    def branchSequenceEmpty: Boolean = branchSequence.isEmpty

    def dropBranch: SymJSState[C] = this.copy(branchSequence = this.branchSequence.tail)
    def dropEvent: SymJSState[C] = this.copy(eventSequence = this.eventSequence.tail)

    def headEvent: Option[TargetTriggered] = eventSequence.headOption
    def headBranch: Option[EdgeWithoutTo[C]] = branchSequence.headOption
  }

  object SymJSState {

    def init[C <: Constraint]: SymJSState[C] = new SymJSState[C](Nil, Nil)
    // After type erasure, this constructor method is a duplicate of the default SymJSState constructor method,
    // so use the trick suggested at https://stackoverflow.com/a/5736428 to prevent this compiler error.
    def apply[C <: Constraint](eventSequence: List[TargetTriggered], branchSequence: Path)
      (implicit d: DummyImplicit): SymJSState[C] = {
      new SymJSState[C](eventSequence, branchSequence.map(_.toEdgeWithoutTo))
    }
  }

}
