package backend.execution_state.store

import backend.execution_state.SymbolicStore

case class EnterScopeUpdate(identifiers: Set[String], scopeId: Int)
  extends ScopeUpdate {
  override def toString: String = s"EnterScope($identifiers)"
  override def replaceIdentifiersWith(newStore: SymbolicStore): StoreUpdate = this // Don't have to replace anything
  override def replaceAndApply(replaceWith: SymbolicStore): (EnterScopeUpdate, SymbolicStore) = (this, replaceWith)
//  override def applyToStore(store: SymbolicStore): SymbolicStore = store
}
