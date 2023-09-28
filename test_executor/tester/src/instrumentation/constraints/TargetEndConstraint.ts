import ConstraintWithExecutionState from "./ConstraintWithExecutionState";
import SymbolicEventEnd from "../../symbolic_expression/symbolic_event_end";

export default class TargetEndConstraint extends ConstraintWithExecutionState {
    protected readonly startingProcess: number = 0;
    protected _symbolic: any;
    constructor(processId: number, id: number, targetId: number, executionState) {
        super(processId, "END_TARGET", executionState);
        this._symbolic = new SymbolicEventEnd(id, processId, targetId);
    }
    get getSymbolic() {
        return this._symbolic;
    }
    toString() {
        return `TargetEndConstraint(pid:${this.processId},symbolic:${this._symbolic})`;
    }
}