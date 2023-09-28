import ConstraintWithExecutionState from "./ConstraintWithExecutionState";
import {ExecutionState} from "@src/tester/ExecutionState";
import SymbolicEventChosen from "@src/symbolic_expression/symbolic_event_chosen";

export default class TargetConstraint extends ConstraintWithExecutionState {
    private _symbolic: SymbolicEventChosen;

    constructor(eventChosen: SymbolicEventChosen, executionState: ExecutionState) {
        super(eventChosen.processIdChosen, "TARGET", executionState);
        this._symbolic = eventChosen;
    }

    get symbolic(): SymbolicEventChosen {
        return this._symbolic;
    }

    toString() {
        return `TargetConstraint(pid:${this.processId},symbolic:${this.symbolic})`;
    }
}