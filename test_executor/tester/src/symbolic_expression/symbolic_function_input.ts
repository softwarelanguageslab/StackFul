import * as SymTypes from "./supported_symbolic_types";
import * as Operators from "./operators";
import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicFunctionInput extends SymbolicExpression {
    private processId: any;
    private functionId: any;
    private id: any;
    private inputType: any;
    private timesCalled: number;
    private equalsToOperator: any;

    constructor(processId, functionId, id, timesCalled, inputType) {
        super("SymbolicFunctionInput");
        this.processId = processId;
        this.functionId = functionId;
        this.id = id;
        this.timesCalled = timesCalled;
        this.inputType = inputType;
        if (this.timesCalled === 0) {
            throw new Error("timesCalled may not be 0");
        }
        let equalsToOperator;
        switch (inputType) {
            case SymTypes.SupportedSymbolicExpressionTypes.Int:
                equalsToOperator = Operators.IntEqual;
                break;
            case SymTypes.SupportedSymbolicExpressionTypes.String:
                equalsToOperator = Operators.StringEqual;
                break;
        }
        this.equalsToOperator = equalsToOperator;
    }

    toString() {
        return `SymbolicFunctionInput(processId ${this.processId}, functionId ${this.functionId}, id ${this.id}, timesCalled ${this.timesCalled}, type ${this.inputType})`;
    }

    copy() {
        return new SymbolicFunctionInput(this.processId, this.functionId, this.id, this.timesCalled, this.inputType);
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicFunctionInput) && this.timesCalled === other.timesCalled && this.functionId === other.functionId && this.id === other.id && this.processId === other.processId;
    }

    toSymbolicConcreteValue(concreteValue) {
        return {
            type: SymTypes.SupportedSymbolicExpressionTypes.Int,
            value: concreteValue,
            functionId: this.functionId,
            processId: this.processId,
            id: this.id
        };
    }

    update(newTimesCalled) {
        return new SymbolicFunctionInput(this.processId, this.functionId, this.id, newTimesCalled, this.inputType);
    }

    instantiate(substitutes) {
        for (let substitute of substitutes) {
            if (this.isSameSymValue(substitute.toReplace)) {
                return substitute.replaceBy.copy();
            }
        }
        return this.copy();
    }

}