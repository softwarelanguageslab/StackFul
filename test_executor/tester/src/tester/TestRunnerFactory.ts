import ExploreModeBackendCommunication from "../backend/ExploreModeBackendCommunication";
import {ExploreTestRunner} from "./test-runners/ExploreTestRunner"
import FunctionSummariesModeBackendCommunication from "../backend/FunctionSummariesModeBackendCommunication";
import {FunctionSummariesTestRunner} from "./test-runners/FunctionSummariesTestRunner"
import {ShouldNeverStop} from "./ShouldStop";
import SolveModeBackendCommunication from "../backend/SolveModeBackendCommunication";
import {SolveTestRunner} from "./test-runners/SolveTestRunner"
import {VerifyIntraBackendCommunication} from "../backend/VerifyIntraBackendCommunication";
import {VerifyTestRunner} from "./test-runners/VerifyTestRunner"
import TestingModesEnum from "../config/user_argument_parsing/TestingModesEnum"
import {TestRunner, } from "./test-runners/TestRunner"

export default class TestRunnerFactory {

    private _mode: any;

    constructor(mode: TestingModesEnum) {
        this._mode = mode;
    }

    makeBackend(inputPort: number, outputPort: number, backendId: number) {
        switch (this._mode) {
            case TestingModesEnum.BruteForce: return new ExploreModeBackendCommunication(inputPort, outputPort, backendId);
            case TestingModesEnum.FunctionSummaries: return new FunctionSummariesModeBackendCommunication(inputPort, outputPort, backendId);
            case TestingModesEnum.SymJS: return new SolveModeBackendCommunication(inputPort, outputPort, backendId);
            case TestingModesEnum.Verify: return new VerifyIntraBackendCommunication(inputPort, outputPort, backendId);
            default: throw new Error(`Unknown mode: ${this._mode}`);
        }
    }

    makeTestRunner(application, backendCommunicator1, backendCommunicator2): TestRunner {
        const shouldStop = new ShouldNeverStop();
        switch (this._mode) {
            case TestingModesEnum.BruteForce: return new ExploreTestRunner(application, backendCommunicator1, backendCommunicator2, shouldStop);
            case TestingModesEnum.FunctionSummaries: return new FunctionSummariesTestRunner(application, backendCommunicator1, backendCommunicator2, shouldStop);
            case TestingModesEnum.SymJS: return new SolveTestRunner(application, backendCommunicator1, backendCommunicator2, shouldStop);
            case TestingModesEnum.Verify: return new VerifyTestRunner(application, backendCommunicator1, backendCommunicator2, shouldStop);
            default: throw new Error(`Unknown mode: ${this._mode}`);
        }
    }
}