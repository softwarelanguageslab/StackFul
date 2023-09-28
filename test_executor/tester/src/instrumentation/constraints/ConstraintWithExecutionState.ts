import Constraint from "./Constraint";
import {ExecutionState} from "@src/tester/ExecutionState";

export default class ConstraintWithExecutionState extends Constraint {

  constructor(processId: number, type: string, protected _executionState: ExecutionState) {
    super(processId, type);
  }
}