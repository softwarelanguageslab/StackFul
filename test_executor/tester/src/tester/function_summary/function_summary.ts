import {jsPrint} from "../../util/io_operations";
import Chalk from "chalk";
import Util from "util";
import * as GTS from "../global_tester_state";
import {Logger as Log} from "../../util/logging";
import * as SymTypes from "../../symbolic_expression/supported_symbolic_types";
import tainter from "../../instrumentation/tainter";
import FixedConstraint from "../../instrumentation/constraints/FixedConstraint";
import SymbolicRelationalExp from "../../symbolic_expression/symbolic_relational_exp";
import SymbolicLogicalBinExpression from "../../symbolic_expression/symbolic_logical_bin_expression";
import {BooleanAnd, BooleanOr} from "../../symbolic_expression/operators";
import SymbolicBool from "../../symbolic_expression/symbolic_bool";
import Nothing, {isNothing} from "../../symbolic_expression/nothing";
import {symbolicIDGenerator} from "../../symbolic_expression/symbolic_id_generator";

class FunctionArgumentsMapper {
  private readonly _convertedArguments: any;
  private readonly _nrOfArgs: any;
  private readonly _nrOfValidArgs: any;

  constructor(functionArgsArray, processId, functionId) {
    this._convertedArguments = functionArgsArray.map(($$arg) => this._argToSymbolicExp(tainter.cleanAndRelease($$arg), processId, functionId));
    this._nrOfArgs = functionArgsArray.length;
    this._nrOfValidArgs = this.getAllArgs().length;
  }
  _argToSymbolicExp(arg, processId, functionId) {
    switch (SymTypes.determineType(arg)) {
      // When creating a new function summary, the timesCalled-counter of this function should always be 1
      case SymTypes.SupportedSymbolicExpressionTypes.Boolean: return symbolicIDGenerator.newFunctionSummaryBooleanInput(processId, functionId, 1);
      case SymTypes.SupportedSymbolicExpressionTypes.Int: return symbolicIDGenerator.newFunctionSummaryIntInput(processId, functionId, 1);
      case SymTypes.SupportedSymbolicExpressionTypes.String: return symbolicIDGenerator.newFunctionSummaryStringInput(processId, functionId, 1);
      default: return new Nothing();
    }
  }
  getArg(idx) {
    return this._convertedArguments[idx];
  }
  // The arguments belonging only to this function, with unsupported expression types removed.
  getAllArgs() {
    return this.getUnfilteredArgs().filter((arg) => ! isNothing(arg));
  }
  getUnfilteredArgs() {
    return this._convertedArguments.slice();
  }
  getNrOfArgs() {
    return this._nrOfArgs;
  }
  getNrOfValidArgs() {
    return this._nrOfValidArgs;
  }
}

class AllArgsMapper {
  private readonly _arguments: any;
  constructor(functionArgumentsWrapper) {
    this._arguments = functionArgumentsWrapper.getAllArgs();
  }
  getAllArgs() {
    return this._arguments;
  }
  addArg(newArg) {
    if (! this.getAllArgs().some((arg) => arg.isSameSymValue(newArg))) {
      this.getAllArgs().push(newArg);
    }
  }
  addArgs(newArgs) {
    for (let newArg of newArgs) {
      this.addArg(newArg);
    }
  }
}

function syncParameters(functionId, theArguments, parameters): FixedConstraint[] {
  jsPrint("syncParameters 1");
  const syncedParametersArray: FixedConstraint[] = [];
  jsPrint(`argsArray.length = ${theArguments.length}, this._nrOfArgs = ${parameters.length}`);
  if (theArguments.length !== parameters.length) {
    const message = `Number of arguments does not match for function ${functionId}: received ${theArguments.length} arguments, but expected ${parameters.length} arguments`;
    jsPrint(message);
    throw new Error(message);
  }
  for (let i in theArguments) {
    const par = parameters[i]; // Should be a FunctionInput
    const arg = theArguments[i];
    if (isNothing(arg) && isNothing(par)) {
      continue;
    } else if (isNothing(arg)) {
      const message = `Argument type mismatch for arg ${i} of function ${functionId}: expected arg ${arg} to be unsupported`;
      jsPrint(message);
      throw new Error(message);
    } else if (isNothing(par)) {
      const message = `Argument type mismatch for arg ${i} of function ${functionId}: expected arg ${arg} to be supported as a ${par}`;
      jsPrint(message);
      throw new Error(message);
    } else {
      const constraint = new FixedConstraint(functionId, new SymbolicRelationalExp(par, par.equalsToOperator, arg));
      syncedParametersArray.push(constraint);
    }
  }
  return syncedParametersArray;
}

class PathSummary {
  protected _preCondition: any;
  protected _pathConditions: any;

  constructor(protected _functionId, preCondition, pathConditions, protected _returnExpression) {
    this._preCondition = preCondition.slice();
    if (pathConditions.length === 0) {
      jsPrint(Chalk.bgRed("Should not happen, pathConditions are empty"));
    }
    this._pathConditions = pathConditions.slice();
  }

  getPreCondition() {
    return this._preCondition;
  }
  getPathConditions() {
    return this._pathConditions;
  }
  getReturnExpression() {
    return this._returnExpression;
  }

