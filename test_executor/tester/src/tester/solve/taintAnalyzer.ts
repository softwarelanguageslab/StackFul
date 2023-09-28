// Keeps track of shared variables flow in event branches across test iterations

import BranchConstraint from "../../instrumentation/constraints/BranchConstraint";
import Event from "@src/tester/Event";
import {SymbolicExpression} from "../../symbolic_expression/symbolic_expressions";

class BranchTaintInfo {
    private _uses = new Set()
    private _visitedThen = false;
    private _visitedElse = false;

    constructor() {
    }

    registerConstraint(constraint: BranchConstraint, $$def: any[]) {
        if(this._uses.size === 0) {
            // We did not extract the variables before so we do it now

        }
        if(constraint.isTrue) {
            this._visitedThen = true;
        } else {
            this._visitedElse = true;
        }
    }

    hasUnvisitedBranch() {
        return !(this._visitedThen && this._visitedElse)
    }

    get uses() {
        return this._uses
    }
}

class EventTaintInfo {
    /**
     *  Symbolic values  to Vsym in SymJS paper
     * @private
     */
    private _symbols = new Set<SymbolicExpression>()
    /**
     * Locally defined variables, to be excluded from analysis
     * @private
     */
    private $$def: any[] = []
    /**
     * Branches identified in the event with their taint information
     * @private
     */
    private branchMap = new Map<number, BranchTaintInfo>()
    // TODO: are convergence points necessary in our analysis?
    //private convergencePointMap = new Map<number, ConvergencePoint>

    constructor() {
    }

    /**
     * Some housekeeping when starting a new event run
     * We can clear local definitions since they are dependent on the invocation
     */
    startEvent() {
        this.$$def = [];
    }

    addDefinition($$definition) {
        // Todo: check if we can use this without deeper comparison function. Current assumption is that we use it
        // only in the context of the same event run, so we can trust object reference comparison
        if(!this.$$def.includes($$definition)) {
            this.$$def.push($$definition)
        }
    }

    registerBranchConstraint(serial: number, constraint: BranchConstraint) {
        let branchInfo = this.branchMap.get(serial)
        if(branchInfo === undefined) {
            branchInfo = new BranchTaintInfo()
            this.branchMap.set(serial, branchInfo)
        }
        branchInfo.registerConstraint(constraint, this.$$def)
    }

    getBranches() {
        return this.branchMap.values()
    }
}

export class TaintAnalyzer {
    // Map between eventHandler serial id and branches
    eventMap = new Map<string, EventTaintInfo>();

    constructor() {
    }

    private _eventToKey(event: Event): string {
        return "" + event.processId + "_" + event.targetId;
    }

    registerLocalDefinition(event: Event, $$definition) {
        const eventKey = this._eventToKey(event);
        const eventInfo = this.eventMap.get(eventKey);
        eventInfo!.addDefinition($$definition);
    }

    registerBranchConstraint(serial: number, event: Event, constraint: BranchConstraint) {
        const eventKey = this._eventToKey(event);
        let eventInfo = this.eventMap.get(eventKey);
        if(eventInfo === undefined) {
            eventInfo = new EventTaintInfo()
            this.eventMap.set(eventKey, eventInfo)
        }
        eventInfo.registerBranchConstraint(serial, constraint);
    }

    getUnvisitedBranchInfo(event: Event): BranchTaintInfo[] {
        const eventKey = this._eventToKey(event);
        let eventInfo = this.eventMap.get(eventKey);
        if(eventInfo === undefined) {
            return []
        }
        return [...eventInfo.getBranches()].filter((branch) => branch.hasUnvisitedBranch())
    }
}
