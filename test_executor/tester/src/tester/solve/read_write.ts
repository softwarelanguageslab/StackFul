import Event from "@src/tester/Event";
import {Logger as Log} from "@src/util/logging";

export default class ReadWrite {
	protected readSets: Map<any, any> = new Map();
	protected writeSets: Map<any, any> = new Map();
	protected _eventsWhoseReadSetChanged: Set<string> = new Set();

	newIteration(): void {
		this._eventsWhoseReadSetChanged = new Set();
	}

	getEventsWhoseReadSetChanged(): Set<string> {
		return this._eventsWhoseReadSetChanged;
	}

	addWrite(eventSeq: Event[], varName, value?): void {
		Log.VRW("Adding variable write", varName);
		const key = this._eventSeqToKey(eventSeq);
		var writeSet = this.writeSets.get(key);
		if (writeSet === undefined) {
			writeSet = new Set();
			this.writeSets.set(key, writeSet);
		}
		writeSet.add(varName);
	}

	getWriteSet(eventSeq: Event[]) {
		const key = this._eventSeqToKey(eventSeq);
		const writeSet = this.writeSets.get(key);
		return writeSet;
	}

	eventToKey(event: Event): string {
		const key = "" + event.processId + "_" + event.targetId;
		return key;
	}

	getWriteSetIncluding (event: Event): any {
		const eventKey = this.eventToKey(event) // e.g. event obj --> 1__1
		let extendedKey; // key in this.writeSets that includes eventKey (e.g. 1_0___1_1)
		for (const [key, value] of this.writeSets.entries()) {
			// jsPrint(Chalk.bgBlue(`trying to find ${eventKey} in ${key}`));
			if (key.includes(eventKey)) {
				// jsPrint(Chalk.blue(`found ${eventKey} in ${key}`));
				extendedKey = key;
				break;
			}
		}
		if (extendedKey == null)
			return;
		const tryWriteSet = this.writeSets.get(extendedKey.replace("___" + eventKey, ""));
		if (tryWriteSet)
			return this.intersectionOfWriteSets(tryWriteSet, this.writeSets.get(extendedKey));
		let otherKeys = extendedKey.split("___").filter(e => e != eventKey); // remove eventKey from extendedKey (e.g. 1_0___1_1 --> ['1_0'])
		// jsPrint(Chalk.blue(`otherKeys: ${otherKeys}`));
		let resWriteSet = this.writeSets.get(extendedKey);
		for (const otherKey of otherKeys) {
			const otherWriteSet = this.writeSets.get(otherKey);
			if (otherWriteSet)
				resWriteSet = this.intersectionOfWriteSets(otherWriteSet, resWriteSet);
		}
		// jsPrint(Chalk.blue(`resWriteSet: ${resWriteSet}`));
		return resWriteSet;
	}
	intersectionOfWriteSets(smallerWriteSet, biggerWriteSet) {
		let res = new Map();
		for (const [key, value] of biggerWriteSet) {
			if (!smallerWriteSet.has(key))
				res.set(key, value);
		}
		return res;
	}

	addRead(event: Event, varName, value?): void {
		const key = this._eventToKey(event);
		var readSet = this.readSets.get(key);
		if (readSet === undefined) {
			readSet = new Set();
			this._eventsWhoseReadSetChanged.add(this._eventToKey(event));
			this.readSets.set(key, readSet);
		}
		if (! readSet.has(varName)) {
			this._eventsWhoseReadSetChanged.add(this._eventToKey(event));
			readSet.add(varName);
		}
	}

	getReadSet(event: Event) {
		const key = this._eventToKey(event);
		return this.readSets.get(key);
	}

	protected _eventToKey(event: Event): string {
		return "" + event.processId + "_" + event.targetId;
	}

	protected _eventSeqToKey(eventSeq): string {
		const individualKeys = eventSeq.map(this._eventToKey);
		return individualKeys.join("___");
	}

}


