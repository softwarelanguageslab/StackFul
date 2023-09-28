import {jsPrint} from "../util/io_operations";
import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicRelationalExp extends SymbolicExpression {
    private left: any;
    public operator: any;
    private right: any;

    constructor(left, operator, right) {
        super("SymbolicRelationalExp");
        this.left = left;
        this.operator = operator;
        this.right = right;
        if (this.left === undefined || this.right === undefined) {
            jsPrint("one of either children is undefined, this =", this);
            throw new Error("One of either children is undefined");
        }
    }

    toString() {
        return `(${this.left.toString()} ${this.operator} ${this.right.toString()})`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicRelationalExp) && this.operator === other.operator &&
            this.left.isSameSymValue(other.left) && this.right.isSameSymValue(other.right);
    }

    usesValue(checkFunction) {
        return checkFunction(this.left) || checkFunction(this.right);
    }

    instantiate(substitutes) {
        if (!(this.left.instantiate instanceof Function)) {
            jsPrint("left child does not have an instantiate-method, left =", this.left);
        }
        if (!(this.right.instantiate instanceof Function)) {
            jsPrint("right child does not have an instantiate-method, right =", this.right);
        }
        return new SymbolicRelationalExp(this.left.instantiate(substitutes), this.operator, this.right.instantiate(substitutes));
    }
}