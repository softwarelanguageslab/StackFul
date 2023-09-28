import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicInt extends SymbolicExpression {
    private i: any;

    constructor(i) {
        super("SymbolicInt");
        this.i = i;
    }

    toString() {
        return `SymbolicInt(${this.i})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicInt) && this.i === other.i;
    }

    instantiate(ignored) {
        return new SymbolicInt(this.i);
    }
}