import {SymbolicExpression} from "./symbolic_expressions";

export default class Nothing extends SymbolicExpression {
    constructor() {
        super("Nothing");
    }

    toString() {
        return "Nothing";
    }

    isSameSymValue(other) {
        return false;
    }

    usesValue(checkFunction) {
        return false;
    }

    instantiate(ignored) {
        return this;
    }

    update(ignored) {
        return new Nothing();
    }
}

export function isNothing(value) {
    return (typeof (value) === "object") && ((value.type === "Nothing") || (value instanceof Nothing));
}