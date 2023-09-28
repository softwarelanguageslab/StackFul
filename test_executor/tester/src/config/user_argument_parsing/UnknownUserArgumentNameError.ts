export default class UnknownUserArgumentNameError extends Error {
    constructor(private argumentName: string) {
        super();
    }
    toString(): string {
        return `Unknown user argument name ${this.argumentName}`;
    }
}