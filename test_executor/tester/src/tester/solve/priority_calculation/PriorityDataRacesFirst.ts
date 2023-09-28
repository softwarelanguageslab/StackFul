import SymJSHandler from "../symjs_handler";
import { PriorityWithBaseHeuristic } from "./PriorityWithBaseHeuristic";
import Event from "@src/tester/Event.js";

export class PriorityDataRacesFirst extends PriorityWithBaseHeuristic {
	constructor(protected _readWrite) {
		super(_readWrite);
	}
	// prefer long sequences that include events that don't 'happen before' each other, avoid repeating events
	calculatePriority(eventSeq: Event[], event: any, executedOrToBeExecuted: boolean, process: any, symJSHandler: SymJSHandler): number {
		const updatedEventSeq = eventSeq.concat(event);
		const eventSeqIds = updatedEventSeq.map(e => e.targetId);
		const oldPriority = this.calculatePriorityWithBaseHeuristic(eventSeq, event);

		const eventRacer = process._eventRacer;
		const allLinesCovered = symJSHandler._allLinesCovered;

		let happensBeforeInfluence = 0;
		let branchingEventInfluence = 0;
		let processedEvents = new Set();


		// phase 1: try to find new branches
		if (!allLinesCovered) {
			if (!executedOrToBeExecuted && oldPriority > 0) {
				// jsPrint(Chalk.green(`EventSeq: [${eventSeqIds}], newPriority: Infinity`));
				return Infinity;
			} else {
				return oldPriority;
			}
			// phase2: prefer sequences with events that don't 'happen before' each other
		} else {
			for (let a of eventSeqIds) {
				for (let b of eventSeqIds) {
					if ((!processedEvents.has(a) || !processedEvents.has(b)) && (eventSeqIds.indexOf(a) < eventSeqIds.indexOf(b)) && !eventRacer.happensBefore(a, b)) {
						happensBeforeInfluence += 1; // prefer sequences with more events that dont 'happen before' each other
						processedEvents.add(a);
						processedEvents.add(b);
					}
				}
			}
			// prefer states with event sequences that include events that branch into multiple events
			for (const event of eventSeqIds) {
				if (eventRacer.isBranchingEvent(event))
					branchingEventInfluence += 0.1;
			}

			// avoid repeating the same events
			const repeatedEventInfluence = eventSeqIds.filter((item, index) => eventSeqIds.indexOf(item) != index).length * 2;

			// jsPrint(Chalk.green(`EventSeq: [${eventSeqIds}], HBI: ${happensBeforeInfluence}, REI: ${repeatedEventInfluence}, BEI: ${branchingEventInfluence}, newPriority: ${happensBeforeInfluence + branchingEventInfluence - (repeatedEventInfluence / (eventSeq.length + 1))}`));
			return happensBeforeInfluence + branchingEventInfluence - (repeatedEventInfluence / (eventSeq.length + 1));
		}
	}
}