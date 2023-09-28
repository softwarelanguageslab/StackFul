/**
 * The test tracer defines test runs for a few known application/mode combinations and asserts that the test run is
 * identical to previous test runs. It does so by exporting test* functions that are called by the tester during test
 * executions.
 */

import {exit} from "process";

import ConfigReader from "../../config/user_argument_parsing/Config";
import ExitCodes from "@src/util/exit_codes";
import assert from "@src/tester/TestTracer/assert";
import {
    DEBUGGING_TYPE,
    ERROR_DISCOVERED_TYPE,
    EVENTS_TYPE,
    INPUTS_TYPE,
    METRICS_TYPE, NEW_EVENT_TYPE, NEW_ITERATION_TYPE, STATE_DEQUEUED_TYPE, STOPPED_TYPE, VARIABLE_READ_TYPE
} from "@src/tester/TestTracer/AssertionClasses";

export default class TestTracer {
    protected readonly VARIABLE_READ_PREFIX = "__test__";

    public _items: any[] = []; // TODO
    protected _counter = 0;

    get counter(): number {
        return this._counter;
    }

    auxTest(itemType, ...actualInputs) {
        if (ConfigReader.config.ENABLE_TESTING) {
            if(this._items.length == 0) {
                exit(0) //exit the process, since there are no more items
            }
            const item = this._items.shift();
            assert(item.getType() === itemType, "Item type does not match. Expected item: " + item.getType() + ", actual item: " + itemType);
            item.isSame(...actualInputs);
            this._counter++;
        }
    }

    testDebugging(debugging) {
        this.auxTest(DEBUGGING_TYPE, debugging);
    }

    testErrorDiscovered(errorMessage) {
        this.auxTest(ERROR_DISCOVERED_TYPE, errorMessage);
    }

    testEvents(events) {
        this.auxTest(EVENTS_TYPE, events);
    }

    testGlobalInputs(inputs) {
        this.auxTest(INPUTS_TYPE, inputs);
    }

    testMetrics(metrics) {
        this.auxTest(METRICS_TYPE, metrics);
    }

    testNewEventDiscovered(event) {
        this.auxTest(NEW_EVENT_TYPE, event);
    }

    testNewIteration() {
        this.auxTest(NEW_ITERATION_TYPE);
    }

    testStateDequeued(state, priority) {
        this.auxTest(STATE_DEQUEUED_TYPE, state, priority);
    }

    testStopped(exitCode: ExitCodes) {
        this.auxTest(STOPPED_TYPE, exitCode);
    }

    protected shouldCheckVariable(variable): boolean {
        return variable.startsWith(this.VARIABLE_READ_PREFIX);
    }

    testVariableRead(variable, innerValue) {
        if (this.shouldCheckVariable(variable)) {
            this.auxTest(VARIABLE_READ_TYPE, variable, innerValue);
        }
    }
}
