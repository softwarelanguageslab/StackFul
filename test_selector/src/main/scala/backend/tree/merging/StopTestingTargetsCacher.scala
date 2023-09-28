package backend.tree.merging

import backend.tree.SymbolicNode
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.constraints.event_constraints.StopTestingTargets

object StopTestingTargetsCacher {

  private var cache: Map[Int, SymbolicNode[ConstraintES]] = Map()

  def add(constraint: StopTestingTargets, node: SymbolicNode[ConstraintES]): Unit = {
    cache += constraint.id -> node
  }

  def get(eventId: Int): Option[SymbolicNode[ConstraintES]] = {
    val result = cache.get(eventId)
    if (result.isDefined) {
      println("Was in cache")
    }
    result
  }

}
