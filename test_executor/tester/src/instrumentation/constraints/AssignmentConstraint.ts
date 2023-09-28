import Constraint from "./Constraint";

export default class AssignmentConstraint extends Constraint {
  constructor(processId: number, protected _identifier: string, protected _symbolic) {
    super(processId, "ASSIGNMENT");
  }
  get getIdentifier(): string {
    return this._identifier;
  }
  get getSymbolic() {
    return this._symbolic;
  }
}