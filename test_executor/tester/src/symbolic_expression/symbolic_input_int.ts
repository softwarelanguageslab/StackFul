import * as Operators from "./operators";
import * as SymTypes from "./supported_symbolic_types"
import SymbolicInput from "@src/symbolic_expression/SymbolicInput";
import {ExecutionState} from "@src/tester/ExecutionState.js";

export default class SymbolicInputInt extends SymbolicInput {

    constructor(processId, executionState: ExecutionState, id: number) {
        super("SymbolicInputInt", processId, executionState, id, Operators.IntEqual);
    }

    toString() {
        return `SymbolicInputInt(processId ${this.processId}, executionState ${this.executionState}, id ${this.id})`;
    }

    toSymbolicConcreteValue(concreteValue) {
        return {
            type: SymTypes.SupportedSymbolicExpressionTypes.Int,
            value: concreteValue,
            processId: this.processId,
            executionState: this.executionState
        };
    }

    instantiate(ignored) {
        return new SymbolicInputInt(this.processId, this.executionState, this.id);
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicInputInt) &&
                this.processId === other.processId &&
                this.executionState === other.executionState;
    }
}