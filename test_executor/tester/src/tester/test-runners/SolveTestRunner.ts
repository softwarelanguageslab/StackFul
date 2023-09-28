import Chalk from 'chalk';

import {AdviceFactory} from "@src/instrumentation/advice_factory";
import BranchConstraint from "@src/instrumentation/constraints/BranchConstraint";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import {getSymJSHandler, symJSHandlerClient, symJSHandlerServer} from "../solve/util";
import * as GTS from "../global_tester_state";
import {Logger as Log} from "@src/util/logging";
import {ShouldStop} from "../ShouldStop";
import * as ShouldTestProcess from "../should_test_process"
import SolveModeBackendCommunication from "@src/backend/SolveModeBackendCommunication";
import SymJSState from "../solve/symjs_state";
import {TestRunner} from "./TestRunner";

export class SolveTestRunner extends TestRunner {
    private _serverInputs: any[];
    private _clientInputs: any[];
    private _serverState: SymJSState | undefined;
    private _clientState: SymJSState | undefined;

    constructor(PROCESSES, backendCommunicator1: SolveModeBackendCommunication, backendCommunicator2: SolveModeBackendCommunication, shouldStop: ShouldStop) {
        super(PROCESSES, backendCommunicator1, backendCommunicator2, shouldStop);
        this._serverInputs = [];
        this._clientInputs = [];
    }

    getAdviceFactory(): AdviceFactory {
        return new AdviceFactory();
    }

    /**
     * Communicate with the backend
     * It is responsible to retrieve the globalInputs (from solver) and the symJSState (from SymJSHandler) to test
     * a new iteration
     * @param backendCommunicator
     */
    communicateWithBackend(backendCommunicator: SolveModeBackendCommunication) {
        const start = (globalInputs, nextState) => {
            setTimeout(() => {
                this._aran.orchestrator.destroy();
                this._aran.destroy();
                this.startMain(globalInputs, nextState);
            }, 5000);
            GTS.globalTesterState!.getProcesses().forEach((process) => this._aran.terminate(process.alias));
        }

        const terminateAllProcesses = (nextState, result) => {
            if (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
                if (result.isValid) {
                    start(result.inputs, nextState);
                } else {
                    Log.ALL(Chalk.red("Invalid inputs, trying another state"));
                    tryNextState(false);
                }
            } else {
                if (result.isValid) {
                    if (!ShouldTestProcess.shouldTestServer(undefined)) {
                        this._serverInputs = result.inputs;
                        this._serverState = nextState;
                        start(this._clientInputs, this._clientState);
                    } else {
                        this._clientInputs = result.inputs;
                        this._clientState = nextState;
                        start(this._serverInputs, this._serverState);
                    }
                } else {
                    Log.ALL(Chalk.red("Invalid inputs, trying another state"));
                    tryNextState(false);
                }
            }
        }

        /**
         * Try to run next iteration. If solver status
         * @param lastSolverResultStatus: last returned status by solver. If true then we have received the global inputs
         * and event sequence
         */
        const tryNextState = (lastSolverResultStatus: boolean) => {
            const nextState = (lastSolverResultStatus) ?
                getSymJSHandler().nextState(GTS.globalTesterState!.getBranchSequence()) :
                getSymJSHandler().dequeueNextState();

            if (!nextState) {
                // No more states to test, let startMain finish the testing
                this.startMain([], nextState);
            } else {
                // Let solver generate concrete values from the path constraint and run
                backendCommunicator.sendPathConstraintWithNextState(GTS.globalTesterState!.getGlobalPathConstraint(), nextState);
                const result = backendCommunicator.receiveSolverResult();
                terminateAllProcesses(nextState, result);
            }
        }

        tryNextState(true);
    }

    /**
     * Starts an iteration. It is called either from startEverything (initial run), or after communication with the backend.
     *
     * @param globalInputs
     * @param symJSState
     * @private
     */
    private startMain(globalInputs, symJSState: SymJSState | undefined): void {
        if (!symJSState) {
            this._successFullyFinishedTesting();
        } else {
            const events = symJSState.getEventSequence().slice();
            return this.main(globalInputs, events);
        }
    }

    /**
     * This is the entry point to start the tester
     * Will check ConfigReader.config.TEST_INDIVIDUAL_PROCESSES
     */
    startEverything(): void {
        if (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
            this._clientState = symJSHandlerClient.nextState();
            this._serverState = symJSHandlerServer.nextState();
            if (ShouldTestProcess.shouldTestServer()) {
                this.startMain(this._serverInputs, this._serverState);
            } else {
                this.startMain(this._clientInputs, this._clientState);
            }
        } else {
            const state = getSymJSHandler().nextState();
            this.startMain([], state);
        }
    }

    receiveLocalDefinition($$definition): void {
        const currentEvent = GTS.globalTesterState?.getCurrentEvent()
        if (currentEvent != undefined) {
            // Todo: refactor to support TEST_INDIVIDUAL_PROCESSES
            getSymJSHandler().taintAnalyzer.registerLocalDefinition(currentEvent, $$definition)
        }
    }

    receiveSymbolicTestCondition(serial: number, alias: string, constraint: BranchConstraint): void {
        super.receiveSymbolicTestCondition(serial, alias, constraint)
        const currentEvent = GTS.globalTesterState?.getCurrentEvent()
        if (currentEvent) {
            // Todo: refactor to support TEST_INDIVIDUAL_PROCESSES
            getSymJSHandler().taintAnalyzer.registerBranchConstraint(serial, currentEvent, constraint)
        }
    }
}