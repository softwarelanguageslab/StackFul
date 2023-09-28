import Tainter, { dirty } from "../instrumentation/tainter";
import {SymbolicExpression} from "./symbolic_expressions";
import * as SymTypes from "./supported_symbolic_types";
import * as Operators from "./operators"
import {BooleanNot, IntegerInverse, IntEqual, IntNonEqual} from "./operators"
import SymbolicStringExpression from "./symbolic_string_expression";
import SymbolicRegularExpression from "./symbolic_regular_expression";
import SymbolicArithmeticExp from "./symbolic_arithmetic_exp";
import SymbolicRelationalExp from "./symbolic_relational_exp";
import SymbolicLogicalBinExpression from "./symbolic_logical_bin_expression";
import SymbolicUnaryExp from "./symbolic_unary_exp";
import SymbolicInt from "./symbolic_int";
import SymbolicBool from "./symbolic_bool";
import SymbolicString from "./symbolic_string";
import Nothing, {isNothing} from "./nothing";
import { SymbolicObjectConstraintExp } from "./symbolic_input_object";
import { SymbolicNullObject } from "./symbolic_null_object";

function notASymbolic(exp: any): boolean {
  return (exp === undefined) || (exp.symbolic === undefined) || isNothing(exp.symbolic);
}

export function convertBinStringExpToSymbolic(operator: string, string1: any, string2: any): SymbolicExpression {
  const maybeSymbolicString1 = notASymbolic(string1) ? convertPrimToSymbolic(Tainter.cleanAndRelease(string1)) : string1.symbolic;
  const maybeSymbolicString2 = notASymbolic(string2) ? convertPrimToSymbolic(Tainter.cleanAndRelease(string2)) : string2.symbolic;
  if (isNothing(maybeSymbolicString1) || isNothing(maybeSymbolicString2)) {
    return new Nothing();
  } else {
    const symbolicString1 = maybeSymbolicString1;
    const symbolicString2 = maybeSymbolicString2;
    var symbolic;
    switch (operator) {
      case "==": case "===" : symbolic = new SymbolicStringExpression(Operators.StringEqual, [symbolicString1, symbolicString2]);     break;
      case "+"              : symbolic = new SymbolicStringExpression(Operators.StringAppend, [symbolicString1, symbolicString2]);    break;
      default               : symbolic = new Nothing();
    }
    return symbolic;
  }
}
export function convertStringInvocationToSymbolic(object: dirty, methodName: string, args: any[]): SymbolicExpression {
  const maybeSymbolicObject = notASymbolic(object) ? convertPrimToSymbolic(Tainter.cleanAndRelease(object)) : object.symbolic;
  var maybeSymbolicArgs = new Array(args.length);
  for(let i = 0; i < args.length; i++) {
    maybeSymbolicArgs[i] = notASymbolic(args[i]) ? convertPrimToSymbolic(Tainter.cleanAndRelease(args[i])) : args[i].symbolic;
  }
  if (isNothing(maybeSymbolicObject)) {
    return new Nothing();
  } else {
    const symbolicObject = maybeSymbolicObject;
    const symbolicArgs = maybeSymbolicArgs;
    var symbolic;
    switch (methodName) {
      case "indexOf"      : symbolic = new SymbolicStringExpression(Operators.StringIndexOf, [symbolicObject, symbolicArgs[0]]);                      break;
      case "substring"    : symbolic = new SymbolicStringExpression(Operators.StringSubstring, [symbolicObject, symbolicArgs[0], symbolicArgs[1]]);   break;
      case "includes"     : symbolic = new SymbolicStringExpression(Operators.StringIncludes, [symbolicObject, symbolicArgs[0]]);                     break;
      case "replace"      : symbolic = new SymbolicStringExpression(Operators.StringReplace, [symbolicObject, symbolicArgs[0], symbolicArgs[1]]);     break;
      case "charAt"       : symbolic = new SymbolicStringExpression(Operators.StringAt, [symbolicObject, symbolicArgs[0]]);                           break;
      case "concat"       : symbolic = new SymbolicStringExpression(Operators.StringAppend, [symbolicObject, symbolicArgs[0]]);                       break;
      case "startsWith"   : symbolic = new SymbolicStringExpression(Operators.StringPrefix, [symbolicArgs[0], symbolicObject]);                       break;
      case "endsWith"     : symbolic = new SymbolicStringExpression(Operators.StringSuffix, [symbolicArgs[0], symbolicObject]);                       break;
      case "length"       : symbolic = new SymbolicStringExpression(Operators.StringLength, [symbolicObject]);                                        break;
      case "test"         : symbolic = new SymbolicRegularExpression(Operators.RegExpTest, symbolicArgs, symbolicObject);                             break;
      default             : symbolic = new Nothing();
    }
    return symbolic;
  }
}
export function convertRegExpConstructToSymbolic(constructor, array$$value) {
  var maybeSymbolicArgs = new Array;
  for(let i = 0; i < array$$value.length; i++) {
    maybeSymbolicArgs.push(notASymbolic(array$$value[i]) ? convertPrimToSymbolic(Tainter.cleanAndRelease(array$$value[i])) : array$$value[i].symbolic);
  }
  const symbolicArgs = maybeSymbolicArgs;
  var symbolic;
  switch (constructor.name) {
    case "RegExp" : symbolic = new SymbolicRegularExpression(Operators.NewRegExp, symbolicArgs, "");   break;
    default       : symbolic = new Nothing();
  }
  return symbolic;
}

