import BackendCommunication from "./BackendCommunication";
import iterationNumber from "@src/tester/iteration_number";
import {Logger as Log} from "@src/util/logging";
import * as SymTypes from "@src/symbolic_expression/supported_symbolic_types"
import SymbolicInputInt from "@src/symbolic_expression/symbolic_input_int";
import SymbolicInputString from "@src/symbolic_expression/symbolic_input_string";
import {
    BackendResult,
    BackendResultType,
    FunctionFullyExplored,
    InputsAndEventsReceived
} from "@src/backend/BackendResult";
import BackendResultState from "@src/backend/BackendResultState";
import ExecutionStateCollector from "@src/tester/ExecutionStateCollector.js";

export default class FunctionSummariesModeBackendCommunication extends BackendCommunication {
    constructor(inputPort, outputPort, solveProcessId) {
        super(inputPort, outputPort, solveProcessId);
    }

    _wrapArg($$arg) {
        return BackendCommunication.wrapConcreteSymbolic($$arg.base, $$arg.symbolic);
    }

    wrapMessageComputePrefix(prefixPathConstraint, allArgsArray, allCorrespondingSymbolicInputs) {
        const wrappedArgs = allArgsArray.map(this._wrapArg);
        const filteredWrappedArgs = BackendCommunication.filterWrappedArgs(wrappedArgs);
        const wrappedMessage = {
            backend_id: this._backendId,
            type: "function_summaries",
            "fs_type": "compute_prefix",
            iteration: iterationNumber.get(),
            prefix: prefixPathConstraint,
            "arguments": filteredWrappedArgs,
            "corresponding_inputs": allCorrespondingSymbolicInputs
        };
        return wrappedMessage;
    }

    wrapMessageExploreFunction(functionId, functionSummary, allArgsArray, prefixPathConstraint, suffixPathConstraint, allCorrespondingSymbolicInputs) {
        const wrappedArgs = allArgsArray.map(this._wrapArg);
        const filteredWrappedArgs = BackendCommunication.filterWrappedArgs(wrappedArgs);
        const wrappedMessage = {
            backend_id: this._backendId,
            type: "function_summaries",
            "fs_type": "explore_function",
            iteration: iterationNumber.get(),
            "arguments": filteredWrappedArgs,
            "function_id": functionId,
            "corresponding_inputs": allCorrespondingSymbolicInputs,
            prefix: prefixPathConstraint,
            suffix: suffixPathConstraint
        };
        return wrappedMessage;
    }

    handleBackendResult(parsedJSON): BackendResult {
        switch (parsedJSON.type) {
            case "SymbolicTreeFullyExplored":
                return {type: BackendResultType.Other, state: BackendResultState.FINISHED};
            case "NewInput":
                return {type: BackendResultType.InputsAndEventsReceived,
                        state: BackendResultState.SUCCESS,
                        inputs: parsedJSON.inputs,
                        events: []} as InputsAndEventsReceived;
            case "FunctionFullyExplored":
                return {type: BackendResultType.Other,
                        state: BackendResultState.FINISHED_EXPLORING_FUNCTION,
                        functionId: parsedJSON["function_id"]} as FunctionFullyExplored;
            default:
                Log.BCK(`Backend did not return a NewInput, but returned ${JSON.stringify(parsedJSON)}`);
                return {type: BackendResultType.Other, state: BackendResultState.INVALID};
        }
    }

    sendComputePrefix(prefixPathConstraint, allArgsArray, allCorrespondingSymbolicInputs) {
        const wrapped = this.wrapMessageComputePrefix(prefixPathConstraint, allArgsArray, allCorrespondingSymbolicInputs);
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);
    }

    sendExploreFunction(functionId, functionSummary, allArgsArray, prefixPathConstraint, suffixPathConstraint, allCorrespondingSymbolicInputs) {
        const instantiatedSummary = functionSummary.instantiate([], false);
        const instantiatedSuffixConstraint = suffixPathConstraint.map((constraint) => constraint.instantiateConstraint([]));
        const wrapped = this.wrapMessageExploreFunction(functionId, instantiatedSummary, allArgsArray, prefixPathConstraint, instantiatedSuffixConstraint, allCorrespondingSymbolicInputs);
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);
    }

    _createSymbolicInput(computedValue) {
        const executionState = ExecutionStateCollector.getExecutionState(computedValue.processId);
        switch (SymTypes.determineType(computedValue.value)) {
            case SymTypes.SupportedSymbolicExpressionTypes.Int:
                return new SymbolicInputInt(computedValue.processId, executionState, computedValue.id)
            case SymTypes.SupportedSymbolicExpressionTypes.String:
                return new SymbolicInputString(computedValue.processId, executionState, computedValue.id)
        }
    }

    checkSatisfiesConstraints(functionId, preCondition, pathConditions, args, correspondingSymbolicInputs, globalSymbolicInputs) {
        const that = this;

        function containsFreeVariable(inputs) {
            return inputs.some((input) => {
                return input.functionId === undefined;
            });
        }

        const wrappedArgs = args.map(this._wrapArg);
        const filteredWrappedArgs = BackendCommunication.filterWrappedArgs(wrappedArgs);
        const instantiatedPathConditions = pathConditions.map((constraint) => constraint.instantiateConstraint([]));
        const wrappedGlobalSymbolicInputs = globalSymbolicInputs.map((input) => {
            const symbolicInput = that._createSymbolicInput(input);
            return BackendCommunication.wrapConcreteSymbolic(input.value, symbolicInput);
        });
        const filteredGlobalSymbolicInputs = BackendCommunication.filterWrappedArgs(wrappedGlobalSymbolicInputs);

        const wrapped = {
            backend_id: this._backendId,
            "type": "function_summaries",
            "fs_type": "satisfies_formula",
            "iteration": iterationNumber.get(),
            "function_id": functionId,
            "arguments": filteredWrappedArgs,
            "pre_conditions": preCondition,
            "global_symbolic_inputs": filteredGlobalSymbolicInputs,
            "path_conditions": instantiatedPathConditions,
            "corresponding_inputs": correspondingSymbolicInputs
        };
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);

        function react(parsedJSON) {
            switch (parsedJSON.type) {
                case "NewInput":
                    return true;
                case "UnsatisfiablePath":
                    return false;
                default:
                    throw Error(`Expected result of type NewInput or UnsatisfiablePath, but got ${parsedJSON.type}`);
            }
        }

        return this.expectResult(react);
    }
}