import {isNothing} from "@src/symbolic_expression/nothing";
import assert from "@src/tester/TestTracer/assert";

export const DEBUGGING_TYPE = "debugging";
export const ERROR_DISCOVERED_TYPE = "error_discovered";
export const EVENTS_TYPE = "events";
export const FIRING_EVENTS_TYPE = "firing_events";
export const INPUTS_TYPE = "inputs";
export const METRICS_TYPE = "metrics";
export const NEW_EVENT_TYPE = "new_event";
export const NEW_ITERATION_TYPE = "new_iteration";
export const STOPPED_TYPE = "stopped_tester";
export const STATE_DEQUEUED_TYPE = "state_dequeued";
export const VARIABLE_READ_TYPE = "variable_read";

export class Inputs {
    constructor(private _type) {
    }

    getType() {
        return this._type;
    }
}

export class DebuggingTest extends Inputs {
    constructor(private _expectedMessage) {
        super(DEBUGGING_TYPE);
    }

    isSame(actualMessage) {
        assert(this._expectedMessage === actualMessage, "Messages differ. Expected: " + this._expectedMessage + ", actual: " + actualMessage);
    }
}


export class ErrorDiscoveredTest extends Inputs {
    constructor(private _errorMessage) {
        super(ERROR_DISCOVERED_TYPE);
    }

    isSame(actualMessage) {
        const expectedMessage = this._errorMessage;
        assert(expectedMessage === actualMessage, "Messages differ. Expected: " + expectedMessage + ", actual: " + actualMessage);
    }
}


export class EventsTest extends Inputs {
    constructor(private _events) {
        super(EVENTS_TYPE);
    }

    isSame(events) {
        assert(this._events.length === events.length, "Number of events differ. Expected: " + this._events.length + ", actual: " + events.length);
        for (let i in this._events) {
            const expectedEvent = this._events[i];
            const actualEvent = events[i];
            assertTargetsSame(expectedEvent, actualEvent);
        }
    }
}


function assertTargetsSame(expectedEvent, actualEvent) {
    assert(expectedEvent.type === actualEvent.type, "Event types differ. Expected: " + expectedEvent.type + ", actual: " + actualEvent.type);
    assert(expectedEvent.processId === actualEvent.processId, "Event processIds differ. Expected: " + expectedEvent.processId + ", actual: " + actualEvent.processId);
    assert(expectedEvent.targetId === actualEvent.targetId, "Event targetIds differ. Expected: " + expectedEvent.targetId + ", actual: " + actualEvent.targetId);
    assert(expectedEvent.specificEventType === actualEvent.specificEventType, "Event specificEventTypes differ. Expected: " + expectedEvent.specificEventType + ", actual: " + actualEvent.specificEventType);
}

export class FiringEventsTest extends Inputs {
    constructor(private _expectedEventFired) {
        super(FIRING_EVENTS_TYPE);
    }

    isSame(actualEvent) {
        assertTargetsSame(this._expectedEventFired, actualEvent)
    }
}


export class InputsTest extends Inputs {
    constructor(private _inputs) {
        super(INPUTS_TYPE);
    }

    isSame(inputs) {
        assert(this._inputs.length === inputs.length, "lengths should match");
        for (let i in this._inputs) {
            const expectedInput = this._inputs[i];
            const actualInput = inputs[i];
            assert(expectedInput.type === actualInput.type, "Input types differ. Expected: " + expectedInput.type + ", actual: " + actualInput.type);
            assert(expectedInput.id === actualInput.id, "Input ids differ. Expected: " + expectedInput.id + ", actual: " + actualInput.id);
            assert(expectedInput.processId === actualInput.processId, "Input processIds differ. Expected: " + expectedInput.processId + ", actual: " + actualInput.processId);
            assert(expectedInput.value === actualInput.value, "Input values differ. Expected: " + expectedInput.value + ", actual: " + actualInput.value);
        }
    }
}


export class MetricsTest extends Inputs {
    constructor(private _expectedMetrics) {
        super(METRICS_TYPE);
    }

    isSame(actualMetrics) {
        assert(this._expectedMetrics.linesCovered === actualMetrics.linesCovered, "Metrics linesCovered differ. Expected: " + this._expectedMetrics.linesCovered + ", actual: " + actualMetrics.linesCovered);
        assert(this._expectedMetrics.linesCoveredPerProcess === actualMetrics.linesCoveredPerProcess, "Metrics linesCoveredPerProcess differ. Expected: " + this._expectedMetrics.linesCoveredPerProcess + ", actual: " + actualMetrics.linesCoveredPerProcess);
        assert(this._expectedMetrics.nrOfErrors === actualMetrics.nrOfErrors, "Metrics nrOfErrors differ. Expected: " + this._expectedMetrics.nrOfErrors + ", actual: " + actualMetrics.nrOfErrors);
    }
}


export class NewEventDiscoveredTest extends Inputs {
    constructor(private _event) {
        super(NEW_EVENT_TYPE);
    }

    isSame(actualEvent) {
        const expectedEvent = this._event;
        assertTargetsSame(expectedEvent, actualEvent);
    }
}


export class NewIterationType extends Inputs {
    constructor() {
        super(NEW_ITERATION_TYPE);
    }

    isSame(ignored) {
        return true;
    }
}


export class StateDequeuedTest extends Inputs {
    constructor(private _state, private _priority) {
        super(STATE_DEQUEUED_TYPE);
    }

    isSame(actualState, actualPriority) {
        const expectedState = this._state;
        const expectedPriority = this._priority;
        assert(expectedPriority.equalTo(actualPriority), "State priorities differ. Expected: " + expectedPriority + ", actual: " + actualPriority);
        assert(expectedState.getBranchSequence() === actualState.getBranchSequence(), "State branch-sequences differ. Expected: " + expectedState.getBranchSequence() + ", actual: " + actualState.getBranchSequence());
        const expectedEvents = expectedState.getEventSequence();
        const actualEvents = actualState.getEventSequence();
        assert(expectedEvents.length === actualEvents.length, "State event-sequences' length differ. Expected: " + expectedEvents.length + ", actual: " + actualEvents.length);
        for (let i in expectedEvents) {
            assertTargetsSame(expectedEvents[i], actualEvents[i]);
        }
    }
}


export class VariableReadTest extends Inputs {
    constructor(private _variable, private _innerValue) {
        super(VARIABLE_READ_TYPE);
    }

    isSame(actualVariable, actualInnerValue) {
        const expectedVariable = this._variable;
        const expectedInnerValue = this._innerValue;
        assert(expectedVariable === actualVariable, "Variable names differ. Expected: " + expectedVariable + ", actual: " + actualVariable);
        if (!expectedInnerValue.symbolic) {
            assert(expectedInnerValue.symbolic === actualInnerValue.symbolic, "Expected symbolic value to be undefined, but is " + actualInnerValue.symbolic);
        } else if (isNothing(expectedInnerValue.symbolic)) {
            assert(isNothing(actualInnerValue.symbolic), "Expected symbolic value to be Nothing, but is " + actualInnerValue.symbolic);
        } else {
            assert(expectedInnerValue.symbolic.isSameSymValue(actualInnerValue.symbolic), "Symbolic values differ. Expected: " + expectedInnerValue.symbolic + ", actual: " + actualInnerValue.symbolic);
        }
    }
}