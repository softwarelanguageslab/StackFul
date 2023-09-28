import {FunctionSummariesGTS} from "../function_summaries_gts";
import iterationNumber from "../iteration_number";
import CounterMap from "../../util/datastructures/counter_map";
import FunctionSummary from "./function_summary";
import FunctionSummaryWrapper from "./function_summary_wrapper";
import * as GTS from "../global_tester_state";
import {jsPrint} from "../../util/io_operations";
import tainter from "../../instrumentation/tainter";
import Stack from "../../util/datastructures/stack";
import Chalk from "chalk";
import FixedConstraint from "../../instrumentation/constraints/FixedConstraint";
import {generateReturnValueConstrained} from "../../symbolic_expression/helper";
import BranchConstraint from "../../instrumentation/constraints/BranchConstraint";

class ConcreteFrame {
    private _testIteration: number = iterationNumber.get()

    constructor(private _functionId, private _$$argsArray, private _localBacktrackingFlag, private _preConditions, private _functionSummary) {
    }

    get testIteration(): number {
        return this._testIteration;
    }

    getFunctionId() {
        return this._functionId;
    }

    getArgsArray() {
        return this._$$argsArray;
    }

    getLocalBacktrackingFlag() {
        return this._localBacktrackingFlag;
    }

    getPreConditions() {
        return this._preConditions;
    }

    getFunctionSummary() {
        return this._functionSummary;
    }
}

class ContextFrame {
    private _testIteration: number = iterationNumber.get()

    constructor(private _functionId, private _symbolicConcreteInputs, private _functionSummary, private _prefixConstraints, private _allArgs, private _allCorrespondingSymbolicInputs) {
    }

    getTestIteration() {
        return this._testIteration;
    }

    getFunctionId() {
        return this._functionId;
    }

    getFunctionSummary() {
        return this._functionSummary;
    }

    getSymbolicConcreteInputs() {
        return this._symbolicConcreteInputs;
    }

    getPrefixConstraints() {
        return this._prefixConstraints;
    }

    getAllArgs() {
        return this._allArgs;
    }

    getAllCorrespondingSymbolicInputs() {
        return this._allCorrespondingSymbolicInputs;
    }

    equals(other) {
        return (other instanceof ContextFrame) && (other.getFunctionId() === this.getFunctionId());
    }
}

function createSubstitutes(argsToReplace, timesCalledMap) {
    const map = new Map();
    return argsToReplace.map((arg) => {
        // const key = `${arg.functionId}__${arg.processId}__${arg.timesCalled}`;
        let timesCalled = map.get(arg.functionId);
        if (timesCalled === undefined) {
            timesCalledMap.incCounter(arg.functionId);
            const currentTC = timesCalledMap.getCounter(arg.functionId);
            map.set(arg.functionId, currentTC);
            timesCalled = currentTC;
        }
        return {toReplace: arg, replaceBy: arg.update(timesCalled)};
    });
}

export default class FunctionSummaryHandler {
    // Maps all functions (identified by their arrival serial) to their corresponding summaries
    private _summaries = new Map();
    private _globalBacktrackingFlag = true;
    // { name: String, serial: Number }
    private _firstFunctionCalled = null;
    // Concrete call information: Stack[ConcreteFrame]
    private _concreteStack = new Stack();
    // Stack of *call* sites, instead of arrival sites (as used in the concrete stack)
    // Stack[ContextFrame]
    private _contextStack = new Stack();
    // Stack of locations of calls to user-defined functions: Stack[{f: Function, serial: Number, start: AranNode.start, end: AranNode.end}]
    private _lastCallSites = [];
    private _timesCalledMap = new CounterMap();
    private _currentFunctionSummariesStack = new Stack();

    constructor(private _backendCommunicator, private _testRunner) {
    }

    reset() {
        this._timesCalledMap = new CounterMap();
        this._concreteStack = new Stack();
    }

    getConcreteStack() {
        return this._concreteStack;
    }

    getContextStack() {
        return this._contextStack;
    }

    getTimesCalledMap() {
        return this._timesCalledMap;
    }

    isBacktracking() {
        return this._globalBacktrackingFlag;
    }

    _getFunctionSummary(processId, f, functionArgsArray, arrivalSerial) {
        let summary = this._summaries.get(arrivalSerial);
        if (!summary) {
            summary = new FunctionSummary(processId, f.name, functionArgsArray, arrivalSerial);
            this._summaries.set(arrivalSerial, summary);
        }
        return summary;
    }

    _deduplicateContextStack() {
        const frames: any[] = [];
        const newContextStack = new Stack();
        this._contextStack.bottomToTopForEach((contextFrame) => {
            if (!frames.find((existingFrame) => contextFrame.equals(existingFrame))) {
                frames.push(contextFrame);
                newContextStack.push(contextFrame);
            }
        })
        this._contextStack = newContextStack;
        jsPrint("After deduplication: _contextStack:", this._contextStack);
    }

    functionSummarised() {
        this._deduplicateContextStack();
        // Don't pop the topmost frame here, do that when generating input for the next test iteration
        jsPrint(Chalk.blue("Function summarised"));
    }

