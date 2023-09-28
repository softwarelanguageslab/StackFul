import AssignmentConstraint from "./AssignmentConstraint";
import EnterScopeConstraint from "./EnterScopeConstraint";
import ExitScopeConstraint from "./ExitScopeConstraint";
import SymbolicEventChosen from "../../symbolic_expression/symbolic_event_chosen";
import TargetConstraint from "./TargetConstraint";
import TargetEndConstraint from "./TargetEndConstraint";
import ExecutionStateCollector from "../../tester/ExecutionStateCollector";

export function newAssignmentConstraint(processId: number, identifier: string, symbolic) {
	return new AssignmentConstraint(processId, identifier, symbolic);
}

export function newEnterScopeConstraint(processId: number, variables: string[], scopeId: number) {
	return new EnterScopeConstraint(processId, variables, scopeId);
}

export function newExitScopeConstraint(processId: number, variables: string[], scopeId: number) {
	return new ExitScopeConstraint(processId, Array.from(variables).flat(), scopeId);
}

export function newTargetConstraint(processId: number, eventChosen: SymbolicEventChosen) {
	const executionState = ExecutionStateCollector.getExecutionState(processId);
  return new TargetConstraint(eventChosen, executionState);
}

export function newTargetEndConstraint(processId: number, id: number, targetId: number) {
	const executionState = ExecutionStateCollector.getExecutionState(processId);
  return new TargetEndConstraint(processId, id, targetId, executionState);
}