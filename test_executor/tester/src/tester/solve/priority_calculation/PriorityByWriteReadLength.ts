import { PriorityWithBaseHeuristic } from "./PriorityWithBaseHeuristic";
import {Logger as Log} from "@src/util/logging";
import Event from "@src/tester/Event.js";

export class PriorityLengthReadWrite extends PriorityWithBaseHeuristic {
	constructor(protected _readWrite) {
		super(_readWrite);
	}
	calculatePriority(eventSeq: Event[], event: any): number {
		const lengthOfSequence = eventSeq.length + 1; // avoid division by zero
		const oldPriority = this.calculatePriorityWithBaseHeuristic(eventSeq, event);
		const lengthInfluence = 1 / (lengthOfSequence * 1000);
		const newPriority = oldPriority + lengthInfluence;
		Log.PRI(`Priority calculated was: old priority = ${oldPriority} and new priority: ${newPriority}`);
		return newPriority;
	}
}