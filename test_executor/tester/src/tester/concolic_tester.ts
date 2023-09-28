import Chalk from "chalk";

import {AssertionFailed} from "@src/tester/TestTracer/assert";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import ExitCodes from "@src/util/exit_codes";
import {Logger as Log} from "@src/util/logging";
import {InputProgram} from "@src/config/inputPrograms/InputProgram";
import modes from "@src/config/user_argument_parsing/TestingModesEnum";
import {TestRunner} from "./test-runners/TestRunner";
import TestRunnerFactory from "./TestRunnerFactory";

/**
 * Creates the TestRunner and BackendCommunicators, sets up process listeners for termination handling and provides
 * the start method to launch the tester
 */
export default class ConcolicTester {
  private _testRunner: TestRunner;
  constructor(application: InputProgram, mode: modes) {
    const testRunnerFactory = new TestRunnerFactory(mode);
    const backendCommunicator1 = testRunnerFactory.makeBackend(ConfigReader.config.INPUT_PORT, ConfigReader.config.OUTPUT_PORT, 0);
    const backendCommunicator2 = (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) ? testRunnerFactory.makeBackend(ConfigReader.config.INPUT_PORT, ConfigReader.config.OUTPUT_PORT, 1) : backendCommunicator1;
    this._testRunner = testRunnerFactory.makeTestRunner(application.processes, backendCommunicator1, backendCommunicator2);

    this._initProcessListeners();
  }

  protected _terminate(exitCode: number) {
    this._testRunner.killChildProcesses("SIGKILL");
    process.exit(exitCode);
  }

  /**
   * Handle possible process ending
   */
  protected _initProcessListeners(): void {
    // Interrupt signal
    process.on("SIGINT",  () => {
      this._terminate(ExitCodes.unknownError);
    });
    // Uncaught exception
    process.on("uncaughtException", (err) => {
      Log.ALL(Chalk.bgRed("uncaught exception"), err);
      this._terminate(ExitCodes.unknownError);
    });
    // Normal ending
    process.on("exit",  (code, signal) => {
      Log.ALL(`Main-process exited with code ${code} and signal ${signal}`);
      this._terminate(code);
    });
  }

  public start(): void {
    try {
      this._testRunner.startEverything();
    } catch (e) {
      var exitCode: ExitCodes = ExitCodes.unknownError;
      if (e instanceof AssertionFailed) {
        exitCode = ExitCodes.testTracerAssertionFailed;
      }
      console.error("Exception encountered", e);
      this._terminate(exitCode);
    }
  }
}
