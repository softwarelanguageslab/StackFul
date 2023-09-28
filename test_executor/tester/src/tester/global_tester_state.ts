import Chalk from "chalk";

import BranchConstraint from "@src/instrumentation/constraints/BranchConstraint";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import Constraint from "@src/instrumentation/constraints/Constraint";
import * as ConstraintFactory from "@src/instrumentation/constraints/ConstraintFactory";
import { dirty, wild } from "@src/instrumentation/tainter";
import Event from "@src/tester/Event";
import EventWithId from "@src/tester/EventWithId";
import ExecutionStateCollector from "./ExecutionStateCollector";
import {getSymJSHandler} from "./solve/util";
import {Logger as Log} from "@src/util/logging";
import MessageInputMap from "@src/util/datastructures/message_input_map";
import NodeProcess from "@src/process/processes/NodeProcess";
import Process from "@src/process/processes/Process";
import * as ShouldTestProcess from "./should_test_process";
import SymJSState from "./solve/symjs_state";
import SymbolicEventChosen from "@src/symbolic_expression/symbolic_event_chosen";
import Target from "@src/process/targets/Target";
import TargetConstraint from "@src/instrumentation/constraints/TargetConstraint";
import TargetEndConstraint from "@src/instrumentation/constraints/TargetEndConstraint";
import WebProcess from "@src/process/processes/WebProcess";
import assert from "node:assert";
import ComputedInput from "@src/backend/ComputedInput";
import TargetNotFoundError from "@src/process/targets/TargetNotFoundError";

export let globalTesterState: GlobalTesterState | undefined = undefined;

export function setGlobalState(state: any): void {
    globalTesterState = state;
}

type SymbolicInput = {
    input: {
        base: Function | wild;
        symbolic: any;
        meta: string;
    };
    origin: string; //indicates where symbolic input comes from
}

export class GlobalTesterState {
    private _processes: Process[];
    private eventTargets: EventWithId[]; // Events that still have to be executed
    private _originalEventTargets: EventWithId[];
    private _globalInputs: ComputedInput[];
    private nrOfActiveProcesses: number;
    private _postponedMessageEmits: (() => dirty)[]; //lst of callbacks
    private _messageCounter: number;
    private _linesCoveredThisIteration: any[];
    private readonly _timeStarted: number;
    protected _currentEvent: EventWithId | null;
    protected _globalPathConstraint: Constraint[];
    private nrProcessesFinishedSetup: number;
    private eventsFired: number;

    private branchStack: number[];
    private branchSerialToEncounter: number | undefined;

    unhandledMessages: Set<any>;
    private _currentBranchSequence: string;
    protected _executedEventSequence: EventWithId[]; // The events that have already been executed in this test run
    private _inputsUsed: SymbolicInput[];
    private _messageInputMap: MessageInputMap;
    
    constructor(processes: Process[], events: Event[], globalInputs: ComputedInput[]) {
        processes.forEach((process) => process.reset());
        const eventsWithIds = events.map((event: Event, idx: number) => EventWithId.fromEvent(event, idx));
        this._processes = processes;
        this.eventTargets = eventsWithIds.slice();
        this._originalEventTargets = eventsWithIds.slice();
        this._globalInputs = globalInputs.slice();

        this.nrOfActiveProcesses = this._processes.length;

        this._postponedMessageEmits = [];
        this._messageCounter = 0;
        this._linesCoveredThisIteration = [];
        this._timeStarted = Date.now();

        this._currentEvent = null;

        this._globalPathConstraint = [];
        this.nrProcessesFinishedSetup = 0;
        this.eventsFired = 0;
        this.unhandledMessages = new Set();

        this.branchStack = [];
        this.branchSerialToEncounter = undefined;

        this._currentBranchSequence = "";
        this._executedEventSequence = []; // array of UITargets
        this._inputsUsed = [];
        this._messageInputMap = new MessageInputMap();
    }

    setBranchSerialToEncounter(newSerial: number): void {
        this.branchSerialToEncounter = newSerial;
    }
    resetBranchSerialToEncounter(): void {
        this.branchSerialToEncounter = undefined;
    }
    getBranchSerialToEncounter(): number | undefined {
        return this.branchSerialToEncounter;
    }

