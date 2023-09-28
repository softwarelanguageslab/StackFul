import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicString extends SymbolicExpression {
    private s: any;

    constructor(s) {
        super("SymbolicString");
        this.s = s;
    }

    toString() {
        return `SymbolicString(${this.s})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicString) && this.s === other.s;
    }

    instantiate(ignored) {
        return new SymbolicString(this.s);
    }
}