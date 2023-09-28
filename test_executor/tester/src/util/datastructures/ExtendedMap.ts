/**
 * Map with extended functions getOrElse and getOrElseInsert
 */
export default class ExtendedMap<K, V> extends Map<K, V> {

  constructor() {
    super();
  }

  getOrElse(key: K, alternative: V): V {
    const optEntry = this.get(key);
    if (optEntry === undefined) {
      return alternative;
    }
    return optEntry;
  }

  getOrElseInsert(key: K, makeAlternative: () => V): V {
    const optEntry = this.get(key);
    if (optEntry === undefined) {
      const alternative = makeAlternative();
      this.set(key, alternative);
      return alternative;
    }
    return optEntry;
  }

  duplicate(makeDuplicate: (V) => V): ExtendedMap<K, V> {
    const newMap = new ExtendedMap<K, V>();
    const iterator = this.entries();
    let result = iterator.next();
    while (!result.done) {
      const [key, value] = result.value;
      const duplicateValue = makeDuplicate(value);
      newMap.set(key, duplicateValue);
      result = iterator.next();
    }
    return newMap;
  }
}
