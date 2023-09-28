import MergeMetrics from "@src/util/metrics/MergeMetrics";

export default interface IterationSpecificMetrics {
  executionTime: number;
  mergeMetrics: MergeMetrics;
  precomputedInputSize: number;
  eventSequenceSize: number;
}