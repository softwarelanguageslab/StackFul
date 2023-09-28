import {
    SupportedSymbolicExpressionType,
    SupportedSymbolicExpressionTypes
} from "@src/symbolic_expression/supported_symbolic_types";
import {ExecutionState} from "@src/tester/ExecutionState.js";

export default interface ComputedInput {
    type: SupportedSymbolicExpressionTypes;
    value: SupportedSymbolicExpressionType;
    processId: number;
    executionState: ExecutionState;
    id: number;
}