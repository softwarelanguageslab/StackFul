package backend.metrics

trait KeepsMetrics[T] {
  def newIteration(): Unit
  def getAllMetrics: Iterable[T]
}

object MergingMetricsKeeper extends KeepsMetrics[MergingMetricsInformation] {
  private var metrics: List[MergingMetricsInformation] = Nil

  private var iteration: Int = 0

  private var currentIterationSolvingTime: Long = 0
  private var cumulativeIterationSolvingTime: Long = 0

  private var currentFindingPathTime: Long = 0
  private var cumulativeFindingPathTime: Long = 0

  private var currentIterationMergingOverhead: Long = 0
  private var cumulativeMergingOverhead: Long = 0

  def addSolvingTime(delta: Long): Unit = {
    currentIterationSolvingTime += delta
  }

  def addFindingPathTime(delta: Long): Unit = {
    currentFindingPathTime += delta
  }

  def addMergingOverhead(delta: Long): Unit = {
    currentIterationMergingOverhead += delta
  }

  protected def updateCumulativeMetrics(): Unit = {
    cumulativeIterationSolvingTime += currentIterationSolvingTime
    cumulativeFindingPathTime += currentFindingPathTime
    cumulativeMergingOverhead += currentIterationMergingOverhead
  }

  protected def reset(): Unit = {
    currentIterationSolvingTime = 0
    currentFindingPathTime = 0
    currentIterationMergingOverhead = 0
  }

  override def newIteration(): Unit = {
    updateCumulativeMetrics()
    if (iteration > 0) {
      val newInfo = MergingMetricsInformation(
        iteration,
        currentIterationSolvingTime,
        cumulativeIterationSolvingTime,
        currentFindingPathTime,
        cumulativeFindingPathTime,
        currentIterationMergingOverhead,
        cumulativeMergingOverhead)
      metrics :+= newInfo
    }
    iteration += 1
    reset()
  }

  override def getAllMetrics: Iterable[MergingMetricsInformation] = metrics
}
