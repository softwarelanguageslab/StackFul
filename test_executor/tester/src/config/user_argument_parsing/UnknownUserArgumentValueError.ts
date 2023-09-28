export default class UnknownUserArgumentValueError<T> extends Error {
    constructor(private argumentName: string, private argumentValue: T) {
        super();
    }
    toString(): string {
        return `Unknown user argument value ${this.argumentValue} for argument ${this.argumentName}`;
    }
}