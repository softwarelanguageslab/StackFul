import Chalk from "chalk";

import Process from "@src/process/processes/Process";
import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import {SymJSAnalysisEnum} from "@src/config/user_argument_parsing/SymJS";
import Target from "@src/process/targets/Target";
import Log from "@src/util/logging";
import {stateInCollection, UITargetToString} from "@src/util/misc";
import Priority from "./priority_implementation/priority";
import { PriorityEventHandlerFirst } from "./priority_calculation/PriorityByEventHandlerFirst";
import PriorityByName from "./priority_calculation/priorityByName";
import PriorityByValue from "./priority_calculation/priorityByValue";
import { PriorityLengthReadWrite } from "./priority_calculation/PriorityByWriteReadLength";
import { PriorityDataRacesFirst } from "./priority_calculation/PriorityDataRacesFirst";
import {PriorityQueue, QElement} from "./priority_implementation/priority_queue";
import ReadWrite from "./read_write";
import ReadWriteValued from "./read_write_valued";
import SymJSState from "./symjs_state";
import {TaintAnalyzer} from "./taintAnalyzer";
import Event from "@src/tester/Event.js";

function makePriorityQueue(): PriorityQueue {
    return new PriorityQueue((s1, s2) => s1.equalsState(s2));
}

export default class SymJSHandler {
    private _priorityQueue: PriorityQueue;
    private _eventHandlerKeysAlreadyDiscovered: Set<string>;
    private _newlyDiscoveredEventHandler: Map<string, Target>;
    public _eventHandlers: Target[];
    private readonly _readWrite: ReadWrite;
    public _currentState: SymJSState;
    private readonly _exercisedStates: SymJSState[];
    private _newForkedStates: SymJSState[];
    private _taintAnalyzer: TaintAnalyzer = new TaintAnalyzer();
    public _process: Process | undefined 
    private _eventsRegisteredThisIteration: Set<string>;
    
    //DRF:
    public _totalNrOfLines: number;
    public _lineCoverageProgress: any[]    
    public _allLinesCovered: boolean
    public _dataRaceCoverageProgress: any[]
    public _nrOfPossibleEventSequences: number
    public _sharedVariables: object;
    public _mustRecalculate: number;

    constructor() {
        this._priorityQueue = makePriorityQueue();
        const initialState = SymJSState.emptyStartState;
        this._priorityQueue.enqueue(initialState, this._priorityQueue.maxPriority);

        this._eventHandlerKeysAlreadyDiscovered = new Set();
        this._newlyDiscoveredEventHandler = new Map();
        this._eventHandlers = [];
        this._readWrite = new ReadWriteValued();
        this._currentState = initialState;

        //DRF:
        this._totalNrOfLines = 0
        this._lineCoverageProgress = []
        this._allLinesCovered = false 
        this._dataRaceCoverageProgress = []
        this._nrOfPossibleEventSequences = 0
        this._sharedVariables = {}
        this._mustRecalculate = 0

        this._exercisedStates = [];
        this._newForkedStates = [];
        this._process = undefined;

        this._eventsRegisteredThisIteration = new Set();
    }

    get readWrite(): ReadWrite {
        return this._readWrite;
    }


    /*********************************************************************
     * Notification functions called by components such as Advice and Process so that SymJSHandler
     * can update information related to events, forked states, etc
     *********************************************************************/

    /**
     * Add forked state to this._newForkedStates only if the state was not exercised yet, avoiding doubles
     * @param state
     */
    addNewForkedState(state: SymJSState): void {
        if (!stateInCollection(state, this._exercisedStates)) {
            this._addStateToArray(state, this._newForkedStates);
        }
    }

    eventHandlerDiscovered(UITarget: Target, branchSequence: string): void {
        const stringRepresentation = UITargetToString(UITarget);
        this._eventsRegisteredThisIteration.add(stringRepresentation)
        if (!this._eventHandlerKeysAlreadyDiscovered.has(stringRepresentation)) {
            Log.ALL("SymJSHandler:", "Discovered a new event handler");
            this._eventHandlerKeysAlreadyDiscovered.add(stringRepresentation);
            this._newlyDiscoveredEventHandler.set(stringRepresentation, UITarget);
            this._eventHandlers.push(UITarget);
        }
    }

