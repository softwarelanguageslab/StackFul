import BackendCommunication from "./BackendCommunication";
import {Logger as Log} from "../util/logging";
import {BackendResult, BackendResultType, InputsAndEventsReceived} from "@src/backend/BackendResult";
import BackendResultState from "@src/backend/BackendResultState";
import ComputedInput from "@src/backend/ComputedInput";

export default class SolveModeBackendCommunication extends BackendCommunication {

    constructor(inputPort: number, outputPort: number, backendId: number) {
        super(inputPort, outputPort, backendId);
    }

    wrapMessage(globalPathConstraint, nextState) {
        const targetsTriggered = nextState.getEventSequence().map(function (UITarget) {
            return {processId: UITarget.processId, targetId: UITarget.targetId};
        });
        const wrappedMessage = {
            backend_id: this._backendId,
            type: "solve",
            PC: globalPathConstraint,
            event_seq: targetsTriggered,
            branch_seq: nextState.getBranchSequence()
        };
        return wrappedMessage;
    }

    handleBackendResult(parsedJSON): BackendResult {
        switch (parsedJSON.type) {
            case "SymbolicTreeFullyExplored":
                return {type: BackendResultType.Other, state: BackendResultState.FINISHED};
            case "NewInput":
                return {type: BackendResultType.InputsAndEventsReceived,
                        state: BackendResultState.SUCCESS,
                        inputs: parsedJSON.inputs,
                        events: parsedJSON.events} as InputsAndEventsReceived;
            default:
                Log.BCK(`Backend did not return a NewInput, but returned ${JSON.stringify(parsedJSON)}`);
                return {type: BackendResultType.Other, state: BackendResultState.INVALID};
        }
    }

    sendPathConstraintWithNextState(globalPathConstraint, nextState) {
        const wrapped = this.wrapMessage(globalPathConstraint, nextState);
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);
    }
}