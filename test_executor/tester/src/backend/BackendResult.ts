import BackendResultState from "@src/backend/BackendResultState";
import ComputedInput from "@src/backend/ComputedInput";
import Event from "@src/tester/Event";

export enum BackendResultType {
    InputsAndEventsReceived,
    PathReceived,
    InputsAndEventsAndPathReceived,
    Other
}

export interface BackendResult {
    type: BackendResultType;
    state: BackendResultState;
}
export interface InputsAndEventsReceived extends BackendResult {
    inputs: ComputedInput[];
    events: Event[];
}
export interface PathReceived extends BackendResult {
    path: any;
}
export interface InputAndEventsAndPathReceived extends InputsAndEventsReceived, PathReceived {}
export interface FunctionFullyExplored extends BackendResult {
    functionId: number
}