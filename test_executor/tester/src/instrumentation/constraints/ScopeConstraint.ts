import Constraint from "./Constraint";

export default class ScopeConstraint extends Constraint {
  constructor(processId: number, constraintType: string, protected _variables: string[], protected _scopeId: number) {
    super(processId, constraintType);
  }
  getVariables(): string[] {
    return this._variables.slice();
  }
  getScopeId(): number {
    return this._scopeId;
  }
}