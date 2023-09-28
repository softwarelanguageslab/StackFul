package backend.execution_state.store

import backend.execution_state.SymbolicStore

trait ScopeUpdate extends StoreUpdate {
  val scopeId: Int
  val identifiers: Set[String]
  override def assignsIdentifiers: Set[String] = Set()
  def applyToStore(store: SymbolicStore): SymbolicStore = store
}