    /*********************************************************************
    ***********************************************************************/


    /**
     * Creates and stores new SymJSState instances based on the last state and newly discovered events.
     * @param branchSequence
     * @private
     */
    private _addNewlyDiscoveredEventHandlers(branchSequence: string | undefined): void {
        // TODO: check second argument of callback, not used and supposed to be the index according to forEach syntax
        this._newlyDiscoveredEventHandler.forEach((UITarget, stringRepresentation) => {
            const newState = new SymJSState(this._currentState.getEventSequence().concat([UITarget.toEvent()]), branchSequence);
            this._priorityQueue.enqueue(newState, new Priority(Infinity, newState.getEventSequence().length));
        })
    }

    countDormantIterations(lineProgress): number { // count number of iteration that no new lines were executed (dorment iterations)
        var ref = lineProgress[lineProgress.length - 1];
        var c = 0;
        for (let i = lineProgress.length - 1; i > 0; i--) {
            if (lineProgress[i] == ref) { c += 1; } else {	return c; }
        }
        return c;
    }

    shouldSkipPhase1(): boolean {
        const lineProgress = this._lineCoverageProgress;
        if (lineProgress.length > 5) {
            let posEventSeqs = this._nrOfPossibleEventSequences;
            const totalLines = this._totalNrOfLines;
            const linesCovered = lineProgress[lineProgress.length - 1];
            let nrOfEvents = this._eventHandlers.length;
            const fill = 10; // minimum nr of iterations before considering skipping
    
            // Log.ALL(Chalk.blue(`posEventSequences: ${posEventSeqs}`));
            // Log.ALL(Chalk.blue(`totalLines: ${totalLines}`));
            // Log.ALL(Chalk.blue(`lineProgress: ${lineProgress}`));
            // Log.ALL(Chalk.blue(`linesCovered: ${linesCovered}`));
    
            const dormentIterations = this.countDormantIterations(lineProgress);
            // Log.ALL(Chalk.blue(`Dorment iterations: ${dormentIterations}`));
    
            // more than 500 possible events sequences would inflate the threshold unnecessarily
            if (posEventSeqs > 500) posEventSeqs = 500;
    
            const m = Math.sqrt(nrOfEvents * Math.sqrt(posEventSeqs));
            const n = (totalLines - linesCovered) / nrOfEvents;
            const threshold = Math.ceil(fill + m * n);
    
            if (dormentIterations > threshold) {
                Log.DRF(Chalk.bgBlue("[DRF] Skipping phase 1, too many iterations without progress"));
                return true;
            } else {
                Log.DRF(Chalk.bgBlue(`[DRF] ${threshold - dormentIterations} dormant iterations before skipping phase 1`));
                return false;
            }
        } else {
            return false; // never skip phase 1 in first 5 iterations of testing
        }
    }

