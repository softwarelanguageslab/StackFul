import {GlobalTesterState} from "./global_tester_state";

export class VerifyIntraGTS extends GlobalTesterState {
    private readonly _messageInputs: {};
    private readonly _messageOutputs: {};

    constructor(processes, events, globalInputs, stopIncomingMessages) {
        super(processes, events, globalInputs);
        this._messageInputs = {};
        this._messageOutputs = {};
    }

    private _incMapCounter(map, messageType, processId) {
        let processMap = map[processId];
        if (processMap === undefined) {
            const newMap = {};
            map[processId] = newMap;
            processMap = newMap;
        }
        let counter = processMap[messageType];
        if (counter === undefined) {
            counter = 0;
        }
        processMap[messageType] = counter + 1;
        return counter;
    }

    newMessageInput(messageType, processId) {
        return this._incMapCounter(this._messageInputs, messageType, processId);
    }

    newMessageOutput(messageType, processId?) {
        return this._incMapCounter(this._messageOutputs, messageType, processId);
    }
}