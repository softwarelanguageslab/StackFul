import {Logger as Log} from "@src/util/logging";
import Event from "@src/tester/Event.js";

export default class SymJSState {
    public static readonly emptyStartState: SymJSState = new SymJSState([], "");

    constructor(private readonly _eventSequence: Event[], private readonly _branchSequence: string | undefined) {
        Log.SCR("Created new ", this.toString());
    }

    toString(): string {
        return `SymJSState([${this._eventSequence.map((event) => event.toString()).join(", ")}], ${this._branchSequence})`;
    }

    equalsState(otherState: SymJSState): boolean {
        const evSeq1 = this.getEventSequence();
        const evSeq2 = otherState.getEventSequence();
        const result = this.getBranchSequence() === otherState.getBranchSequence() &&
            evSeq1.length === evSeq2.length &&
            evSeq1.every((e1, i) => e1.equals(evSeq2[i]));
        return result;
    }

    getEventSequence(): Event[] {
        return this._eventSequence;
    }

    getBranchSequence(): string | undefined {
        return this._branchSequence;
    }

}