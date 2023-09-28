import noIdentifier from "../instrumentation/NoIdentifier"

export abstract class SymbolicExpression {
    protected _identifier: string;

    protected constructor(public type: string) {
        this._identifier = noIdentifier;
    }

    public usesValue(checkFunction) {
        return false
    }

    get getIdentifier() {
        return this._identifier;
    }
    public setIdentifier(identifier: string): SymbolicExpression {
        const copy = Object.assign(Object.create(Object.getPrototypeOf(this)), this);
        copy._identifier = identifier;
        return copy;
    }

}
