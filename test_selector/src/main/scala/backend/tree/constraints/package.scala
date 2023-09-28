package backend.tree

import backend.execution_state.store.StoreUpdate
import backend.expression._
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, EventConstraintWithExecutionState}
import backend.tree.constraints.event_constraints.{StopTestingTargets, TargetChosen}

package object constraints {

  implicit object BasicConstraintHasStoreUpdates extends HasStoreUpdates[BasicConstraint] {
    def storeUpdates(bc: BasicConstraint): Iterable[StoreUpdate] = bc match {
      case bc: BranchConstraint => bc.storeUpdates
      case _ => Nil
    }
    def replaceStoreUpdates(
      bc: BasicConstraint,
      newStoreUpdates: Iterable[StoreUpdate]
    ): BasicConstraint = bc match {
      case bc: BranchConstraint => bc.copy(storeUpdates = newStoreUpdates)
      case _ => bc
    }
  }
  implicit object EventConstraintHasStoreUpdates extends HasStoreUpdates[EventConstraint] {
    def storeUpdates(ec: EventConstraint): Iterable[StoreUpdate] = ec match {
      case bc: BasicConstraint => implicitly[HasStoreUpdates[BasicConstraint]].storeUpdates(bc)
      case stt: StopTestingTargets => stt.storeUpdates
      case tc: TargetChosen => tc.storeUpdates
      case _ => Nil
    }
    def replaceStoreUpdates(
      ec: EventConstraint,
      newStoreUpdates: Iterable[StoreUpdate]
    ): EventConstraint = ec match {
      case bc: BranchConstraint => bc.copy(storeUpdates = newStoreUpdates)
      case stt: StopTestingTargets => stt.copy(storeUpdates = newStoreUpdates)
      case tc: TargetChosen => tc.copy(storeUpdates = newStoreUpdates)
    }
  }

  implicit object ConstraintESHasStoreUpdates extends HasStoreUpdates[ConstraintES] {
    def storeUpdates(ec: ConstraintES): Iterable[StoreUpdate] = ec match {
      case EventConstraintWithExecutionState(ec, _) => implicitly[HasStoreUpdates[EventConstraint]].storeUpdates(ec)
    }
    def replaceStoreUpdates(
      constraint: ConstraintES,
      newStoreUpdates: Iterable[StoreUpdate]
    ): ConstraintES = constraint match {
      case constraint@EventConstraintWithExecutionState(ec, _) =>
        constraint.copy(EventConstraintHasStoreUpdates.replaceStoreUpdates(ec, newStoreUpdates))
    }
  }

}
