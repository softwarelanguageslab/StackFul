import { SymbolicExpression } from "./symbolic_expressions";

export class SymbolicNullObject extends SymbolicExpression {
    n: any;
    
    constructor(n) {
      super("SymbolicNullObject");
      this.n = n;
    }
    toString() {
      return `SymbolicNullObject(${this.n})`;
    }
    isSameSymValue(other) {
      return (other instanceof SymbolicNullObject)
    }
    instantiate(ignored) {
      return new SymbolicNullObject(this.n);
    }
  }
  