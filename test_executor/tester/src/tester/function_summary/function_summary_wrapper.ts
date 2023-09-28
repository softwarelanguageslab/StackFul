import FixedConstraint from "../../instrumentation/constraints/FixedConstraint";

export default class FunctionSummaryWrapper {
  constructor(private _summary, private _argsArray, private _substitutes) {
  }

  _copyArgsArray(argsArray) {
    const copiedArray: any[] = [];
    for (let arg of argsArray) {
      copiedArray.push({base: arg.base, symbolic: arg.symbolic});
    }
    return copiedArray;
  }
  getSummary() {
    return this._summary;
  }
  getArgsArray() {
    return this._copyArgsArray(this._argsArray);
  }
  instantiate(ignored) {
    // jsPrint(`instantiating FSWrapper of function ${this._summary.getFunctionId()} with argsArray`, this.getArgsArray());
    return this.getSummary().instantiate(this._substitutes, this._copyArgsArray(this.getArgsArray()));
  }
  toBranchConstraint(ignored) {
    const c = new FixedConstraint(this._summary.getProcessId(), this.instantiate(this._substitutes));
    c._comesFrom = "FunctionSummaryWrapper";
    return c;
  }
  toString() {
    return `FunctionSummaryWrapper(fid:${this._summary.getFunctionId()})`
  }
}
