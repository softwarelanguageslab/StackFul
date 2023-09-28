/**
 * Checks if applying a predicate on each element of a set evaluates to true. Stops executing on first positive.
 * @param set
 * @param pred unary predicate to test elements of set
 * @returns {boolean} true on first positive match, false if none match
 */
export default function findInSet(set, pred) {
  var flag = false;
	set.forEach((e) => {
		if (pred(e)) {
			flag = true;
	  		return;
		}
	});
  return flag;
}

// Source: https://stackoverflow.com/a/46051065
export function setDifference(set1, set2) {
	return new Set([...set1].filter(x => !set2.has(x)));
}
// Source: https://stackoverflow.com/a/46051065
export function setIntersection(set1, set2) {
	return new Set([...set1].filter(x => set2.has(x)));
}
// Source: https://stackoverflow.com/a/46051065
export function setUnion(set1, set2) {
	return new Set([...set1, ...set2]);
}
