import ScopeConstraint from "./ScopeConstraint";

export default class ExitScopeConstraint extends ScopeConstraint {
  constructor(processId: number, variables: string[], scopeId: number) {
    super(processId, "EXIT_SCOPE", variables, scopeId);
  }
}