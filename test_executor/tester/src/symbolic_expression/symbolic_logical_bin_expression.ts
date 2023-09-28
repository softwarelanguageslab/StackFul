import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicLogicalBinExpression extends SymbolicExpression {
    private left: any;
    private operator: any;
    private right: any;

    constructor(left, operator, right) {
        super("SymbolicLogicalBinExpression");
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    toString() {
        return `(${this.left.toString()} ${this.operator} ${this.right.toString()})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicLogicalBinExpression) && this.operator === other.operator &&
            this.left.isSameSymValue(other.left) && this.right.isSameSymValue(other.right);
    }

    usesValue(checkFunction) {
        return checkFunction(this.left) || checkFunction(this.right);
    }

    instantiate(substitutes) {
        return new SymbolicLogicalBinExpression(this.left.instantiate(substitutes), this.operator, this.right.instantiate(substitutes));
    }
}