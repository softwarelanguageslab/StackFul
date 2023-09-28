import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicArithmeticExp extends SymbolicExpression {
    private operator: any;
    private args: any;

    constructor(operator, args) {
        super("SymbolicArithmeticExp");
        this.operator = operator;
        this.args = args;
    }

    toString() {
        return "(" + this.args.map((arg) => arg.toString()).join(" " + this.operator + " ") + ")";
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicArithmeticExp) && this.operator === other.operator &&
            this.args.length === other.args.length && this.args.every((arg, idx) => arg.isSameSymValue(other.args[idx]));
    }

    usesValue(checkFunction) {
        return this.args.some(checkFunction);
    }

    instantiate(substitutes) {
        const mappedArgs = this.args.map((arg) => arg.instantiate(substitutes));
        return new SymbolicArithmeticExp(this.operator, mappedArgs);
    }
}