    /**
     * Prepare for a new iteration run by:
     * -
     * @param branchSequenceFollowed
     */
    _newIteration(branchSequenceFollowed: string | undefined): void {
        
        //DRF bookkeeping:
        if (ConfigurationReader.config.SYMJS_ANALYSIS == SymJSAnalysisEnum.data_races_first) {
            // as long as not all lines have been covered, try to find new branches (phase 1 of DRF)
            if (!this._allLinesCovered) {
                Log.DRF(Chalk.bgBlue(`[DRF] Phase 1`));
            } else {
                Log.DRF(Chalk.bgBlue(`[DRF] Phase 2`));
            }
    
            // if finding unexplored branches takes too long, give up and start looking for data races
            // !this._allLinesCovered && this._nrOfTryToFindNewEvent > 10 && this._infinityThreshold >= Math.floor(this._eventHandlers.length/3)
            if (!this._allLinesCovered && this.shouldSkipPhase1()) {
                this._allLinesCovered = true;
            }
    
            // if all lines have been covered, recalculate all states in PQ once (to get rid of inf priorities)
            if (this._allLinesCovered && this._mustRecalculate == 0) {
                this._mustRecalculate = 1;
            }
        }

        // Add newly forked states to the priority-queue while checking the state does not exist yet
        this._addNewForkedStatesToPQ();

        // Add new states based on newly discovered events
        this._addNewlyDiscoveredEventHandlers(branchSequenceFollowed);

        // Add new states for existing events
        Log.ALL("SymJSHandler:", "Going to create new states with added event sequences");
        const currentEventSeq = this._currentState.getEventSequence();
        for (let i in this._eventHandlers) {
            const UITarget = this._eventHandlers[i];
            const stringRepresentation = UITargetToString(UITarget);
            
            // If this event was not registered, do not add it to a potential event sequence
		    if (! this._eventsRegisteredThisIteration.has(stringRepresentation)) {
		    	continue;
		    }

            if (this._newlyDiscoveredEventHandler.has(UITargetToString(UITarget))) {
                continue; // If this is a newly discovered event-handler, it has already been added to the queue with maximum priority
            }
            const updatedEventSeq = currentEventSeq.concat([UITarget.toEvent()]);
            const nrOfConflicts = this.calculatePriority(currentEventSeq, UITarget, false);
            const priority = new Priority(nrOfConflicts, updatedEventSeq.length);
            Log.PRI(`Priority is ${priority}`);
            const newState = new SymJSState(updatedEventSeq, branchSequenceFollowed);
            this._priorityQueue.enqueue(newState, priority);
        }
        Log.ALL("SymJSHandler:","Has created new states with added event sequences");

        // Recalculate the priority of those states of which the read-set of the last event in the event-sequence has changed
        Log.ALL("SymJSHandler:", "Going to recalculate state priorities");
        const newPriorityQueue = makePriorityQueue();
        const eventsWhoseReadSetChanged = this._readWrite.getEventsWhoseReadSetChanged();
        Log.PRI("eventsWhoseReadSetChanged =", eventsWhoseReadSetChanged);
        this._priorityQueue.forEach((qElement: QElement) => {
            const oldPriority: Priority = qElement.priority;
            const state: SymJSState = qElement.element;
            const eventSeq: Event[] = state.getEventSequence();
            const lastEvent: any = (eventSeq.length !== 0) ? eventSeq[eventSeq.length - 1] : undefined; 
            Log.PRI("Recalculating state", state.toString(), "; last event =", lastEvent ? lastEvent.toString() : "undefined");
            let alreadyExecutedOrToBeExecuted: boolean;
            if(this._exercisedStates.length == 0) {
                alreadyExecutedOrToBeExecuted = true
            } else {
                let suffixesOfBranchExecuted: SymJSState[] = this._exercisedStates.filter((exercisedState) => {
                    const exercisedBranchSeq = exercisedState.getBranchSequence();
                    const branchSeq = state.getBranchSequence();
                    return (exercisedBranchSeq as string).startsWith(branchSeq as string)
                           && this.compareEventSequences(exercisedState.getEventSequence(), eventSeq);
                });
                alreadyExecutedOrToBeExecuted = suffixesOfBranchExecuted.length > 0;
            }
            if (lastEvent && eventsWhoseReadSetChanged.has(UITargetToString(lastEvent))) {
                Log.PRI(Chalk.green("Changing this state's priority"));
                const initialPart: Event[] = eventSeq.slice(0, -1);
                const newPriority: Priority = new Priority(this.calculatePriority(initialPart, lastEvent, alreadyExecutedOrToBeExecuted), eventSeq.length);
                Log.PRI(`New priority is ${newPriority}`);
                newPriorityQueue.enqueue(state, newPriority);
            } else if (ConfigurationReader.config.SYMJS_ANALYSIS == SymJSAnalysisEnum.data_races_first && this._mustRecalculate == 1 && oldPriority._nrOfConflicts == Infinity) {
                const initialPart: Event[] = eventSeq.slice(0, -1);
                const newPriority: Priority = new Priority(this.calculatePriority(initialPart, lastEvent, alreadyExecutedOrToBeExecuted), eventSeq.length);
                newPriorityQueue.enqueue(state, newPriority);
            }
            else {
                newPriorityQueue.enqueue(state, oldPriority);
            }
        });
        Log.ALL("SymJSHandler:", "Has recalculated");

        if (this._mustRecalculate == 1) this._mustRecalculate = 2;
        this._newForkedStates = [];
        this._readWrite.newIteration();
        this._priorityQueue = newPriorityQueue;
        this._newlyDiscoveredEventHandler = new Map();
        this._eventsRegisteredThisIteration = new Set();
    }

