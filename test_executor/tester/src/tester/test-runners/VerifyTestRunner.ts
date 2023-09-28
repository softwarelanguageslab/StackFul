import Chalk from 'chalk';

import ConfigReader from "@src/config/user_argument_parsing/Config";
import ExitCodes from "@src/util/exit_codes";
import * as GTS from "../global_tester_state";
import iterationNumber from "../iteration_number";
import {jsPrint} from "@src/util/io_operations";
import Event from "@src/tester/Event";
import Process from "@src/process/processes/Process";
import {ShouldStop} from "../ShouldStop";
import * as ShouldTestProcess from "../should_test_process";
import {TestRunner} from "./TestRunner";
import {VerifyIntraGTS} from "../verify_intra_gts";
import VerifyAdviceFactory from "@src/instrumentation/verify_advice_factory";
import {VerifyIntraBackendCommunication} from "@src/backend/VerifyIntraBackendCommunication";
import ComputedInput from "@src/backend/ComputedInput";

export class VerifyTestRunner extends TestRunner {
    private _stopIncomingMessages: any;
    private _chunkFinished: boolean;
    private _serverInputs: ComputedInput[];
    private _clientInputs: ComputedInput[];
    private _serverEvents: Event[];
    private _clientEvents: Event[];

    constructor(PROCESSES, backendCommunicator1: VerifyIntraBackendCommunication, backendCommunicator2: VerifyIntraBackendCommunication, shouldStop: ShouldStop) {
        super(PROCESSES, backendCommunicator1, backendCommunicator2, shouldStop);
        this._stopIncomingMessages = false;
        this._chunkFinished = false;
        this._serverInputs = [];
        this._clientInputs = [];
        if (ConfigReader.config.INCLUDE_INITIAL_EVENT) {
            this._serverEvents = [new Event(0, 0)];
            this._clientEvents = [new Event(1, 0)];
        } else {
            this._clientEvents = [];
            this._serverEvents = [];
        }
    }

    getAdviceFactory(): VerifyAdviceFactory {
        return new VerifyAdviceFactory(this);
    }

    _makeGlobalTesterState(events, globalInputs): VerifyIntraGTS {
        return new VerifyIntraGTS(this._processes, events, globalInputs, undefined);
    }

    getBackendCommunicator(): VerifyIntraBackendCommunication {
        if (this._chunkFinished) {
            return this._backendCommunicator2 as VerifyIntraBackendCommunication;
        } else {
            return this._backendCommunicator1 as VerifyIntraBackendCommunication;
        }
    }

    attemptConnection(inputs, events, errorTargetPosition, state, messageInputMap): void {
        function convertInputs() {
            // Drop all symbolic inputs that were directly generated from message inputs
            var counter = 0;
            const newInputs : any[] = [];
            for (let input of inputs) {
                if (input.processId === 0 && messageInputMap.getInputType(input.id) === "regular") {
                    const copiedInput = Object.assign({}, input);
                    copiedInput.id = counter;
                    newInputs.push(copiedInput);
                    counter++;
                } else if (input.processId !== 0) {
                    newInputs.push(input);
                }
            }
            return newInputs;
        }

        jsPrint(Chalk.blue(`VerifyTestRunner.attemptConnection 1`), inputs);
        jsPrint(Chalk.blue(`VerifyTestRunner.attemptConnection 2`), events);
        const convertedInputs = convertInputs()
        jsPrint("VerifyTestRunner.attemptConnection 3, converted inputs:", convertedInputs);
        this._stopIncomingMessages = {
            state,
            errorTargetPosition,
            inputs: convertedInputs,
            events: events.map((event) => new Event(event.processId, event.targetId))
        };
    }

    incomingMessage(process, messageType): void {
        if (process.isANodeProcess() && !this._stopIncomingMessages) {
            const errors = process.getErrorStore().getUnmarkedConditions();
            const eventSequences = errors.map((tuple) => tuple.eventSequence);
            // @ts-ignore
            const currentPC = GTS.globalTesterState!.getGlobalPathConstraint();
            const firstTuple = errors.find((tuple) => tuple.eventSequence[0].type === "socketIO" && tuple.eventSequence[0].specificEventType === messageType);
            if (firstTuple !== undefined) {
                const communicator = this.getBackendCommunicator();
                const canBeConnected = communicator.checkConnectsWithPC(currentPC, firstTuple.condition);
                if (canBeConnected) {
                    // @ts-ignore
                    this.attemptConnection(canBeConnected.inputs, GTS.globalTesterState!.getEventSequence(), firstTuple.position, firstTuple.state, firstTuple.messageInputMap);
                }
            }
        }
    }

