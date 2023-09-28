package backend.tree.search_strategy

import backend.Path
import backend.coverage_info.BranchCoverageMap
import backend.execution_state.emptyStore
import backend.tree.SymbolicNode
import backend.tree.constraints.{Constraint, ConstraintNegater}
import backend.tree.follow_path.PathFollower
import backend.tree.path.SymJSState

case class FollowPathSolver[C <: Constraint : ConstraintNegater](root: SymbolicNode[C], toFollow: Path, pathFollower: PathFollower[C])
  extends SearchStrategyCached[C] {
  private def follow(root: SymbolicNode[C]): Option[TreePath[C]] = {
    val symJsState = SymJSState[C](Nil, toFollow)
    val optPathFollowed = pathFollower.followPathWithAssignments(root, emptyStore, symJsState, false)
    optPathFollowed.map(_.pathFollowed.path)
  }
  override def next(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]] = None
  override def findUnexploredNodes(optBranchCoverage: Option[BranchCoverageMap] = None, optEventHandlersCoverage: Option[List[String]] = None): Option[TreePath[C]] = {
    follow(root)
  }
}