  argumentsMatch(backend, instancedArgs, correspondingSymbolicInputs, substitutes) {
    Log.PSI("Checking path summary: preCondition:", JSON.stringify(this._preCondition),
            "\npathConditions:", JSON.stringify(Util.inspect(this._pathConditions)),
            "\nconcreteArguments:", JSON.stringify(instancedArgs),
            "\ncorrespondingSymbolicInputs:", JSON.stringify(correspondingSymbolicInputs));
    const result = backend.checkSatisfiesConstraints(this._functionId, this._preCondition.map((constraint) => constraint.toBranchConstraint(substitutes)),
                                                     this._pathConditions.map((constraint) => constraint.toBranchConstraint(substitutes)),
                                                     instancedArgs, correspondingSymbolicInputs, GTS.globalTesterState!.getGlobalInputs());
    return result;
  }

  instantiate(substitutes) {
    const that = this;
    function makeOutline() {
      let addTo: any = undefined;
      that._pathConditions.forEach((branchConstraint) => {
        const exp = branchConstraint.instantiate(substitutes);
        if (addTo === undefined) {
          addTo = exp;
        } else {
          addTo = new SymbolicLogicalBinExpression(addTo, BooleanAnd, exp);
        }
      });
      if (addTo === undefined) {
        return that.getReturnExpression();
      } else {
        return new SymbolicLogicalBinExpression(addTo, BooleanAnd, that.getReturnExpression());
      }
    }
    return makeOutline().instantiate(substitutes);
  }

  usesValue(checkFunction) {
    return this._pathConditions.some((summary) => summary.usesValue(checkFunction)) ||
           this._preCondition.some((summary) => summary.usesValue(checkFunction));
  }
}

export default class FunctionSummary {
  private _inputs = null;
  private _inputsOutputsMap = new Map();
  private _pathSummaries: any[] = [];
  private readonly _ownArgs: FunctionArgumentsMapper;
  private _allArgs: AllArgsMapper;
  private _nrOfArgs: any;
  private _nrOfValidArgs: any;

  constructor(protected _processId, protected _functionName, functionArgsArray, protected _functionId) {
    this._ownArgs = new FunctionArgumentsMapper(functionArgsArray, _processId, _functionId);
    this._allArgs = new AllArgsMapper(this._ownArgs);
    this._nrOfArgs = this._ownArgs.getNrOfArgs();
    this._nrOfValidArgs = this._ownArgs.getNrOfValidArgs();
  }

  toString() {
    return `FS(id: ${this.getFunctionId()}, allArgs: ${this.getAllArgs()}, paths: ${JSON.stringify(Util.inspect(this._pathSummaries, {depth: 4}))})`;
  }
  
  getProcessId() {
    return this._processId;
  }
  getFunctionId() {
    return this._functionId;
  }
  getArg(i) {
    return this._ownArgs.getArg(i);
  }
  getOwnArgs() {
    return this._ownArgs.getAllArgs();
  }
  getAllArgs() {
    return this._allArgs.getAllArgs();
  }
  getFunctionName() {
    return this._functionName;
  }

  addArg(newArg) {
    this._allArgs.addArg(newArg);
  }
  addArgs(newArgs) {
    this._allArgs.addArgs(newArgs);
  }

  addPathSummary(preCondition, pathConditions, returnExpression) {
    const pathSummary = new PathSummary(this.getFunctionId(), preCondition, pathConditions, returnExpression);
    this._pathSummaries.push(pathSummary);
  }

  satisfiesAnyInputs(functionSummariesBackend, args, substitutes?) {
    let idx = 0;
    jsPrint(`Function ${this._functionId} has ${this._pathSummaries.length} path summaries`);
    const instancedArgs = args;
    for (let pathSummary of this._pathSummaries) {
      idx++;
      jsPrint(`Checking path summaries for function ${this._functionId} for the ${idx}th time`);
      // The result of the sync is discarded. The sync is only used to check whether both arrays might match.
      syncParameters(this._functionId, instancedArgs, this._ownArgs.getUnfilteredArgs());
      const isSatisfied = pathSummary.argumentsMatch(functionSummariesBackend, instancedArgs, this.getOwnArgs(), substitutes);
      if (isSatisfied) {
        jsPrint(Chalk.green("satisfies inputs"));
        return true;
      }
    }
    jsPrint(Chalk.red("does not satisfy inputs"));
    return false;
  }

  _toAndExp(array) {
    let addTo: any = undefined;
    array.forEach((exp) => {
      if (addTo === undefined) {
        addTo = exp;
      } else {
        addTo = new SymbolicLogicalBinExpression(addTo, BooleanAnd, exp);
      }
    });
    if (addTo === undefined) {
      return new SymbolicBool(true);
    } else {
      return addTo;
    }
  }

  _syncParameters(argsArray) {
    const syncedParameters = syncParameters(this._functionId, argsArray.map((arg) => arg.symbolic), this._ownArgs.getUnfilteredArgs());
    return this._toAndExp(syncedParameters);
  }

  instantiate(substitutes, argsArray) {
    let addTo: any = undefined;
    this._pathSummaries.forEach((pathSummary) => {
      if (addTo === undefined) {
        addTo = pathSummary.instantiate(substitutes);
      } else {
        addTo = new SymbolicLogicalBinExpression(addTo, BooleanOr, pathSummary.instantiate(substitutes));
      }
    });
    if (argsArray) {
      jsPrint("FS.instantiate 1");
      const syncedParametersExp = this._syncParameters(argsArray);
      return new SymbolicLogicalBinExpression(syncedParametersExp, BooleanAnd, addTo).instantiate(substitutes);
    } else {
      return addTo.instantiate(substitutes);
    }    
  }

  usesValue(checkFunction) {
    return this._pathSummaries.some((pathSummary) => pathSummary.usesValue(checkFunction));
  }
}
