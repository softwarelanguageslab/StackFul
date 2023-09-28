import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicBool extends SymbolicExpression {
    private b: boolean;

    constructor(b: boolean) {
        super("SymbolicBool");
        this.b = b;
    }

    toString() {
        return `SymbolicBool(${this.b})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicBool) && this.b === other.b;
    }

    instantiate(ignored) {
        return new SymbolicBool(this.b);
    }
}