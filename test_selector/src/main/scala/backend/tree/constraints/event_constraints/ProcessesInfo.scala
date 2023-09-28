package backend.tree.constraints.event_constraints

case class ProcessesInfo(processesInfo: List[Int], allowCreatingExtraTargets: Boolean) {
  def totalNrOfProcesses: Int = processesInfo.length
}
