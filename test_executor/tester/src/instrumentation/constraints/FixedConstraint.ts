import Constraint from "./Constraint";

export default class FixedConstraint extends Constraint {
    _comesFrom: any;
    
    constructor(processId, private _symbolic) {
        super(processId, "FIXED");
    }

    get symbolic() {
        return this._symbolic;
    }

    instantiate(substitutes) {
        return this.symbolic.instantiate(substitutes);
    }

    instantiateConstraint(substitutes): FixedConstraint {
        return new FixedConstraint(this.processId, this.instantiate(substitutes));
    }

    toString() {
        return `FixedConstraint(pid:${this.processId}, symbolic:${this._symbolic}`;
    }

    toBranchConstraint() {
        const c = new FixedConstraint(this.processId, this.symbolic);
        c._comesFrom = this._comesFrom;
        return c;
    }

    usesValue(checkFunction) {
        return this.symbolic.usesValue(checkFunction);
    }
}