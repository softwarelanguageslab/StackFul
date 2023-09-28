import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicUnaryExp extends SymbolicExpression {
    public operator: any;
    private argument: any;

    constructor(operator, argument) {
        super("SymbolicUnaryExp");
        this.operator = operator;
        this.argument = argument;
    }

    toString() {
        return this.operator + this.argument.toString();
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicUnaryExp) && this.operator === other.operator && this.argument.isSameSymValue(other.argument);
    }

    usesValue(checkFunction) {
        return checkFunction(this.argument);
    }

    instantiate(substitutes) {
        return new SymbolicUnaryExp(this.operator, this.argument.instantiate(substitutes));
    }
}