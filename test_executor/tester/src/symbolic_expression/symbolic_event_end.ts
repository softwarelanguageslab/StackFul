import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicEventEnd extends SymbolicExpression {
  constructor(protected id: number, protected processIdChosen: number, protected eventIdChosen: number) {
    super("SymbolicEventEnd");
  }
  toString() {
    return `SymbolicEventEnd(${this.id}, ${this.processIdChosen}, ${this.eventIdChosen})`;
  }
  isSameSymValue(other): boolean {
    return (other instanceof SymbolicEventEnd) &&
           this.id === other.id &&
           this.processIdChosen === other.processIdChosen &&
           this.eventIdChosen === other.eventIdChosen;
  }
  usesValue(checkFunction): boolean {
    return false;
  }
  instantiate(ignored): SymbolicEventEnd { 
    return new SymbolicEventEnd(this.id, this.processIdChosen, this.eventIdChosen);
  }
}