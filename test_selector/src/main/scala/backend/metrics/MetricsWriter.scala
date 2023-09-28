package backend.metrics

import java.io._

trait MetricsWriter[T] {
  def write(metrics: Iterable[T]): Unit
}

class MergingMetricsWriter(path: String) extends MetricsWriter[MergingMetricsInformation] {

  private val file = new File(path)

  override def write(metrics: Iterable[MergingMetricsInformation]): Unit = {
    val writer = new BufferedWriter(new FileWriter(file))
//    file.delete()
    val header = ProvidesMetaInformationMergingMetrics.CSVHeader
    writer.write(header + "\n")
    if (metrics.nonEmpty) {
      metrics.foreach(metric => writer.append(metric.toCSVString + "\n"))
    }
    writer.flush()
  }
}
