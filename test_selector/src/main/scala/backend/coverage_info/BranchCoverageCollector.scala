package backend.coverage_info

import backend.execution_state.CodePosition
import backend.tree.follow_path.Direction

trait BranchCoverageCollector {
  def wasBranchCovered(branchPosition: CodePosition, branchTaken: Direction): Boolean
}

case class BranchCoverageMap(map: Map[CodePosition, Set[Direction]]) extends BranchCoverageCollector {
  override def wasBranchCovered(
    branchPosition: CodePosition,
    branchTaken: Direction
  ): Boolean = {
    map.getOrElse(branchPosition, Set()).contains(branchTaken)
  }
}
