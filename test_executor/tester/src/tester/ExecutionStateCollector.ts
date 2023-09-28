import {ExecutionState, TargetTriggeredExecutionState} from "./ExecutionState"
import EventWithId from "@src/tester/EventWithId.js";

class ExecutionStateCollector {
	private _processes: Map<number, ExecutionState>;
	constructor() {
		this._processes = new Map<number, ExecutionState>();
		this.reset();
	}
	setExecutionState(processId: number, executionState: ExecutionState) {
		// if (processId === 1) {
		// 	console.log("ExecutionStateCollector.setExecutionState:", executionState);
		// }
		this._processes.set(processId, executionState);
	}
	getExecutionState(processId: number): ExecutionState {
		return this._processes.get(processId)!;
	}
	updateSerial(newSerial: number, processId: number): void {
		const es = this.getExecutionState(processId);
		this.setExecutionState(processId, es.updateSerial(newSerial, processId));
	}
	fireEvent(event: EventWithId | null): void {
		// console.log("ExecutionStateCollector.fireEvent");
		// console.log("this._processes:", this._processes);
		for (let [processId, _] of this._processes) {
			// console.log("ExecutionStateCollector.fireEvent", processId, "event?.processId", event?.processId);
			if (event !== null && event.processId == processId) {
				// console.log("ExecutionStateCollector.fireEvent, true");
				const newES = new TargetTriggeredExecutionState(event.id, event.processId)
				this.setExecutionState(processId, newES);
			} else {
				// console.log("ExecutionStateCollector.fireEvent, false");
				this.setExecutionState(processId, this.getExecutionState(processId).fireEvent());
			}
		}
	}
	reset(): void {
		this._processes = new Map<number, ExecutionState>();
	}
}

const instance = new ExecutionStateCollector();

export default instance;