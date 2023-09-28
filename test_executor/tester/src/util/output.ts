import Path from "path";

import * as IO from "./io_operations";
import ConfigReader from "../config/user_argument_parsing/Config";
import iterationNumber from "../tester/iteration_number";
import Metrics, {IterationMetric} from "./metrics/metrics";
import Process from "@src/process/processes/Process";
import BranchCoverageCollector, {BranchCoverageEntry} from "@src/util/metrics/BranchCoverageCollector";
import CodePosition from "@src/instrumentation/code_position";
import AranNode from "@src/instrumentation/AranNode";
import LineColumnCodePosition from "@src/instrumentation/LineColumnCodePosition.js";

class Output {

  private static SEPARATOR = ";";

  private readonly _errorsPath: string;
  private readonly _inputValuesPath: string;
  private readonly _metricsPath: string;
  private readonly linesCoveredDirectoryPath: string;
  private readonly _metrics: Metrics;
  private readonly _errorsDiscovered: Set<any>;
  private readonly branchCoverageCollector: BranchCoverageCollector;

  /**
   * Provides initialization routines and methods to write to output files.
   * @constructor
   */
  constructor(public outputDirPath) {
    IO.makeDir(this.outputDirPath, { recursive: true });
    this._errorsPath = this.makeOutputFilePath("errors", "txt");
    this._inputValuesPath = this.makeOutputFilePath("input_values", "csv");
    this._metricsPath = this.makeOutputFilePath("metrics", "csv");
    this.branchCoverageCollector = new BranchCoverageCollector();
    this._metrics = new Metrics(this._metricsPath, this.branchCoverageCollector);

    this._errorsDiscovered = new Set();

    // Clear existing output files
    IO.clearFile(this._errorsPath);
    IO.clearFile(this._inputValuesPath);
    IO.clearFile(this._metricsPath);

    this.linesCoveredDirectoryPath = Path.join(this.outputDirPath, "/lines_covered");
    IO.removeDir(this.linesCoveredDirectoryPath);

    // Add metrics header to metrics csv
    const metricsHeader = this.arrayToCSVRow([
      "iteration", "linesCovered", "linesCoveredPerProcess", "branchesCovered",
      "nrOfErrors", "nrOfTotalServerErrors", "nrOfUnmarkedServerErrors", "nrOfMarkedServerErrors",
      "cumulativeMergeAttempts", "cumulativeSuccessfulMerges",
      "currentInputSize", "maxInputSize", "currentEventSequenceSize", "maxEventSequenceSize", "uniqueEventHandlersExecuted",
      "executionTime", "cumulativeTime", "totalCumulativeTime"]);
    IO.appendFile(this._metricsPath, metricsHeader);
  }

  public tookBranch(position: CodePosition, filePosition: LineColumnCodePosition, wasTrue: boolean): void {
    this.branchCoverageCollector.tookBranch(position, filePosition, wasTrue);
  }

  public makeBranchCoverageSnapshot(): BranchCoverageEntry[] {
    return this.branchCoverageCollector.makeSnapshot();
  }

  public makeEventHandlersExploredSnapshot(): string[] {
    return [...this._metrics.getEventHandlersExecuted()];
  }

  public includeAST(root: AranNode, processId: number): void {
    this.branchCoverageCollector.includeAST(root, processId);
  }

  protected _metricsObjectToArray(metrics: IterationMetric): any[] {
    return [
      metrics.iteration, metrics.cumulative.nrOfLinesCovered, metrics.cumulative.linesCoveredPerProcess, metrics.cumulative.branchCoverage,
      metrics.cumulative.errors.nrOfErrors, metrics.cumulative.errors.totalServerErrors,
      metrics.cumulative.errors.unmarkedServerErrors, metrics.cumulative.errors.markedServerErrors,
      metrics.cumulative.mergeMetrics.mergeAttempts, metrics.cumulative.mergeMetrics.successFulMerges,
      metrics.individual.precomputedInputSize, metrics.cumulative.maxPrecomputedInputSize, metrics.individual.eventSequenceSize, metrics.cumulative.maxEventSequenceSize, metrics.cumulative.uniqueEventHandlersExecuted,
      metrics.individual.executionTime, metrics.cumulative.cumulativeTime, metrics.cumulative.totalCumulativeTime];
  }

  private arrayToCSVRow(array: any[]): string {
    return array.join(Output.SEPARATOR) + "\n";
  }

  /**
   * Constructs full output file path by using fileName, extension and suffix (defined in config)
   * @param fileName
   * @param extensionOrEmpty
   * @private
   */
  private makeOutputFilePath(fileName: string, extensionOrEmpty: string): string {
    const extension = (extensionOrEmpty === "") ? "" : ("." + extensionOrEmpty);
    const suffix = (ConfigReader.config.OUTPUT_SUFFIX === "") ? "" : ("_" + ConfigReader.config.OUTPUT_SUFFIX);
    return Path.join(this.outputDirPath, "/" + fileName + suffix + extension);
  }

  getMetrics() {
    return this._metrics;
  }

  errorDiscovered(serial, aranNodes): void {
    if (!this._errorsDiscovered.has(serial)) {
      this._errorsDiscovered.add(serial);
      const loc = (aranNodes[serial] && aranNodes[serial].loc) ? JSON.stringify(aranNodes[serial].loc) : "unknown_line";
      const toWrite = `${iterationNumber.get()}:${loc}\n`;
      IO.appendFile(this._errorsPath, toWrite);
      this._metrics.incNumberOfErrors();
    }
  }

  protected writeLinesCovered(linesCovered: Set<string>): void {
    IO.makeDir(this.linesCoveredDirectoryPath, {recursive:true});
    const linesCoveredPath = Path.join(this.linesCoveredDirectoryPath, "iteration_" + iterationNumber.get() + ".txt");
    IO.clearFile(linesCoveredPath);
    for (let line of linesCovered) {
      IO.appendFile(linesCoveredPath, line + "\n");
    }
  }

  public mergeAttempted(wasSuccessFul: boolean): void {
    this._metrics.mergeAttempted(wasSuccessFul);
  }

  public endIteration(executionTime: number, serverProcess: Process) {
    const metricsThisIteration = this._metrics.endIteration(executionTime, iterationNumber.get(), serverProcess);
    this.writeLinesCovered(metricsThisIteration.cumulative.linesCovered);
    const array = this._metricsObjectToArray(metricsThisIteration);
    const toWrite = this.arrayToCSVRow(array);
    IO.appendFile(this._metricsPath, toWrite)
    return metricsThisIteration;
  }

  writeInputValue(processId: number, $$input) {
    const toWrite = this.arrayToCSVRow([iterationNumber.get(), processId, "input", $$input.symbolic.id, $$input.base, ""]);
    IO.appendFile(this._inputValuesPath, toWrite)
  }

  writeEvent(processId: number, targetId: number, eventType: string, specificEventName: string) {
    const toWrite = this.arrayToCSVRow([iterationNumber.get(), processId, "event", targetId, eventType, specificEventName]);
    IO.appendFile(this._inputValuesPath, toWrite)
  }
}

/**
 * Singleton implementation to share Output instance
 */
export default class SharedOutput {
  private static output: Output | undefined = undefined;

  static getOutput(): Output {
    if(SharedOutput.output === undefined) {
      SharedOutput.output = new Output(ConfigReader.config.OUTPUT_REL_PATH);
    }
    return SharedOutput.output;
  }
}
