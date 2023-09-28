package backend.execution_state.store

import backend.execution_state.SymbolicStore

trait StoreUpdate {
  def assignsIdentifiers: Set[String]
  def replaceIdentifiersWith(newStore: SymbolicStore): StoreUpdate
  def replaceAndApply(replaceWith: SymbolicStore): (StoreUpdate, SymbolicStore)
  def applyToStore(store: SymbolicStore): SymbolicStore
}

