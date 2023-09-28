import Event from "./Event";

export default class EventWithId extends Event {
	constructor(processId: number, targetId: number, public readonly id: number) {
		super(processId, targetId);
	}
	override equals(other: EventWithId): boolean {
		return super.equals(other) && this.id === other.id;
	}
	override toString(): string {
		return `{pid:${this.processId};tid:${this.targetId};id:${this.id}}`;
	}

	static fromEvent(event: Event, id: number): EventWithId {
		return new EventWithId(event.processId, event.targetId, id);
	}
}