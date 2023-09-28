import * as Operators from "./operators";
import * as SymTypes from "./supported_symbolic_types";
import SymbolicInput from "@src/symbolic_expression/SymbolicInput";
import {ExecutionState} from "@src/tester/ExecutionState.js";

export default class SymbolicInputString extends SymbolicInput {

    constructor(processId, executionState: ExecutionState, id: number) {
        super("SymbolicInputString", processId, executionState, id, Operators.StringEqual);
    }

    toString() {
        return `SymbolicInputString(processId ${this.processId}, executionState ${this.executionState}, id ${this.id})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicInputString) &&
               this.processId === other.processId &&
               this.executionState === other.executionState;
    }

    toSymbolicConcreteValue(concreteValue) {
        return {
            type: SymTypes.SupportedSymbolicExpressionTypes.String,
            value: concreteValue,
            processId: this.processId,
            executionState: this.executionState
        };
    }

    instantiate(ignored) {
        return new SymbolicInputString(this.processId, this.executionState, this.id);
    }
}