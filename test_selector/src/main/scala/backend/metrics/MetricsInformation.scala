package backend.metrics

trait MetricsInformation {
  def toCSVString: String
}

case class MergingMetricsInformation(
  iteration: Int,
  solvingTimeForIteration: Long,
  cumulativeSolvingTime: Long,
  findingPathTimeForIteration: Long,
  cumulativeFindingPathTime: Long,
  mergingOverheadForIteration: Long,
  cumulativeMergingOverhead: Long
) extends MetricsInformation {
  override def toCSVString: String = {
    s"$iteration;$solvingTimeForIteration;$cumulativeSolvingTime;" +
    s"$findingPathTimeForIteration;$cumulativeFindingPathTime;" +
    s"$mergingOverheadForIteration;$cumulativeMergingOverhead"
  }
}

trait ProvidesMetaInformationMetrics[T] {
  def CSVHeader: String
}
object ProvidesMetaInformationMergingMetrics extends ProvidesMetaInformationMetrics[MergingMetricsInformation] {
  def CSVHeader: String = {
    "iteration;solvingTimeForIteration;cumulativeSolvingTime;" +
    "findingPathTimeForIteration;cumulativeFindingPathTime;" +
    "mergingOverheadForIteration;cumulativeMergingOverhead"
  }
}
