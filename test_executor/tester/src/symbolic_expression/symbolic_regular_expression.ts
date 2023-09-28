import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicRegularExpression extends SymbolicExpression {
    private _constructor: any;
    private args: any;
    private object: any;

    constructor(_constructor, args, object) {
        super("SymbolicRegularExpression");
        this._constructor = _constructor;
        this.args = args;
        this.object = object
    }

    toString() {
        if (this.object == "") {
            return this._constructor + " " + this.args.toString();
        } else {
            return this._constructor + " " + this.object.toString() + " " + this.args.toString();
        }
    }

    isSameSymValue(other) {
        return this._constructor === other._constructor && this.args.length === other.args.length && this.args.every((arg, idx) => arg.isSameSymValue(other.args[idx])) &&
            (this.object === other.object || this.object.isSameSymValue(other.object));
    }

    usesValue(checkFunction) {
        return this.args.some(checkFunction);
    }

    instantiate(substitutes) {
        const mappedArgs = this.args.map((arg) => arg.instantiate(substitutes));
        return new SymbolicRegularExpression(this._constructor, mappedArgs, this.object);
    }
}