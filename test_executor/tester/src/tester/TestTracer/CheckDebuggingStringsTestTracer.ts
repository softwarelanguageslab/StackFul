import assert from "./assert"
import ExitCodes from "@src/util/exit_codes";
import TestTracer from "./TestTracer";
import {jsPrint} from "@src/util/io_operations";

export default class CheckDebuggingStringsTestTracer extends TestTracer {
    protected _expectedStringsCounted: Map<string, number>;
    protected _currentIteration: number;
    constructor(protected _expectedStrings: Set<string>,
                protected _stringsNotExpected: Set<string>,
                protected readonly _expectedIterations: number) {
        super();
        this._expectedStringsCounted = new Map<string, number>;
        _expectedStrings.forEach(string => this._expectedStringsCounted.set(string, 0));
        this._currentIteration = 0;
    }

    auxTest(itemType, ...actualInputs) { }

    testDebugging(debugging) {
        this._stringsNotExpected.forEach(string => jsPrint("an unexpected string", string));
        assert(! this._stringsNotExpected.has(debugging), `Unexpected string ${debugging}`);
        if (this._expectedStrings.has(debugging)) {
            const optOldNumber: number | undefined = this._expectedStringsCounted.get(debugging);
            const newNumber: number = optOldNumber === undefined ? 1 : optOldNumber + 1;
            this._expectedStringsCounted.set(debugging, newNumber);
        }
    }

    testNewIteration() {
        this._currentIteration++;
    }

    protected checkAllExpectdStringsEncountered(): void {
        this._expectedStringsCounted.forEach((counter: number, string: string) => {
            assert(counter > 0, `Expected string ${string} was not encountered`);
        });
    }

    testStopped(exitCode: ExitCodes) {
        const iterationMessage =
            `Expected number of iterations (${this._expectedIterations}) ` +
            `does not match actual number of iterations (${this._currentIteration})`;
        assert(this._currentIteration === this._expectedIterations, iterationMessage);
        const exitMessage =
            `Expected exit code (${ExitCodes.success}) ` +
            `does not match actual number exit code (${exitCode})`;
        assert(exitCode === ExitCodes.success, exitMessage);
        this.checkAllExpectdStringsEncountered();
    }
}

const merging_experiments1Expected: string[] = [
    "DEBUGGING EXP1 1.T", "DEBUGGING EXP1 1.F",
    "DEBUGGING EXP1 2.T", "DEBUGGING EXP1 2.F",
    "DEBUGGING EXP1 3.T", "DEBUGGING EXP1 3.F"
];
export const merging_experiments1TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments1Expected), new Set(), 4);

const merging_experiments2Expected: string[] = [
    "DEBUGGING EXP2 1.T", "DEBUGGING EXP2 1.F",
    "DEBUGGING EXP2 2.T", "DEBUGGING EXP2 2.F"
];
export const merging_experiments2TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments2Expected), new Set(), 3);

const merging_experiments3Expected: string[] = [
    "DEBUGGING EXP3 1.T", "DEBUGGING EXP3 1.F",
    "DEBUGGING EXP3 2.T", "DEBUGGING EXP3 2.F",
    "DEBUGGING EXP3 3.T", "DEBUGGING EXP3 3.F"
];
export const merging_experiments3TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments3Expected), new Set(), 3);

const merging_experiments4Expected: string[] = [
    "DEBUGGING EXP4 1.T", "DEBUGGING EXP4 1.F",
    "DEBUGGING EXP4 2.T", "DEBUGGING EXP4 2.F",
    "DEBUGGING EXP4 3.T", "DEBUGGING EXP4 3.F"
];
export const merging_experiments4TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments4Expected), new Set(), 3);

const merging_experiments5Expected: string[] = [
    "DEBUGGING EXP5 1.T", "DEBUGGING EXP5 1.F",
    "DEBUGGING EXP5 2.T", "DEBUGGING EXP5 2.F",
    "DEBUGGING EXP5 3.T", "DEBUGGING EXP5 3.F",
    "DEBUGGING EXP5 4.T", "DEBUGGING EXP5 4.F",
    "DEBUGGING EXP5 5.T", "DEBUGGING EXP5 5.F"
];
export const merging_experiments5TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments5Expected), new Set(), 6);

const merging_experiments6Expected: string[] = [
    "DEBUGGING EXP6 1.T", "DEBUGGING EXP6 1.F",
    "DEBUGGING EXP6 2.T", "DEBUGGING EXP6 2.F",
    "DEBUGGING EXP6 3.T", "DEBUGGING EXP6 3.F",
    "DEBUGGING EXP6 4.T", "DEBUGGING EXP6 4.F"
];
export const merging_experiments6TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments6Expected), new Set(), 5);

const merging_experiments7Expected: string[] = [
    "DEBUGGING EXP7 1.T", "DEBUGGING EXP7 1.F",
    "DEBUGGING EXP7 2.T", "DEBUGGING EXP7 2.F",
    "DEBUGGING EXP7 3.F"
];
export const merging_experiments7TT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments7Expected),
                                        new Set(["DEBUGGING EXP7 3.T"]),
                        3);

export const merging_experiments1_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments1Expected),
                                        new Set(),
                                        8);
export const merging_experiments2_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments2Expected),
        new Set(),
        4);
export const merging_experiments3_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments3Expected),
        new Set(),
        4);
export const merging_experiments4_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments4Expected),
        new Set(),
        4);
export const merging_experiments5_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments5Expected),
        new Set(),
        32);
export const merging_experiments6_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments6Expected),
        new Set(),
        8);
export const merging_experiments7_no_mergingTT =
    new CheckDebuggingStringsTestTracer(new Set(merging_experiments7Expected),
        new Set(),
        4);