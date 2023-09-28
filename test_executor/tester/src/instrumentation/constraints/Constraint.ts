export default class Constraint {
    private _processId: any;
    private _type: any;

    constructor(processId, type) {
        this._processId = processId;
        this._type = type;
    }

    get processId() {
        return this._processId;
    }

    get type() {
        return this._type;
    }
}