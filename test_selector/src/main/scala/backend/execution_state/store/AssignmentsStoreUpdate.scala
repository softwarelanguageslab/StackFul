package backend.execution_state.store

import backend.execution_state.SymbolicStore
import backend.expression._

case class AssignmentsStoreUpdate(assignments: Assignments)
  extends StoreUpdate {
  override def toString: String = s"Assignments($assignments)"

  def identifiers: List[String] = assignments.map(_._1)
  def contains(identifier: String): Boolean = identifiers.contains(identifier)

  override def assignsIdentifiers: Set[String] = identifiers.toSet

  override def replaceIdentifiersWith(newStore: SymbolicStore): AssignmentsStoreUpdate = {
    val newAssignments = assignments.foldLeft[Assignments](Nil)((acc, tuple) => {
      val (identifier, value) = tuple
      newStore.get(identifier) match {
        case Some(newValue) => acc :+ (identifier, newValue)
        case None => acc
      }
    })
    copy(newAssignments)
  }

//  protected def replaceIdentifiersAtTopLevel(replaceWith: SymbolicStore): AssignmentsStoreUpdate = {
//    // https://stackoverflow.com/a/46552687
////    val mapIntersection = assignsIdentifiers.intersect(replaceWith.map.keySet).map(key => (key, replaceWith(key))).toMap
//    val newAssignments = assignments.foldLeft(mapIntersection)((map, keyValue) => {
//      val newValue = ExpressionSubstituter.replaceAtTopLevel(map, keyValue._2)
//      map :+ (keyValue._1, newValue)
//    })
//    copy(assignments = newAssignments)
//  }

  override def replaceAndApply(replaceWith: SymbolicStore): (AssignmentsStoreUpdate, SymbolicStore) = {
//    val newStoreUpdate = replaceIdentifiersAtTopLevel(replaceWith)
    type Acc = (Assignments, SymbolicStore)
    val (newAssignments, newStore) = assignments.foldLeft[Acc]((Nil, replaceWith))((acc, assignmentTuple) => {
      val (listAcc, storeAcc) = acc
      val (identifier, assignedToExp) = assignmentTuple
      val newAssignedToExp = ExpressionSubstituter.replaceAtTopLevel(storeAcc, assignedToExp)
      val newStore = storeAcc.updated(identifier, newAssignedToExp)
      (listAcc :+ (identifier, newAssignedToExp), newStore)
    })
    (this.copy(assignments = newAssignments), newStore)
//    val newStore = newStoreUpdate.applyToStore(replaceWith)
//    (newStoreUpdate, newStore)
  }

  override def applyToStore(store: SymbolicStore): SymbolicStore = {
    assignments.foldLeft(store)((store, assignment) => {
      val identifier = assignment._1
      store.updated(identifier, assignment._2)
    })
  }
}
