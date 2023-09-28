import Event from "@src/tester/Event";

// processIdChosen, targetIdChosen: Int
import {SymbolicExpression} from "./symbolic_expressions";
import Target from "../process/targets/Target";
import EventWithId from "@src/tester/EventWithId.js";

export default class SymbolicEventChosen extends SymbolicExpression {

    protected readonly startingProcess: number = 0;

    constructor(public id: number, public processIdChosen: number, public targetIdChosen: number,
                public totalNrOfProcesses: number, public totalNrOfEvents: number, protected target: Target) {
        super("SymbolicEventChosen");
    }

    public getTarget(): Target  {
        return this.target;
    }

    toString() {
        return `SymbolicEventChosen(id:${this.id}, processIdChosen:${this.processIdChosen}, targetIdChosen:${this.targetIdChosen}, totalNrOfProcesses: ${this.totalNrOfProcesses}, totalNrOfEvents: ${this.totalNrOfEvents}, target: ${this.target})`;
    }

    isSameSymValue(other) {
        throw (other instanceof SymbolicEventChosen) && this.id === other.id && this.processIdChosen === other.processIdChosen && this.targetIdChosen === other.targetIdChosen &&
        this.totalNrOfProcesses === other.totalNrOfProcesses && this.totalNrOfEvents === other.totalNrOfEvents && this.target === other.target;
    }

    usesValue(checkFunction) {
        return false;
    }

    public static fromEvent(event: Event, eventId: number, totalNrOfProcesses: number, totalNrOfEvents: number, target: Target): SymbolicEventChosen {
        return new SymbolicEventChosen(eventId, event.processId, event.targetId, totalNrOfProcesses, totalNrOfEvents, target);
    }

    public toEventWithId(): EventWithId {
        return new EventWithId(this.processIdChosen, this.targetIdChosen, this.id);
    }

}