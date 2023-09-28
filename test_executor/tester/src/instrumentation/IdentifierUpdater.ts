import ExtendedMap from "../util/datastructures/ExtendedMap";
import noIdentifier from "./NoIdentifier"

interface ValueTuple {
  isInnerValue: boolean,
  value: any
}

export default class IdentifierUpdater {
  protected _store: ExtendedMap<String, Set<ValueTuple>>;

  constructor() {
    this._store = new ExtendedMap<String, Set<ValueTuple>>();
  }
  _toKey(processId: number, identifier: string): string {
    return "" + processId + "_" + identifier;
  }
  reset(): void {
    this._store = new ExtendedMap<String, Set<ValueTuple>>();
  }
  _addValueByKey(key, value, isInnerValue): void {
    const set = this._store.getOrElseInsert(key, () => new Set<ValueTuple>());
    set.add({isInnerValue, value});
  }
  _removeIdentifiersByKey(key): void {
    const set = this._store.getOrElse(key, new Set<ValueTuple>());
    for (let key in set) {
      const tuple = set[key];
      const {isInnerValue, value} = tuple;
      if (isInnerValue) {
        value.varName = null;
        if (value.symbolic) {
          value.symbolic = value.symbolic.setIdentifier(noIdentifier);
        }
      } else {
        const newValue = value.setIdentifier(noIdentifier);
        set[key] = {isInnerValue,newValue}
      }
    }
  }
  public addValue(processId, identifier, $$value): void {
    const key = this._toKey(processId, identifier);
    this._addValueByKey(key, $$value, true);
  }
  public addSymValue(processId, identifier, symValue): void {
    const key = this._toKey(processId, identifier);
    this._addValueByKey(key, symValue, false);
  }
  public removeIdentifiers(processId, identifier): void {
    const key = this._toKey(processId, identifier);
    this._removeIdentifiersByKey(key);
  }
}