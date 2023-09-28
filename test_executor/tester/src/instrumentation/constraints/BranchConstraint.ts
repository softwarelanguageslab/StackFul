import * as SymExp from "../../symbolic_expression/symbolic_expressions";
import ConstraintWithExecutionState from "./ConstraintWithExecutionState";
import { SymbolicExpression } from "../../symbolic_expression/symbolic_expressions";
import SymbolicUnaryExp from "../../symbolic_expression/symbolic_unary_exp";
import {BooleanNot} from "../../symbolic_expression/operators";


export default class BranchConstraint extends ConstraintWithExecutionState {

    constructor(processId: number, private _symbolic, private _wasTrue, executionState, private _canBeNegated = true) {
        super(processId, "CONSTRAINT", executionState);
    }

    get symbolic() {
        return this._symbolic;
    }

    get isTrue() {
        return this._wasTrue;
    }

    instantiate(substitutes): SymbolicExpression {
        var res: SymbolicExpression;
        if (this.isTrue) {
            res = this.symbolic.instantiate(substitutes);
        } else {
            res = new SymbolicUnaryExp(BooleanNot, this.symbolic.instantiate(substitutes))
        }
        return res;
    }

    instantiateConstraint(substitutes): BranchConstraint {
        return new BranchConstraint(this.processId, this.symbolic.instantiate(substitutes), this.isTrue, this._executionState, this._canBeNegated);
    }

    toBranchConstraint(): BranchConstraint {
        return new BranchConstraint(this.processId, this.symbolic, this.isTrue, this._executionState, this._canBeNegated);
    }

    toString() {
        return `BranchConstraint(pid:${this.processId},symbolic:${this.symbolic},isTrue:${this.isTrue})`;
    }

    usesValue(checkFunction) {
        return this.symbolic.usesValue(checkFunction);
    }
}