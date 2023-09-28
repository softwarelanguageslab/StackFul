package backend

package object metrics {
  implicit val mergingMetricsKeeper: KeepsMetrics[MergingMetricsInformation] = MergingMetricsKeeper
  implicit val providesMetaInformationMergingMetrics: ProvidesMetaInformationMetrics[MergingMetricsInformation] = ProvidesMetaInformationMergingMetrics

}
