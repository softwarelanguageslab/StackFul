export default class Priority {
  public _nrOfConflicts: any;
  private _eventSequenceLength: any;

  constructor(nrOfConflicts, eventSequenceLength) {
    this._nrOfConflicts = nrOfConflicts;
    this._eventSequenceLength = eventSequenceLength;
  }

  equalTo(other) {
    return this._nrOfConflicts === other._nrOfConflicts &&
           this._eventSequenceLength === other._eventSequenceLength;
  }

  greaterThan(other) {
    return (this._nrOfConflicts > other._nrOfConflicts) ||
           (this._nrOfConflicts === other._nrOfConflicts && this._eventSequenceLength < other._eventSequenceLength);
  }

  toString() {
    return `<${this._nrOfConflicts};${this._eventSequenceLength}>`;
  }
}
