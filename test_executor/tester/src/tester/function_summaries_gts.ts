import Constraint from "@src/instrumentation/constraints/Constraint";
import Event from "@src/tester/Event";
import {GlobalTesterState} from "./global_tester_state";
import Process from "@src/process/processes/Process";
import TargetConstraint from "@src/instrumentation/constraints/TargetConstraint";
import ComputedInput from "@src/backend/ComputedInput";
import SymbolicEventChosen from "@src/symbolic_expression/symbolic_event_chosen.js";

export class FunctionSummariesGTS extends GlobalTesterState {
    private _globalPathConstraintCollection: Constraint[][]
    private _globalPathConstraintFS: Constraint[][]

    constructor(processes: Process[], events: Event[], globalInputs: ComputedInput[]) {
        super(processes, events, globalInputs);
        this._globalPathConstraintCollection = [[]];
        this._globalPathConstraintFS = [[]];
    }

    _getLastPCFrame(): Constraint[] {
        return this._globalPathConstraintCollection[this._globalPathConstraintCollection.length - 1];
    }

    _getLastPCFrameFS() {
        return this._globalPathConstraintFS[this._globalPathConstraintFS.length - 1];
    }

    getGlobalPathConstraint(): Constraint[] {
        return this._globalPathConstraint.concat.apply([], this._globalPathConstraintCollection); // Flatten the individual paths
    }

    getGlobalPathConstraintFS() {
        return this._globalPathConstraint.concat.apply([], this._globalPathConstraintFS); // Flatten the individual paths
    }

    getPrefixPathConstraint() {
        const allButLastFrames = this._globalPathConstraintCollection.slice(0, this._globalPathConstraintCollection.length - 1);
        return this._globalPathConstraintCollection.concat.apply([], allButLastFrames);
    }

    enterUserFunction() {
        this._globalPathConstraintCollection.push([]);
        this._globalPathConstraintFS.push([]);
    }

    leaveUserFunction() {
        return {lastFrame: this._globalPathConstraintCollection.pop(), lastFrameFS: this._globalPathConstraintFS.pop()};
    }

    addConditionWithSummary(branchConstraint, alias, summary) {
        const lastFrame = this._getLastPCFrame();
        const lastFrameFS = this._getLastPCFrameFS();
        lastFrame.push(branchConstraint);
        lastFrameFS.push((summary === undefined) ? branchConstraint : summary);
    }

    addEvent(concreteProcessId, eventChosen: SymbolicEventChosen) {
        this._executedEventSequence.push(eventChosen.toEventWithId());
        this.setCurrentEvent(concreteProcessId, eventChosen.toEventWithId());
        const lastFrame = this._getLastPCFrame();
    }
}