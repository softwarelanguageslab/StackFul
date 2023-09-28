import BackendCommunication from "./BackendCommunication";
import {BackendResult, BackendResultType, InputsAndEventsReceived} from "@src/backend/BackendResult";
import Event from "@src/tester/Event";
import ComputedInput from "@src/backend/ComputedInput";
import BackendResultState from "@src/backend/BackendResultState";

export default class DummyBackendCommunication extends BackendCommunication {
    constructor(inputPort, outputPort, solveProcessId) {
        super(inputPort, outputPort, solveProcessId);
    }

    sendPathConstraint() { /* Do nothing */
    }

    receiveSolverResult(): BackendResult {
        const dummyInputs: ComputedInput[] = [];
        const dummyEvents: Event[] = [new Event(1, 0)];
        return {type: BackendResultType.InputsAndEventsReceived,
                state: BackendResultState.SUCCESS,
                inputs: dummyInputs,
                events: dummyEvents} as InputsAndEventsReceived;
    }

    handleBackendResult(parsedJSON): BackendResult {
        // Do nothing
        throw Error("Not implemented")
    }
}