    popBranchFrame(frameSerial: number): void {
        function doAssertedPop<T>(array: T[]): T {
            assert(array.length > 0);
            return array.pop()!;
        }
        function top<T>(array: T[]): T {
            return array[array.length - 1]
        }
        /*
        Might have to pop more than just one frameSerial:
        e.g., in a while-loop, the frameSerial of the body is pushed in every iteration of the
        loop, so the entire sequence those serials at the top of the stack have to all be popped
        at the same time.
        */

        /*
        Might still be problematic in certain cases? e.g.:
        function foo() {
          while (test) {
            foo();
          }
        }
         */
        while (top(this.branchStack) === frameSerial) {
            this.branchStack.pop();
        }
    }

    pushBranchFrame(frameSerial: number): void {
        this.branchStack.push(frameSerial);
    }
    getBranchStackLength(): number {
        return this.branchStack.length;
    }

    getGlobalInputs(): ComputedInput[] {
        return this._globalInputs;
    }

    addMessageInput(id): void {
        this._messageInputMap.addMessageInput(id);
    }

    addRegularInput(id): void {
        this._messageInputMap.addRegularInput(id);
    }

    getMessageInputMap(): MessageInputMap {
        return this._messageInputMap;
    }

    getProcesses(): Process[] {
        if (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
            return this._processes;
        } else if (ShouldTestProcess.shouldTestClient()) {
            return this._processes.slice(1);
        } else {
            return [this._processes[0]];
        }
    }

    getNodeProcesses(): NodeProcess[] {
        return this.getProcesses().filter((process) : process is NodeProcess => process.isANodeProcess());
    }

    getWebProcesses(): WebProcess[] {
        return this.getProcesses().filter((process) : process is WebProcess => !process.isANodeProcess());
    }

    hasRemainingProcesses(): boolean {
        return this.nrOfActiveProcesses === 0;
    }

    removeProcess(): void {
        this.nrOfActiveProcesses--;
    }

    getCurrentEvent(): EventWithId | null {
        return this._currentEvent;
    }
    setCurrentEvent(processId: number, newEvent: EventWithId | null): void {
        this._currentEvent = newEvent;
        ExecutionStateCollector.fireEvent(newEvent);
    }

    setTargetEndEncountered(processId: number): void {
        if (this._currentEvent) {
            const targetId = this._currentEvent.targetId;
            const id = this._currentEvent.id;
            this.setCurrentEvent(processId, null);
            const constraint: TargetEndConstraint = ConstraintFactory.newTargetEndConstraint(processId, id, targetId);
            this._globalPathConstraint.push(constraint);
        }
    }

    addConstraint(constraint: Constraint): void {
      this._globalPathConstraint.push(constraint);
    }

    getEventSequence(): EventWithId[] {
        return this._originalEventTargets;
    }

    getBranchSequence(): string {
        return this._currentBranchSequence;
    }

    findProcessByAlias(alias: string): Process | undefined {
        const process = this.getProcesses().find(function (process) {
            return process.alias === alias;
        })
        return process;
    }

    getCurrentEventSequence(): EventWithId[] {
        return this._executedEventSequence.slice();
    }

    getGlobalPathConstraint(): Constraint[] {
        return this._globalPathConstraint;
    }

    addCondition(branchConstraint: BranchConstraint, alias): void {
        // if (branchConstraint.symbolic.type === "Nothing") {
        //   throw new Exception("GTS.addCondition");
        // }
        const forkedBranchSequence = this._currentBranchSequence + (branchConstraint.isTrue ? "E" : "T");
        this._currentBranchSequence = this._currentBranchSequence + (branchConstraint.isTrue ? "T" : "E");
        const forkedState = new SymJSState(this._executedEventSequence.slice(), forkedBranchSequence);
        getSymJSHandler().addNewForkedState(forkedState)

        this._globalPathConstraint.push(branchConstraint);
    }

