export default class Event {
	constructor(public processId: number, public targetId: number) {}
	toString(): string {
		return `{pid:${this.processId};tid:${this.targetId}}`;
	}
	equals(other: Event): boolean {
		return this.processId === other.processId && this.targetId === other.targetId;
	}
}