package backend.tree.util

import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, EventConstraintWithExecutionState}
import backend.tree.constraints.event_constraints.StopTestingTargets
import backend.tree.{BranchSymbolicNode, MergedNode, RegularLeafNode, SymbolicNode, UnexploredNode, UnsatisfiableNode}

import scala.collection.mutable.{Map => MMap}

object StopTestingTargetsFinder {

  def findStopTestingTargets[T](root: SymbolicNode[ConstraintES]): Map[Int, Set[SymbolicNode[ConstraintES]]] = {
    val map = MMap[Int, Set[SymbolicNode[ConstraintES]]]()

    def addNodeToMap(id: Int, node: SymbolicNode[ConstraintES]): Unit = {
      map.update(id, map.getOrElse(id, Set[SymbolicNode[ConstraintES]]()) + node)
    }

    def loop(node: SymbolicNode[ConstraintES]): Unit = node match {
      case RegularLeafNode() | UnsatisfiableNode() | UnexploredNode() | MergedNode() =>
      case bsn@BranchSymbolicNode(EventConstraintWithExecutionState(ec, _), left, right) => ec match {
        case StopTestingTargets(id, _, _, _) => addNodeToMap(id, bsn)
        case _ =>
      }
        loop(left.to)
        loop(right.to)
    }
    loop(root)
    map.toMap
  }

}