    userParameterEncountered($$argValue, idx, process, serial) {
        if (this._testRunner.isStopping()) {
            return $$argValue;
        }
        const concreteFrame = this.getCurrentConcreteFrame();
        const correspondingArgs = concreteFrame.getFunctionSummary().getOwnArgs();
        const correspondingArg = correspondingArgs[idx];
        $$argValue.symbolic = correspondingArg;
        return $$argValue;
    }

    getCurrentConcreteFrame() {
        return this._concreteStack.top();
    }

    getCurrentContextFrame() {
        return this._contextStack.top();
    }

    _getAllArgs() {
        const previousConcreteFrames = this.getConcreteStack();
        const allArgsArray: any[] = [];
        const allCorrespondingSymbolicInputs: any[] = [];
        previousConcreteFrames.bottomToTopForEach(function (concreteFrame) {
            allArgsArray.push(...concreteFrame.getArgsArray());
            allCorrespondingSymbolicInputs.push(...concreteFrame.getFunctionSummary().getOwnArgs());
        });
        return {allArgsArray, correspondingSymbolicInputs: allCorrespondingSymbolicInputs};
    }


    arrivedInFunction(process, f, argumentsValue, serial, aranNodes) {
        function copyArgsArray(argsArray) {
            const copiedArray: any[] = [];
            for (let arg of argsArray) {
                copiedArray.push({base: arg.base, symbolic: arg.symbolic});
            }
            return copiedArray;
        }

        if (this._testRunner.isStopping()) {
            return;
        }
        const argsArray = Array.prototype.slice.call(argumentsValue); // argumentsValue is not actually an array, so convert it to one first
        const summary = this._getFunctionSummary(process.processId, f, argsArray, serial);
        const that = this;
        let localBacktrackingFlag;
        if (summary.satisfiesAnyInputs(this._backendCommunicator, argsArray)) {

            const allArgsToReplace = summary.getAllArgs();
            const substitutes = createSubstitutes(allArgsToReplace, this._timesCalledMap);

            // Follow function summary: do not collect symbolic constraints
            this._globalBacktrackingFlag = false;
            localBacktrackingFlag = true;

            const copiedArray = copyArgsArray(argsArray);

            const fsWrapped = new FunctionSummaryWrapper(summary, copiedArray, substitutes);
            const fixedConstraint = new FixedConstraint(process.processId, fsWrapped);
            fixedConstraint._comesFrom = "arrivedInFunction";
            const funcSummaryGTS = <FunctionSummariesGTS>GTS.globalTesterState;
            funcSummaryGTS.addConditionWithSummary(fixedConstraint, process.alias, fsWrapped);
        } else {
            this._timesCalledMap.incCounter(serial)
            // Function summary incomplete: collect symbolic constraints + gather path summary
            this._globalBacktrackingFlag = true;
            localBacktrackingFlag = false;
        }
        const concreteFrame = new ConcreteFrame(serial, copyArgsArray(argsArray), localBacktrackingFlag, GTS.globalTesterState!.getGlobalPathConstraint(), summary);
        this._concreteStack.push(concreteFrame);
        if (!localBacktrackingFlag) {
            const {allArgsArray, correspondingSymbolicInputs} = this._getAllArgs();
            const currentSymbolicConcreteInputs = GTS.globalTesterState!.getSymbolicInputs().map((tuple) => tuple.input);
            const newConcreteFrame = new ContextFrame(serial, currentSymbolicConcreteInputs, summary, (<FunctionSummariesGTS>GTS.globalTesterState!).getPrefixPathConstraint(), allArgsArray, correspondingSymbolicInputs);
            this._contextStack.push(newConcreteFrame);
        }
        (<FunctionSummariesGTS>GTS.globalTesterState!).enterUserFunction();

    }

    callFunction(f, aranNode) {
        if (this._testRunner.isStopping()) {
            return;
        }
    }

    returned(process, $$consumed) {
        if (this._testRunner.isStopping()) {
            return $$consumed;
        }
        const concreteFrame = this._concreteStack.pop();
        const timesCalled = this._timesCalledMap.getCounter(concreteFrame.getFunctionId());
        const {
            returnVariable,
            comparison
        } = generateReturnValueConstrained(process.processId, concreteFrame.getFunctionId(), $$consumed.symbolic, timesCalled);
        const {lastFrame, lastFrameFS} = (<FunctionSummariesGTS>GTS.globalTesterState!).leaveUserFunction();
        const functionSummary = concreteFrame.getFunctionSummary();
        functionSummary.addArg(returnVariable);
        if (concreteFrame.getLocalBacktrackingFlag()) {
            this._globalBacktrackingFlag = true;
        } else {
            functionSummary.addPathSummary(concreteFrame.getPreConditions(), lastFrame, comparison);
            const lastFrameConverted = lastFrame!.map((constraint) => (<BranchConstraint> constraint).toBranchConstraint());
            this._testRunner.stopExploringFunction(concreteFrame, lastFrameConverted);
        }
        const $$result = tainter.taintAndCapture(tainter.cleanAndRelease($$consumed), returnVariable);
        return $$result;
    }

    getFirstFunctionFunctionCall() {
        return this._firstFunctionCalled;
    }

    getLastFunctionCall() {
        const lastCall = this._currentFunctionSummariesStack.top();
        return {name: lastCall.getFunctionName(), serial: lastCall.getCallSerial()};
    }
}

