import {SymbolicExpression} from "@src/symbolic_expression/symbolic_expressions";
import {ExecutionState} from "@src/tester/ExecutionState.js";

export default abstract class SymbolicInput extends SymbolicExpression {

  protected constructor(type: string, public readonly processId: number,
                        public readonly executionState: ExecutionState,
                        public readonly id: number,
                        public readonly equalsToOperator: string) {
    super(type);
    this.processId = processId;
    console.log("Creating symbolic input with execution state", executionState.toString());
  }

}