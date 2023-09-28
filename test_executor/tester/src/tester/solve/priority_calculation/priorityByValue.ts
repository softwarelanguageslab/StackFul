import findInSet from "@src/util/datastructures/ExtendedSet";
import Log from "@src/util/logging";
import ReadWrite from "../read_write";
import Event from "@src/tester/Event.js";

export default class PriorityByValue {

	constructor(private _readWrite: ReadWrite) {
	}

	calculatePriority(eventSeq: Event[], event: any): number {
		const writeSet = this._readWrite.getWriteSet(eventSeq);
		const readSet = this._readWrite.getReadSet(event);
		Log.PRI("EventSeq", eventSeq.map((e) => e.targetId), "has writeSet", writeSet, "\nand event", event.targetId, " has readSet", readSet);
		if (readSet && writeSet) {
			const intersection = new Set();
			writeSet.forEach((lastWrittenValue, variable) => {
				const valuesReadForVariable = readSet.get(variable);
				if (valuesReadForVariable && findInSet(valuesReadForVariable, (valueRead) => {
					return !valueRead.isSameSymValue(lastWrittenValue);
				})) {
					intersection.add(variable);
				}
			});
			return intersection.size;
		} else {
			return 0;
		}
	}
}
