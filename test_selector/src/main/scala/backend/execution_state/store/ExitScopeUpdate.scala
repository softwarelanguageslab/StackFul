package backend.execution_state.store

import backend.execution_state
import backend.execution_state.SymbolicStore

case class ExitScopeUpdate(identifiers: Set[String], scopeId: Int)
  extends ScopeUpdate {
  override def toString: String = s"ExitScope($identifiers)"
  override def replaceIdentifiersWith(newStore: SymbolicStore): StoreUpdate = this // Don't have to replace anything
  override def replaceAndApply(replaceWith: SymbolicStore): (ExitScopeUpdate, SymbolicStore) = (this, replaceWith)
//  override def applyToStore(store: SymbolicStore): execution_state.SymbolicStore = {
//    new SymbolicStore(store.map -- identifiers)
//  }
}