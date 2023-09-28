package backend.tree.checks

import backend.execution_state.ExecutionState
import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.ConstraintES

object NoDuplicateExecutionStatesChecker {

  /**
    *
    * @param root
    * @param exempted Execution states which may be duplicated
    * @tparam T
    */
  def check[T](root: SymbolicNode[ConstraintES], exempted: Set[ExecutionState] = Set()): Unit = {
    var nodesEncountered: Set[SymbolicNode[ConstraintES]] = Set()
    var executionStateToNode: Map[ExecutionState, SymbolicNode[ConstraintES]] = Map()

    def checkAndAddToMap(nwc: SymbolicNodeWithConstraint[ConstraintES]): Unit = {
      if (! exempted.contains(nwc.constraint.executionState)) {
        executionStateToNode.get(nwc.constraint.executionState) match {
          case None => executionStateToNode += nwc.constraint.executionState -> nwc
          case Some(other) => assert(false, s"Found two nodes with the same execution state: $other and $nwc")
        }
      }
    }

    def loop(node: SymbolicNode[ConstraintES]): Unit = node match {
      case _ if (nodesEncountered.contains(node)) =>
      case bsn: BranchSymbolicNode[ConstraintES] =>
        checkAndAddToMap(bsn)
        nodesEncountered += bsn
        loop(bsn.elseBranch.to)
        loop(bsn.thenBranch.to)
      case _ =>
    }

    loop(root)
  }

}
