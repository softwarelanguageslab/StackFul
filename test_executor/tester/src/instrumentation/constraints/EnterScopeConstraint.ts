import ScopeConstraint from "./ScopeConstraint";

export default class EnterScopeConstraint extends ScopeConstraint {
  constructor(processId: number, variables: string[], scopeId: number) {
    super(processId, "ENTER_SCOPE", variables, scopeId);
  }
}