    addEvent(concreteProcessId, eventChosen: SymbolicEventChosen): void {
        const eventWithId = eventChosen.toEventWithId();
        this._executedEventSequence.push(eventWithId);
        this.setCurrentEvent(concreteProcessId, eventChosen.toEventWithId());
    }

    addSymbolicInput($$input: dirty, origin: string): void {
        const clonedInput = {base: $$input.base, symbolic: $$input.symbolic, meta: $$input.meta};
        this._inputsUsed.push({input: clonedInput, origin: origin});
    }

    getSymbolicInputs(): SymbolicInput[] {
        return this._inputsUsed.slice();
    }

    processFinishedSetup(processId): void {
        this.nrProcessesFinishedSetup++;
    }

    allProcessesFinishedSetup(): boolean {
        console.log("GTS.allProcessesFinishedSetup, nrProcessesFinishedSetup =", this.nrProcessesFinishedSetup);
        console.log("GTS.allProcessesFinishedSetup, this.getProcesses().length =", this.getProcesses().length);
        return this.nrProcessesFinishedSetup === this.getProcesses().length;
    }

    hasNextEvent(): boolean {
        return this.eventTargets.length > 0;
    }

// processId = id of the process to which to send the process
    addUnfinishedMessageEmit(callback: () => dirty): void {
        this._postponedMessageEmits.push(callback);
    }

    processHasPendingMessageEmits(): boolean {
        return this._postponedMessageEmits.length > 0;
    }

    getUnfinishedMessageEmit(): false | (() => dirty) {
        const result = this._postponedMessageEmits.shift();
        return result ? result : false;
    }

    _findProcessById(processId): Process | undefined {
        const process = this._processes.find(function (process) {
            return process.processId === processId;
        })
        return process;
    }

    private _makeNextSymbolicEvent(id: number): SymbolicEventChosen {
        const optEvent = this.eventTargets.shift();
        console.assert(optEvent !== undefined);
        const event: EventWithId = optEvent!;
        const totalNrOfProcesses = this.getProcesses().length;
        if (!event) {
            const errorMessage = `Should not happen: event is empty. Head of event-sequence is ${event}`
            Log.ALL(Chalk.bgRed(errorMessage));
            throw new Error(errorMessage);
        }
        const process = this._processes[event.processId];
        const target: Target | undefined = process.getTarget(event.targetId);
        if (target === undefined) {
            throw new TargetNotFoundError(event.targetId);
        }
        const nrOfPotentialTargets = process.getTotalNumberOfTargets();
        const eventChosen = SymbolicEventChosen.fromEvent(event, id, totalNrOfProcesses, nrOfPotentialTargets, target);
        return eventChosen;
    }

    fireNextEvent(): void {
        const eventChosen = this._makeNextSymbolicEvent(this.eventsFired);
        this.eventsFired++;
        const concreteProcessId = eventChosen.processIdChosen;
        const concreteTargetId = eventChosen.targetIdChosen;
        this.addEvent(eventChosen, eventChosen);
        const targetConstraint = ConstraintFactory.newTargetConstraint(concreteProcessId, eventChosen);
        this._globalPathConstraint.push(targetConstraint);
        const process = this._findProcessById(concreteProcessId);
        if(!process) {
            throw Error(`Process with id ${concreteProcessId} not found`);
        }
        Log.EVT(Chalk.blue(`Process ${process.alias} going to fire next event on target ${concreteTargetId}`));
        Log.EVT("Remaining events:", this.eventTargets);
        process.fireEvent(eventChosen.toEventWithId());
    }

    addUnhandledMessage(): number {
        const id = this._messageCounter;
        this._messageCounter++;
        this.unhandledMessages.add(id);
        return id;
    }

    deleteUnhandledMessage(id): boolean {
        return this.unhandledMessages.delete(id);
    }

    addLineCovered(processId: number, lineNumber: number, typeOfProcess: number): void {
        this._linesCoveredThisIteration.push({
            processId: processId,
            lineNumber: lineNumber,
            typeOfProcess: typeOfProcess
        });
    }

    getLinesCoveredThisIteration(): any[] {
        return this._linesCoveredThisIteration;
    }

    executionTime(): number {
        return Date.now() - this._timeStarted;
    }
}
