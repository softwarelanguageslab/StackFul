import {Logger as Log} from "@src/util/logging";
import { PriorityWithBaseHeuristic } from "./PriorityWithBaseHeuristic";
import Event from "@src/tester/Event.js";

export class PriorityEventHandlerFirst extends PriorityWithBaseHeuristic {
	constructor(protected _readWrite) {
        super(_readWrite);
    }
	// EventHandlerFirst - If the specific targetBranch wasn't yet executed or to be executed, set the priority to Infinity
	calculatePriority(eventSeq: Event[], event: any, executedOrToBeExecuted: boolean): number {
		const writeSet = this._readWrite.getWriteSet(eventSeq);
		const readSet = this._readWrite.getReadSet(event);
		const oldPriority = this.calculatePriorityWithBaseHeuristic(eventSeq, event);

		Log.PRI("[PriorityEventHandlerFirst] EventSeq", eventSeq.map((e) => e.targetId), "has writeSet", writeSet, "\nand event", event.targetId, " has readSet", readSet);
		if (executedOrToBeExecuted) {
			Log.PRI("[EHF] Target branch already executed or about to be executed, calculating priority using base heuristic");
			return oldPriority;
		} else {
			Log.PRI("[EHF] priority = INFINITY");
			return Infinity;
		}
	}
}