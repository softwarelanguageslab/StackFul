/* https://stackoverflow.com/a/34890276 */
/**
 * constructs a new array of (valuekey, instances) pairs where valuekey is a unique value corresponding to key and
 * instances are the instances which have that value for the specified key
 * @param {array} xs - array of objects to group
 * @param {string | function} key - key to group by
 * @returns {array} - array of values and corresponding items
 */
export default function groupByArray(xs, key) {
    return xs.reduce(function (rv, x) {
        let v = key instanceof Function ? key(x) : x[key];
        let el = rv.find((r) => r && r.key === v);
        if (el) {
            el.values.push(x);
        } else {
            rv.push({key: v, values: [x]});
        }
        return rv;
    }, []);
}

