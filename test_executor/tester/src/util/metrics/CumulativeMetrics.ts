import ErrorMetrics from "@src/util/metrics/ErrorMetrics";
import MergeMetrics from "@src/util/metrics/MergeMetrics";

export default interface CumulativeMetrics {
  nrOfLinesCovered: number;
  linesCoveredPerProcess: number;
  branchesPartiallyVisited: number;
  branchCoverage: number;
  maxPrecomputedInputSize: number;
  maxEventSequenceSize: number;
  uniqueEventHandlersExecuted: number;
  errors: ErrorMetrics;
  cumulativeTime: number;
  totalCumulativeTime: number; // Includes time spent by backend for selecting and solving for new path
  linesCovered: Set<string>;
  mergeMetrics: MergeMetrics;
}