export function convertBinExpToSymbolic(operator: string, $$left: any, $$right: any): SymbolicExpression {
  // const maybeSymbolicLeft = notASymbolic($$left) ? convertPrimToSymbolic(Tainter.cleanAndRelease($$left)) : $$left.symbolic;
  // const maybeSymbolicRight = notASymbolic($$right) ? convertPrimToSymbolic(Tainter.cleanAndRelease($$right)) : $$right.symbolic;



  if (notASymbolic($$left) || notASymbolic($$right)) {
    return new Nothing();
  }

  const maybeSymbolicLeft = $$left.symbolic;
  const maybeSymbolicRight = $$right.symbolic;
  /* 
   * If the operands were not symbolic values, try to convert them 
   */
  if (isNothing(maybeSymbolicLeft) || isNothing(maybeSymbolicRight)) {
    return new Nothing();
  } else {
    const symbolicLeft = maybeSymbolicLeft;
    const symbolicRight = maybeSymbolicRight;
    var symbolic: SymbolicExpression = new Nothing();
    switch (operator) {
      case "+":   symbolic = new SymbolicArithmeticExp(Operators.IntPlus, [symbolicLeft, symbolicRight]);           break;
      case "-":   symbolic = new SymbolicArithmeticExp(Operators.IntMinus, [symbolicLeft, symbolicRight]);          break;
      case "*":   symbolic = new SymbolicArithmeticExp(Operators.IntTimes, [symbolicLeft, symbolicRight]);          break;
      case "/":   symbolic = new SymbolicArithmeticExp(Operators.IntDiv, [symbolicLeft, symbolicRight]);            break;
      case ">":   symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntGreaterThan, symbolicRight);      break;
      case ">=":  symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntGreaterThanEqual, symbolicRight); break;
      case "<":   symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntLessThan, symbolicRight);         break;
      case "<=":  symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntLessThanEqual, symbolicRight);    break;
      case "==":  symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntEqual, symbolicRight);            break;
      case "===": symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntEqual, symbolicRight);            break;
      case "!=":  symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntNonEqual, symbolicRight);         break;
      case "!==": symbolic = new SymbolicRelationalExp(symbolicLeft, Operators.IntNonEqual, symbolicRight);         break;
      case "&&":  symbolic = new SymbolicLogicalBinExpression(symbolicLeft, Operators.BooleanAnd, symbolicRight);   break;
      case "&":   symbolic = new SymbolicLogicalBinExpression(symbolicLeft, Operators.BooleanAnd, symbolicRight);   break;
      case "||":  symbolic = new SymbolicLogicalBinExpression(symbolicLeft, Operators.BooleanOr, symbolicRight);    break;
      case "|":   symbolic = new SymbolicLogicalBinExpression(symbolicLeft, Operators.BooleanOr, symbolicRight);    break;
    }
    return symbolic;
  }
}
export function convertUnaryExpToSymbolic(operator: string, $$argument: any): SymbolicExpression {
  // const maybeSymbolicArg = notASymbolic($$argument) ? convertPrimToSymbolic(Tainter.cleanAndRelease($$argument)) : $$argument.symbolic;
  if (notASymbolic($$argument)) {
    return new Nothing();
  }
  const maybeSymbolicArg = $$argument.symbolic;
  if (isNothing(maybeSymbolicArg)) {
    return new Nothing();
  } else {
    const symbolicArg = maybeSymbolicArg;
    var symbolic: SymbolicExpression = new Nothing();
    switch (operator) {
      case "!":   symbolic = new SymbolicUnaryExp(BooleanNot, symbolicArg); break;
      case "-":   symbolic = new SymbolicUnaryExp(IntegerInverse, symbolicArg); break;
    }
    return symbolic;
  }
}
export function convertPrimToSymbolic(prim: any): SymbolicExpression {
  const MAX_INT = 2147483647;
  const MIN_INT = -2147483648;
  const type = SymTypes.determineType(prim);
  if (type === SymTypes.SupportedSymbolicExpressionTypes.Int && prim <= MAX_INT && prim >= MIN_INT) {
    return new SymbolicInt(prim);
  } else if (prim === null) {
    return new Nothing();
  } else {
    switch (type) {
      case SymTypes.SupportedSymbolicExpressionTypes.Boolean: return new SymbolicBool(prim);
      case SymTypes.SupportedSymbolicExpressionTypes.String: return new SymbolicString(prim);
      default: return new Nothing();
    }
  }
}

export function convertObjectConstraintExpToSymbolic(operator: string, $$left: any, $$right: any): SymbolicExpression {
  const left = Tainter.cleanAndRelease($$left);
  const right = Tainter.cleanAndRelease($$right);
  const maybeSymbolicLeft = notASymbolic($$left) ? convertPrimToSymbolic(left) : $$left.symbolic;
  const maybeSymbolicRight = notASymbolic($$right) ? convertPrimToSymbolic(right) : $$right.symbolic;

  if (isNothing(maybeSymbolicLeft) || isNothing(maybeSymbolicRight) || !(SymTypes.isObject(left) && SymTypes.isObject(right))) {
    console.log("succesfull prevented wrong object constraint");
    return new Nothing();
  } else {
    const symbolicLeft = maybeSymbolicLeft;
    const symbolicRight = maybeSymbolicRight;
    var symbolic: SymbolicExpression = new Nothing();
    switch (operator) {
      case "==":  symbolic = new SymbolicObjectConstraintExp(symbolicLeft, IntEqual, symbolicRight);            break;
      case "===": symbolic = new SymbolicObjectConstraintExp(symbolicLeft, IntEqual, symbolicRight);            break;
      case "!=":  symbolic = new SymbolicObjectConstraintExp(symbolicLeft, IntNonEqual, symbolicRight);         break;
      case "!==": symbolic = new SymbolicObjectConstraintExp(symbolicLeft, IntNonEqual, symbolicRight);         break;
    }
    return symbolic;
  }
}
