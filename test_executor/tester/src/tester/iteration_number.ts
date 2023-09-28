/**
 * Simple class that manages an iteration counter
 */
export default class IterationNumber {
	static iterationNumber: number = 0;

	/**
	 * Get current iteration number
	 * @returns {number} : current iteration number
	 */
	static get(): number {
		return this.iterationNumber;
	}

	/**
	 * Increases current iteration number
	 * @returns {number} : new iteration number
	 */
	static inc(): number {
		return ++this.iterationNumber;
	}
}
