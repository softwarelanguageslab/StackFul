package backend.execution_state.store

import backend.execution_state.SymbolicStore
import backend.expression.{ExpressionSubstituter, SymbolicExpression}

case class TemporaryAssignmentStoreUpdate(identifier: String, exp: SymbolicExpression)
  extends StoreUpdate {
  override def assignsIdentifiers: Set[String] = Set(identifier)
  override def replaceIdentifiersWith(newStore: SymbolicStore): TemporaryAssignmentStoreUpdate = {
    newStore.get(identifier) match {
      case Some(newValue) => copy(identifier, newValue)
      case None => this
    }
  }
  override def replaceAndApply(replaceWith: SymbolicStore): (TemporaryAssignmentStoreUpdate, SymbolicStore) = {
    val newExp = ExpressionSubstituter.replaceAtTopLevel(replaceWith, exp)
    val newStoreUpdate = this.copy(exp = newExp)
    val newStore = newStoreUpdate.applyToStore(replaceWith)
    (newStoreUpdate, newStore)
  }
  override def applyToStore(store: SymbolicStore): SymbolicStore = {
    store.updated(identifier, exp)
  }
}
