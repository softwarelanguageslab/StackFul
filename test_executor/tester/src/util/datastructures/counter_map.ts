/**
 * Simple map that keeps counter for each key
 */
export default class CounterMap {
  private _map = new Map();

  getCounter(id) {
    return this._map.has(id) ? this._map.get(id) : 0;
  }
  incCounter(id) {
    const newCounter = this.getCounter(id) + 1;
    this._map.set(id, newCounter);
    return newCounter
  }
  setCounter(id, counter) {
    this._map.set(id, counter);
  }
  copy() {
    const clonedMap = new Map(this._map)
    const clone = new CounterMap();
    clone._map = clonedMap;
    return clone;
  }
}