    compareEventSequences = function (seq1, seq2): boolean {
        if (seq1.length != seq2.length) return false;
        for (const idx in seq1) {
            if (seq1[idx].targetId != seq2[idx].targetId)
                return false;
        }
        return true;
    }

    /**
     * Calculates priority score by selecting the configured strategy
     * @param initialPart
     * @param lastEvent
     * @private
     */
    private calculatePriority(initialPart: Event[], lastEvent: any, alreadyExecutedOrToBeExecuted: boolean): number {
        switch(ConfigurationReader.config.SYMJS_ANALYSIS) {
            case SymJSAnalysisEnum.named: {
                const priorityCalculator = new PriorityByName(this._readWrite)
                return priorityCalculator.calculatePriority(initialPart, lastEvent)
            }
            case SymJSAnalysisEnum.valued:{
                const priorityCalculator = new PriorityByValue(this._readWrite)
                return priorityCalculator.calculatePriority(initialPart, lastEvent)
            }
            case SymJSAnalysisEnum.event_handler_first: {
                const priorityCalculator = new PriorityEventHandlerFirst(this._readWrite)
                return priorityCalculator.calculatePriority(initialPart, lastEvent, alreadyExecutedOrToBeExecuted)
            }
            case SymJSAnalysisEnum.write_read_length: {
                const priorityCalculator = new PriorityLengthReadWrite(this._readWrite)
                return priorityCalculator.calculatePriority(initialPart, lastEvent)
            }
            case SymJSAnalysisEnum.data_races_first: {
                const priorityCalculator = new PriorityDataRacesFirst(this._readWrite)
                return priorityCalculator.calculatePriority(initialPart, lastEvent, alreadyExecutedOrToBeExecuted, this._process, this)
            }
            case SymJSAnalysisEnum.conditional_valued:
                throw new Error("conditional_valued priority calculation not yet implemented")
            case SymJSAnalysisEnum.taint_named:
                // return PriorityByTaintName.calculatePriority(this._readWrite, initialPart, lastEvent, undefined)
                throw new Error("Not implemented yet")
        }
        throw new Error("Unsupported SYMJS analysis method: " + ConfigurationReader.config.SYMJS_ANALYSIS)
    }

    /**
     * Add newly forked states to priority queue, avoiding doubles
     * @private
     */
    private _addNewForkedStatesToPQ(): void {
        const currentlyEnqueuedStates = this._priorityQueue._items.map((qElement) => qElement.element);
        this._newForkedStates.forEach((newState) => {
            if (!stateInCollection(newState, currentlyEnqueuedStates)) {
                this._priorityQueue.enqueue(newState, new Priority(+Infinity, newState.getEventSequence().length));
            }
        })
    }

    /**
     * Gets next state from priority queue, save it into current state and pushes it into the exercisedStates array
     */
    dequeueNextState(): SymJSState {
        const nextState = this._priorityQueue.dequeue();
        if (nextState) {
            Log.ALL("SymJSHandler", "nextState = ", nextState.toString());
        }
        this._exercisedStates.push(nextState);
        this._currentState = nextState;
        return nextState;
    }

    nextState(branchSequenceFollowed?: string): SymJSState {
        this._newIteration(branchSequenceFollowed);
        return this.dequeueNextState();
    }

    /**
     * Add SymJSState to an array, avoiding doubles
     * @param state
     * @param collection
     */
    _addStateToArray(state: SymJSState, collection: SymJSState[]): void {
        if (!stateInCollection(state, collection)) {
            collection.push(state);
        }
    }
    
    get taintAnalyzer(): TaintAnalyzer {
        return this._taintAnalyzer
    }

}
