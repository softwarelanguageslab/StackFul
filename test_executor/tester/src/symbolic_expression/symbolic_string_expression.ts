import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicStringExpression extends SymbolicExpression {
    // general case: operations that take 2 strings as parameters
    // --> different function for ops that take 1 or 3 string??
    private args: any;
    private operator: any;

    constructor(operator, args) {
        super("SymbolicStringOperationExp");
        this.args = args;
        this.operator = operator;
    }

    toString() {
        return `SymbolicStringExpression(${this.operator}, ${this.args.join()})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicStringExpression) && this.operator === other.operator &&
            this.args.length === other.args.length && this.args.every((arg, idx) => arg.isSameSymValue(other.args[idx]));
    }

    usesValue(checkFunction) {
        return this.args.some(checkFunction);
    }

    instantiate(substitutes) {
        const mappedArgs = this.args.map((arg) => arg.instantiate(substitutes));
        return new SymbolicStringExpression(this.operator, mappedArgs);
    }
}