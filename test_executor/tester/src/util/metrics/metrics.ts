import ConfigurationReader, {startTime} from "@src/config/user_argument_parsing/Config";
import IterationSpecificMetrics from "@src/util/metrics/IterationSpecificMetrics";
import CumulativeMetrics from "@src/util/metrics/CumulativeMetrics";
import ErrorMetrics from "@src/util/metrics/ErrorMetrics";
import Event from "@src/tester/Event";
import MergeMetrics from "@src/util/metrics/MergeMetrics";
import Process from "@src/process/processes/Process";
import BranchCoverageCollector from "@src/util/metrics/BranchCoverageCollector";
import ComputedInput from "@src/backend/ComputedInput";

export interface IterationMetric {
  iteration: number;
  individual: IterationSpecificMetrics;
  cumulative: CumulativeMetrics;
}

/**
 * Keeps metrics about a test iteration such as lines covered, nr of errors discovered, cumulative time
 */
export default class Metrics {
  public _metrics: IterationMetric[];
  private _cumulativeTime: number;
  /* Lines covered per type of process: e.g., two client processes testing the same website would belong to the same type. */
  private readonly _linesCovered: Set<string>;
  /* Lines covered per individual process */
  private _linesCoveredPerProcess: Set<string>;
  private _nrOfErrorsDiscovered: number;
  protected _currentPrecomputedInputSize: number;
  private _currentEventSequenceSize: number;
  protected _maxPrecomputedInputSize: number;
  protected _maxEventSequenceSize: number;
  protected _eventHandlersTriggered: Set<string>;
  public _nrOfDataRacesDiscovered: number;

  protected iterationSpecificMergeMetrics: MergeMetrics;
  protected cumulativeMergeMetrics: MergeMetrics;

  constructor(private _metricsPath: string, private readonly branchCoverage: BranchCoverageCollector) {
    this._metrics = [];
    this._linesCoveredPerProcess = new Set();
    this._nrOfErrorsDiscovered = 0;
    this._currentPrecomputedInputSize = 0;
    this._currentEventSequenceSize = 0;
    this._maxPrecomputedInputSize = 0;
    this._maxEventSequenceSize = 0;
    this._eventHandlersTriggered = new Set();
    this._cumulativeTime = 0;
    this._nrOfDataRacesDiscovered = 0;
    this._linesCovered = new Set<string>();
    this._linesCoveredPerProcess = new Set();
    this.iterationSpecificMergeMetrics = this.newMergeMetrics();
    this.cumulativeMergeMetrics = this.newMergeMetrics();
  }

  get getMetrics() {
    return this._metrics;
  }

  public getEventHandlersExecuted(): Set<string> {
    return new Set(this._eventHandlersTriggered);
  }

  public incNumberOfErrors(): void {
    this._nrOfErrorsDiscovered++;
  }

  public setPrecomputedInputs(inputs: ComputedInput[]): void {
    const size = inputs.length;
    this._currentPrecomputedInputSize = size;
    if (size > this._maxPrecomputedInputSize) {
      this._maxPrecomputedInputSize = size;
    }
  }

  public setEventSequence(events: Event[]): void {
    const size = events.length;
    this._currentEventSequenceSize = size;
    if (size > this._maxEventSequenceSize) {
      this._maxEventSequenceSize = size;
    }
    for (let event of events) {
      const eventKey = event.toString();
      this._eventHandlersTriggered.add(eventKey);
    }
  }

  public addLineCovered(linesCoveredThisIteration): void {
    let that = this;
    linesCoveredThisIteration.forEach((object) => {
      const entry1 = object.processId + "_" + object.lineNumber;
      that._linesCoveredPerProcess.add(entry1);
      const entry2 = object.typeOfProcess + "_" + object.lineNumber;
      that._linesCovered.add(entry2);
    });
  }

  public setDataRacesDiscovered(n): void {
    this._nrOfDataRacesDiscovered = n;
  }

  private newMergeMetrics(): MergeMetrics {
    return { mergeAttempts: 0, successFulMerges: 0 };
}

  public mergeAttempted(wasSuccessful: boolean): void {
    this.iterationSpecificMergeMetrics.mergeAttempts++;
    if (wasSuccessful) {
      this.iterationSpecificMergeMetrics.successFulMerges++;
    }
  }

  public endIteration(executionTime: number, iterationNumber: number, serverProcess: Process): IterationMetric {
    this._cumulativeTime += executionTime;
    this.cumulativeMergeMetrics.mergeAttempts += this.iterationSpecificMergeMetrics.mergeAttempts;
    this.cumulativeMergeMetrics.successFulMerges += this.iterationSpecificMergeMetrics.successFulMerges;

    const nrOfTotalServerErrors = (ConfigurationReader.config.LISTEN_FOR_PROGRAM) ? 0 : serverProcess.getErrorStore().getConditions().length;
    const nrOfUnmarkedServerErrors = (ConfigurationReader.config.LISTEN_FOR_PROGRAM) ? 0 : serverProcess.getErrorStore().getUnmarkedConditions().length;
    const nrOfMarkedServerErrors = (ConfigurationReader.config.LISTEN_FOR_PROGRAM) ? 0 : serverProcess.getErrorStore().getMarkedConditions().length;
    const iterationMetrics: IterationSpecificMetrics = {
      executionTime: executionTime,
      mergeMetrics: {...this.iterationSpecificMergeMetrics}, // Shallow copy of iterationSpecificMergeMetrics
      precomputedInputSize: this._currentPrecomputedInputSize,
      eventSequenceSize: this._currentEventSequenceSize,
    };
    const errorMetrics: ErrorMetrics = {
      nrOfErrors: this._nrOfErrorsDiscovered,
      totalServerErrors: nrOfTotalServerErrors,
      unmarkedServerErrors: nrOfUnmarkedServerErrors,
      markedServerErrors: nrOfMarkedServerErrors,
    };
    const cumulativeMetrics: CumulativeMetrics = {
      nrOfLinesCovered: this._linesCovered.size,
      linesCoveredPerProcess: this._linesCoveredPerProcess.size,
      branchesPartiallyVisited: this.branchCoverage.getUniqueBranchesPartiallyVisited(),
      branchCoverage: this.branchCoverage.getBranchCoverage(),
      maxPrecomputedInputSize: this._maxPrecomputedInputSize,
      maxEventSequenceSize: this._maxEventSequenceSize,
      uniqueEventHandlersExecuted: this._eventHandlersTriggered.size,
      errors: errorMetrics,
      cumulativeTime: this._cumulativeTime,
      totalCumulativeTime: Date.now() - startTime,
      linesCovered: this._linesCovered,
      mergeMetrics: {...this.cumulativeMergeMetrics} // Shallow copy of cumulativeMergeMetrics
    };
    const metricsThisIteration: IterationMetric = {
      iteration: iterationNumber,
      individual: iterationMetrics,
      cumulative: cumulativeMetrics
    };

    this.iterationSpecificMergeMetrics = this.newMergeMetrics();

    this._metrics.push(metricsThisIteration);
    return metricsThisIteration;
  }
}
