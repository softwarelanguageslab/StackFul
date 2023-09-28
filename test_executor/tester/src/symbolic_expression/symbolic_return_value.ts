import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicReturnValue extends SymbolicExpression {
    private processId: any;
    private functionId: any;
    private symbolicValue: any;
    private timesCalled: any;

    constructor(processId, functionId, symbolicValue, timesCalled) {
        super("SymbolicReturnValue");
        this.processId = processId;
        this.functionId = functionId;
        this.symbolicValue = symbolicValue;
        this.timesCalled = timesCalled;
    }

    toString() {
        return `SymbolicReturnValue(${this.processId}, called: ${this.timesCalled}, functionId: ${this.functionId}, symbolicValue: ${this.symbolicValue}, timesCalled ${this.timesCalled})`;
    }

    usesValue(checkFunction) {
        return this.symbolicValue.usesValue(checkFunction);
    }

    copy() {
        return new SymbolicReturnValue(this.processId, this.functionId, this.symbolicValue, this.timesCalled);
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicReturnValue) && this.functionId === other.functionId && this.timesCalled === other.timesCalled && this.processId === other.processId;
    }

    instantiate(substitutes) {
        for (let substitute of substitutes) {
            if (this.isSameSymValue(substitute.toReplace)) {
                return substitute.replaceBy.copy();
            }
        }
        return this.copy();
    }

    update(newTimesCalled) {
        return new SymbolicReturnValue(this.processId, this.functionId, this.symbolicValue, newTimesCalled);
    }
}