    errorDiscovered(process: Process, errorMessage): void {
        // @ts-ignore
        process.errorDiscovered(errorMessage, GTS.globalTesterState!.getCurrentEventSequence(), codePosition);
        if (this._chunkFinished) {
            if (this._stopIncomingMessages && this._stopIncomingMessages.state === errorMessage) {
                this._stopIncomingMessages = false;
                jsPrint(Chalk.bgBlue("Encountered expected error:"), errorMessage);
                process.getErrorStore().markExplored(errorMessage);
            }
            // @ts-ignore
            if (GTS.globalTesterState!.getNodeProcesses().every((process) => process.getErrorStore().getUnmarkedConditions().length === 0)) {
                this._successFullyFinishedTesting();
            }
        }
    }

    _terminateAndRestart(globalInputs, events): void {
        const that = this;
        setTimeout(function () {
            that._aran.orchestrator.destroy();
            that._aran.destroy();
            that.startMain(globalInputs, events);
        }, 5000);
        // @ts-ignore
        GTS.globalTesterState!.getProcesses().forEach((process) => that._aran.terminate(process.alias));
    }

    communicateWithBackend(backendCommunicator: VerifyIntraBackendCommunication): void {
        const terminateAllProcesses = (result) => {

            if (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
                if (result.isValid) {
                    this._terminateAndRestart(result.inputs, result.events);
                } else {
                    jsPrint(Chalk.red("Invalid inputs"));
                    this._stopTesting(ExitCodes.unknownError);
                }
            } else {
                const nextIterationNumber = iterationNumber.get() + 1;
                if (ShouldTestProcess.shouldExclusivelyTestClient(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestClient(nextIterationNumber)) {
                    this._terminateAndRestart(result.inputs, result.events);
                } else if (ShouldTestProcess.shouldExclusivelyTestServer(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestServer(nextIterationNumber)) {
                    this._terminateAndRestart(result.inputs, result.events);
                } else if (ShouldTestProcess.shouldExclusivelyTestClient(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestServer(nextIterationNumber)) {
                    this._clientInputs = result.inputs;
                    this._clientEvents = result.events;
                    this._terminateAndRestart(this._serverInputs, this._serverEvents);
                } else if (ShouldTestProcess.shouldExclusivelyTestServer(iterationNumber.get()) && ShouldTestProcess.shouldExclusivelyTestClient(nextIterationNumber)) {
                    this._serverInputs = result.inputs;
                    this._serverEvents = result.events;
                    this._terminateAndRestart(this._clientInputs, this._clientEvents);
                } else {
                    throw new Error("Can use neither client nor server inputs");
                }
            }
        }

        let result;
        if (this._stopIncomingMessages) {
            result = {
                isValid: true,
                inputs: this._stopIncomingMessages.inputs,
                events: this._stopIncomingMessages.events
            };
        } else {
            // @ts-ignore
            backendCommunicator.sendPathConstraint(GTS.globalTesterState!.getGlobalPathConstraint(), GTS.globalTesterState!.getProcesses(), ConfigReader.config.TEST_INDIVIDUAL_PROCESSES);
            result = backendCommunicator.receiveSolverResult();
        }
        jsPrint("In VerifyTestRunner.communicateWithBackend, result =", result);
        terminateAllProcesses(result);
    }

    main(globalInputs: ComputedInput[], events: Event[]): void {
        if (this._chunkFinished && this._processes[0].getErrorStore().getUnmarkedConditions().length === 0) {
            // All errors (if any were found) discovered during intra-process testing of the server have been found.
            this._successFullyFinishedTesting();
        } else {
            super.main(globalInputs, events);
        }
    }

    startMain(globalInputs: ComputedInput[], events: Event[]): void {
        if (this._isChunkFinished(iterationNumber.get() + 1)) {
            this._chunkFinished = true;
            ConfigReader.config.TEST_INDIVIDUAL_PROCESSES = false;
            ConfigReader.config.INPUT_SUFFIX = "";
            this._processes.forEach((process) => process.revertToInterTesting());
        }
        return this.main(globalInputs, events);
    }

    private _isChunkFinished(iterationNumber: number): boolean {
        return iterationNumber > ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS;
    }

    private _chunkJustFinished(iteration = iterationNumber.get()): boolean {
        return iteration === ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS + 1;
    }

    startEverything(): void {
        if (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
            if (ShouldTestProcess.shouldTestServer(undefined)) {
                jsPrint("shouldTestServer", this._serverInputs, this._serverEvents);
                this.startMain(this._serverInputs, this._serverEvents);
            } else {
                jsPrint("NOT shouldTestServer", this._clientInputs, this._clientEvents);
                this.startMain(this._clientInputs, this._clientEvents);
            }
        } else {
            this.startMain(this._clientInputs, this._clientEvents);
        }
    }
}