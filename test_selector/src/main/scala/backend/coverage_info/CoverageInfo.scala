package backend.coverage_info

import backend.execution_state.CodePosition

trait CoverageInfo

case class BranchLocation(pointsTo: CodePosition) extends CoverageInfo
case class EventHandlerLocation(targetId: Int) extends CoverageInfo
