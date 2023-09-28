import Event from "@src/tester/Event.js";
import {Logger as Log} from "@src/util/logging";
import ReadWrite from "../read_write";

export default class PriorityByName {
	constructor(private _readWrite: ReadWrite) {
	}

	calculatePriority(eventSeq: Event[], event: any): number {
		const writeSet = this._readWrite.getWriteSet(eventSeq);
		const readSet = this._readWrite.getReadSet(event);
		Log.PRI("EventSeq", eventSeq.map((e) => e.targetId), "has writeSet", (writeSet) ? writeSet.toString() : "undefined", "\nand event", event.targetId, " has readSet", (readSet) ? readSet.toString() : "undefined");
		if (readSet && writeSet) {
			const intersection = new Set([...writeSet].filter(i => readSet.has(i)));
			return intersection.size;
		} else {
			return 0;
		}
	}
}

