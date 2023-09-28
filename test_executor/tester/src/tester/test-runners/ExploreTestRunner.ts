import Chalk from 'chalk';

import {AdviceFactory} from "@src/instrumentation/advice_factory";
import {BackendResult, BackendResultType, InputsAndEventsReceived} from "@src/backend/BackendResult";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import Event from "@src/tester/Event";
import {ExecutionState, TargetEndExecutionState} from "../ExecutionState";
import ExitCodes from "@src/util/exit_codes";
import ExploreModeBackendCommunication from "@src/backend/ExploreModeBackendCommunication";
import * as GTS from "../global_tester_state"
import iterationNumber from "../iteration_number";
import {jsPrint} from "@src/util/io_operations";
import {Logger as Log} from "@src/util/logging"
import {ShouldNeverStop, ShouldStop} from "../ShouldStop";
import * as ShouldTestProcess from "../should_test_process";
import {TestRunner} from "./TestRunner";
import BackendResultState from "@src/backend/BackendResultState";
import ComputedInput from "@src/backend/ComputedInput";
import SharedOutput from "@src/util/output";

export class ExploreTestRunner extends TestRunner {
    protected _serverInputs: ComputedInput[];
    protected _clientInputs: ComputedInput[];
    protected _serverEvents: Event[];
    protected _clientEvents: Event[];
    constructor(PROCESSES: any, backendCommunicator1: ExploreModeBackendCommunication, backendCommunicator2: ExploreModeBackendCommunication, shouldStop: ShouldStop) {
        super(PROCESSES, backendCommunicator1, backendCommunicator2, shouldStop);
        this._serverInputs = [];
        this._clientInputs = [];
        if (ConfigReader.config.INCLUDE_INITIAL_EVENT) {
            const webProcessId: number = (ConfigReader.config.LISTEN_FOR_PROGRAM) ? 0 : 1;
            this._serverEvents = [new Event(0, 0)];
            this._clientEvents = [new Event(webProcessId, 0)];
        } else {
            this._clientEvents = [];
            this._serverEvents = [];
        }
    }

    getAdviceFactory(): AdviceFactory {
        return new AdviceFactory();
    }

    protected eventHandlerFinished(): void {
        super.eventHandlerFinished();
        if (! ConfigReader.config.LISTEN_FOR_PROGRAM) {
            console.assert(GTS.globalTesterState);
            // Only generate a StopTesting constraint if tester is currently executing an event handler
            if (GTS.globalTesterState!.getCurrentEvent() !== null) {
                const id = GTS.globalTesterState!.getCurrentEvent()!.id;
                GTS.globalTesterState!.setTargetEndEncountered(1); // TODO: hard-coded client process id
                if (ConfigurationReader.config.MERGE_PATHS) {
                    const executionState = new TargetEndExecutionState(id);
                    this.tryMerge(executionState);
                }
            }
        }
    }

    tryMerge(executionState: ExecutionState): void {
        if (ConfigReader.config.MERGE_PATHS && !this._isStopping) {
            console.assert(GTS.globalTesterState);
            const pc = GTS.globalTesterState!.getGlobalPathConstraint();
            const processes = GTS.globalTesterState!.getProcesses();
            const prescribedEvents = GTS.globalTesterState!.getEventSequence();
            const result = (this.getBackendCommunicator() as ExploreModeBackendCommunication).sendTryMerge(executionState, pc, processes, prescribedEvents);
            const mergeSuccessFul = result.state === BackendResultState.SUCCESS;
            SharedOutput.getOutput().mergeAttempted(mergeSuccessFul);
        }
    }

    communicateWithBackend(backendCommunicator: ExploreModeBackendCommunication, stoppedBeforeEnd: boolean): void {
        const that = this;

        function terminateAllProcesses(result: BackendResult) {
            function start(globalInputs: ComputedInput[], events: Event[]) {
                setTimeout(function () {
                    that._aran.orchestrator.destroy();
                    that._aran.destroy();
                    that.startMain(globalInputs, events);
                }, 5000);
                GTS.globalTesterState!.getProcesses().forEach((process) => that._aran.terminate(process.alias));
            }
            jsPrint(result);
            const newShouldStop = new ShouldNeverStop();
            that._shouldStop = newShouldStop;

            if (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
                if (result.state == BackendResultState.SUCCESS) {
                    const castedResult = result as InputsAndEventsReceived;
                    start(castedResult.inputs, castedResult.events);
                } else if (result.state == BackendResultState.FINISHED) {
                    that._successFullyFinishedTesting();
                    that._stopTesting(ExitCodes.success);
                } else if (result.state == BackendResultState.INVALID) {
                    Log.ALL(Chalk.red("Invalid inputs"));
                    that._stopTesting(ExitCodes.unknownError);
                }
            } else {
                switch (result.type) {
                    case BackendResultType.InputsAndEventsReceived:
                    case BackendResultType.InputsAndEventsAndPathReceived:
                        const castedResult = result as InputsAndEventsReceived;
                        const nextIterationNumber = iterationNumber.get() + 1;
                        if (ShouldTestProcess.shouldExclusivelyTestClient(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestClient(nextIterationNumber)) {
                            start(castedResult.inputs, castedResult.events);
                        } else if (ShouldTestProcess.shouldExclusivelyTestServer(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestServer(nextIterationNumber)) {
                            start(castedResult.inputs, castedResult.events);
                        } else if (ShouldTestProcess.shouldExclusivelyTestClient(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestServer(nextIterationNumber)) {
                            that._clientInputs = castedResult.inputs;
                            that._clientEvents = castedResult.events;
                            start(that._serverInputs, that._serverEvents);
                        } else if (ShouldTestProcess.shouldExclusivelyTestServer(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestClient(nextIterationNumber)) {
                            that._serverInputs = castedResult.inputs;
                            that._serverEvents = castedResult.events;
                            start(that._clientInputs, that._clientEvents);
                        } else {
                            throw new Error("Can use neither client nor server inputs");
                        }
                }
            }
        }

        backendCommunicator.sendPathConstraint(GTS.globalTesterState!.getGlobalPathConstraint(), GTS.globalTesterState!.getProcesses(), stoppedBeforeEnd, GTS.globalTesterState!.getEventSequence());
        const result = backendCommunicator.receiveSolverResult() as BackendResult;
        Log.BCK("In ExploreTestRunner.communicateWithBackend, result =", result);
        terminateAllProcesses(result);
    }

    startMain(globalInputs: ComputedInput[], events: Event[]): void {
        // Log.ALL("ExploreTestRunner.startMain, events =", events);
        return this.main(globalInputs, events);
    }

    startEverything(): void {
        if (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
            if (ShouldTestProcess.shouldTestServer(undefined)) {
                // Log.ALL("shouldTestServer", this._serverInputs, this._serverEvents);
                this.startMain(this._serverInputs, this._serverEvents);
            } else {
                // Log.ALL("NOT shouldTestServer", this._clientInputs, this._clientEvents);
                this.startMain(this._clientInputs, this._clientEvents);
            }
        } else {
            this.startMain(this._clientInputs, this._clientEvents);
        }
    }

}