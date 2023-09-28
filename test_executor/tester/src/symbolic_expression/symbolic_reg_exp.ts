import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicRegExp extends SymbolicExpression {
    private readonly e: any;

    constructor(e) {
        super("SymbolicRegExp");
        this.e = e;
    }

    toString() {
        return `SymbolicRegExp(${this.e})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicRegExp) && this.e === other.e;
    }

    instantiate(ignored) {
        return new SymbolicRegExp(this.e);
    }
}