import * as SymTypes from "./supported_symbolic_types";
import * as Operators from "./operators";
import { SymbolicExpression } from "./symbolic_expressions";

export default class SymbolicFunction extends SymbolicExpression {
  constructor(public environment: any, public serial: number) {
    super("SymbolicFunction");
    this.environment = environment;
  }
  toString(): string {
    return `SymbolicFunction(${this.serial}`;
  }
  isSameSymValue(): boolean {
    return false;
  }
  usesValue(checkFunction): boolean {
    return false;
  }
  instantiate(substitutes): SymbolicFunction {
    return new SymbolicFunction(this.environment, this.serial);
  }
  toJSON(){
    return {type: this.type, serial: this.serial, _identifier: this._identifier};
  }
}