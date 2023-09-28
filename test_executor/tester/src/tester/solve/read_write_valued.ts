import findInSet from "@src/util/datastructures/ExtendedSet";
import {Logger as Log} from "@src/util/logging";
import ReadWrite from "./read_write";
import Event from "@src/tester/Event.js";

export default class ReadWriteValued extends ReadWrite {
    constructor() {
        super();
    }

    // Stores 1 value per variable in the write-set of the event-sequence
    addWrite(eventSeq: Event[], varName, value): void {
        Log.VRW("Adding variable write", varName, "with value:", value);
        const key = this._eventSeqToKey(eventSeq);
        let writeSet = this.writeSets.get(key);
        if (writeSet === undefined) {
            writeSet = new Map();
            this.writeSets.set(key, writeSet);
        }
        writeSet.set(varName, value);
    }

    // Stores multiple values per variable in the read-set of the event-sequence
    addRead(event: Event, varName, value): void {
        Log.VRW("Adding variable read:", varName, "with value:", value);
        const key = this._eventToKey(event);
        let readSet = this.readSets.get(key);
        if (readSet === undefined) {
            readSet = new Map();
            this._eventsWhoseReadSetChanged.add(key);
            this.readSets.set(key, readSet);
        }
        let valuesReadSet = readSet.get(varName);
        if (valuesReadSet === undefined) {
            valuesReadSet = new Set();
            this._eventsWhoseReadSetChanged.add(this._eventToKey(event));
            readSet.set(varName, valuesReadSet);
        }
        if (!findInSet(valuesReadSet, (val) => value.isSameSymValue(val))) {
            this._eventsWhoseReadSetChanged.add(this._eventToKey(event));
            valuesReadSet.add(value);
        }
    }
}