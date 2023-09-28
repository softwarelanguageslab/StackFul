package backend.tree.constraints

import backend.execution_state.{ExecutionState, HasExecutionState, SymbolicStore}
import backend.execution_state.store.StoreUpdate

sealed trait Constraint
trait BasicConstraint extends Constraint
trait ConstraintWithExecutionState extends Constraint with HasExecutionState {
  def executionState: ExecutionState
}
trait EventConstraint extends Constraint

trait HasStoreUpdates[T] {
  def storeUpdates(hasStoreUpdates: T): Iterable[StoreUpdate]
  def assignsIdentifiers(hasStoreUpdates: T): Set[String] = {
    storeUpdates(hasStoreUpdates).foldLeft[Set[String]](Set())((identifiers, storeUpdate) => {
      identifiers ++ storeUpdate.assignsIdentifiers
    })
  }
  def addStoreUpdatesAfter(hasStoreUpdates: T, newStoreUpdates: Iterable[StoreUpdate]): T = {
    val completeStoreUpdates = storeUpdates(hasStoreUpdates) ++ newStoreUpdates
    replaceStoreUpdates(hasStoreUpdates, completeStoreUpdates)
  }
  def addStoreUpdatesBefore(hasStoreUpdates: T, newStoreUpdates: Iterable[StoreUpdate]): T = {
    val completeStoreUpdates = newStoreUpdates ++ storeUpdates(hasStoreUpdates)
    replaceStoreUpdates(hasStoreUpdates, completeStoreUpdates)
  }
  def replaceIdentifiersWith(hasStoreUpdates: T, newStore: SymbolicStore): T = {
    val newStoreUpdates = storeUpdates(hasStoreUpdates).map(_.replaceIdentifiersWith(newStore))
    replaceStoreUpdates(hasStoreUpdates, newStoreUpdates)
  }
  def replaceAndApply(hasStoreUpdates: T, replaceWith: SymbolicStore): (T, SymbolicStore) = {
    type Acc = (List[StoreUpdate], SymbolicStore)
    val (newStoreUpdates, appliedToStore) = storeUpdates(hasStoreUpdates).foldLeft[Acc]((Nil, replaceWith))(
      (acc, storeUpdate) => {
        val (previousStoreUpdates, store) = acc
        val (newStoreUpdate, newStore) = storeUpdate.replaceAndApply(store)
        (previousStoreUpdates :+ newStoreUpdate, newStore)
      })
    val withStoreUpdatesReplaced = replaceStoreUpdates(hasStoreUpdates, newStoreUpdates)
    (withStoreUpdatesReplaced, appliedToStore)
  }
  def applyToStore(hasStoreUpdates: T, store: SymbolicStore): SymbolicStore = {
    storeUpdates(hasStoreUpdates).foldLeft(store)((store, constraint) => constraint.applyToStore(store))
  }
  def replaceStoreUpdates(hasStoreUpdates: T, newStoreUpdates: Iterable[StoreUpdate]): T
}

case object AllProcessesExplored extends Throwable