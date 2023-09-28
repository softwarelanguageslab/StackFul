import Chalk from 'chalk';

import ConfigReader from "@src/config/user_argument_parsing/Config";
import * as ExitCodes from "@src/util/exit_codes";
import {ExploreTestRunner} from "./ExploreTestRunner";
import FunctionSummariesAdviceFactory from "@src/instrumentation/function_summaries_advice_factory";
import {FunctionSummariesGTS} from "../function_summaries_gts";
import FunctionSummaryHandler from "../function_summary/function_summary_handler";
import * as GTS from "../global_tester_state";
import iterationNumber from "../iteration_number";
import {Logger as Log} from "@src/util/logging";
import {ShouldStop} from "../ShouldStop";
import * as ShouldTestProcess from "../should_test_process";
import ComputedInput from "@src/backend/ComputedInput";
import Event from "@src/tester/Event";

export class FunctionSummariesTestRunner extends ExploreTestRunner {
    private readonly _functionSummaryHandler: FunctionSummaryHandler;
    private _lastCallInformation: { lastFrame?: any; concreteFrame?: any };

    constructor(processes,
                backendCommunicator1: any, //backend communicators will be of type FunctionSummariesModeBackendCommunication
                backendCommunicator2: any,
                shouldStop: ShouldStop) {
        super(processes, backendCommunicator1, backendCommunicator2, shouldStop);

        this._functionSummaryHandler = new FunctionSummaryHandler(this.getBackendCommunicator(), this);
        this._lastCallInformation = {};
    }

    getAdviceFactory(): FunctionSummariesAdviceFactory {
        return new FunctionSummariesAdviceFactory(this._functionSummaryHandler);
    }

    _makeGlobalTesterState(events: Event[], globalInputs: ComputedInput[]): FunctionSummariesGTS {
        return new FunctionSummariesGTS(this._processes, events, globalInputs);
    }

    _findFirstFunctionToSummarise(backendCommunicator: any, produceResult): void {

        const terminateAllProcesses = (result) => {

            const start = (globalInputs: ComputedInput[], events: Event[]) => {
                setTimeout( () => {
                    this._aran.orchestrator.destroy();
                    this._aran.destroy();
                    this.startMain(globalInputs, events);
                }, 5000);
                // todo: below is buggy since _aran.terminate does not accept a parameter
                GTS.globalTesterState!.getProcesses().forEach((process) => this._aran.terminate(process.alias));
            }

            if (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
                if (result.type === "FunctionFullyExplored") {
                    Log.ALL(Chalk.green("FunctionSummaryTestRunner._findFirstFunctionToSummarise, FunctionFullyExplored"));
                    this._functionSummaryHandler.functionSummarised();
                    if (this._functionSummaryHandler.getContextStack().isEmpty()) {
                        this._successFullyFinishedTesting();
                    } else {
                        const poppedContextFrame = this._functionSummaryHandler.getContextStack().pop();
                        Log.ALL("FunctionSummaryTestRunner._findFirstFunctionToSummarise, popped context stack is now", this._functionSummaryHandler.getContextStack());

                        function retrievePreviousInputs() {
                            const symbolicConcreteInputs = poppedContextFrame.getSymbolicConcreteInputs();
                            const inputs = symbolicConcreteInputs.map((input) => input.symbolic.toSymbolicConcreteValue(input.base));
                            Log.ALL(`FSTestRunner._findFirstFunctionToSummarise, reusing previous symbolic-concrete inputs of context frame ${poppedContextFrame.getFunctionId()}. Inputs =`, inputs);
                            return {isValid: true, inputs: inputs};
                        }

                        this._findFirstFunctionToSummarise(backendCommunicator, retrievePreviousInputs);
                    }
                } else if (result.isValid) {
                    start(result.inputs, []);
                } else {
                    Log.ALL(Chalk.red("Invalid inputs"));
                    // @ts-ignore
                    this._stopTesting(ExitCodes.unknownError);
                }
            } else {
                if (!ShouldTestProcess.shouldTestServer(undefined)) {
                    this._serverInputs = result.inputs;
                    this._serverEvents = result.events;
                    start(this._clientInputs, this._clientEvents);
                } else {
                    this._clientInputs = result.inputs;
                    this._clientEvents = result.events;
                    start(this._serverInputs, this._serverEvents);
                }
            }
        }

        const result = produceResult();

        terminateAllProcesses(result);
    }

    _exploreTopFunction(backendCommunicator: any, suffixPathConstraint, timesCalledMap): void {
        const that = this;
        if (this._functionSummaryHandler.getContextStack().isEmpty()) {
            this._successFullyFinishedTesting();
        } else {
            const contextFrame = this._functionSummaryHandler.getContextStack().top();

            function sendExploreFunction() {
                const prefix = contextFrame.getPrefixConstraints().map((constraint) => constraint.toBranchConstraint(timesCalledMap));
                Log.ALL(`FSTestRunner._exploreTopFunction, it: ${iterationNumber.get()}, contextFrame.getAllCorrespondingSymbolicInputs() = `, contextFrame.getAllCorrespondingSymbolicInputs());
                const instancedCorrespondingInputs = contextFrame.getAllCorrespondingSymbolicInputs();
                const allArgs = contextFrame.getAllArgs();
                Log.ALL(`FSTestRunner._exploreTopFunction, it: ${iterationNumber.get()}, allArgs = `, allArgs);
                backendCommunicator.sendExploreFunction(contextFrame.getFunctionId(), contextFrame.getFunctionSummary(), allArgs, prefix, suffixPathConstraint, instancedCorrespondingInputs);
                return backendCommunicator.receiveSolverResult();
            }

            this._findFirstFunctionToSummarise(backendCommunicator, sendExploreFunction);
        }
    }

    communicateWithBackend(backendCommunicator: any): void {
        const timesCalledMap = this._functionSummaryHandler.getTimesCalledMap();
        const suffixPathConstraint = this._lastCallInformation.lastFrame;
        this._exploreTopFunction(backendCommunicator, suffixPathConstraint, timesCalledMap);
    }

    stopExploringFunction(concreteFrame, lastFrame): void {
        this._lastCallInformation = {concreteFrame: concreteFrame, lastFrame: lastFrame};
        this._endGlobalTestIteration(false); // TODO should pass false?
    }

    main(globalInputs:ComputedInput[], events: Event[]): void {
        this._functionSummaryHandler.reset();
        Log.ALL("FSTestRunner.main, contextStack = ", this._functionSummaryHandler.getContextStack());
        super.main(globalInputs, events);
    }
}
