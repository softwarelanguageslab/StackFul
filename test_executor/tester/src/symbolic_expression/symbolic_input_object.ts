import {Logger as Log} from "../util/logging";
import { IntEqual, StringEqual } from "./operators";
import { SupportedSymbolicExpressionTypes } from "./supported_symbolic_types";
import { SymbolicExpression } from "./symbolic_expressions";

//implementation of symbolic Input object
export class SymbolicInputObject extends SymbolicExpression {
    processId: number;
    id: any;
    equalsToOperator: any;
    objectId: any;
    
    constructor(processId, id, objectId) {
      super("SymbolicInputObject");
      this.processId = processId;
      this.id = id;
      this.equalsToOperator = IntEqual;
      this.objectId = objectId;
    }
    toString() {
      return `SymbolicInputObject(processId ${this.processId}, id ${this.id}, objectId ${this.objectId})`;
    }
    toSymbolicConcreteValue(concreteValue) {
      return { type: SupportedSymbolicExpressionTypes.Int, value: concreteValue, processId: this.processId, id: this.id };
    }
    instantiate(ignored) {
      return new SymbolicInputObject(this.processId, this.id, this.objectId);
    }
    isSameSymValue(other) {
      return (other instanceof SymbolicInputObject) && this.processId === other.processId && this.id === other.id && this.objectId === other.objectId;
    }
  }

/*
 * Constraint repr:
 */ 

//objectConstraintExp
export class SymbolicObjectConstraintExp extends SymbolicExpression {
  left: any;
  operator: any;
  right: any;
  constructor(left, operator, right) {
    super("SymbolicObjectConstraintExp");
    this.left = left;
    this.operator = operator;
    this.right = right;
    if (this.left === undefined || this.right === undefined) {
      Log.ALL("one of either children is undefined, this =", this);
      throw new Error("One of either children is undefined");
    }
  }
  toString() {
    return `(${this.left.toString()} ${this.operator} ${this.right.toString()})`;
  }
  isSameSymValue(other) {
    return (other instanceof SymbolicObjectConstraintExp) && this.operator === other.operator &&
           this.left.isSameSymValue(other.left) && this.right.isSameSymValue(other.right);
  }
  usesValue(checkFunction) {
    return checkFunction(this.left) || checkFunction(this.right);
  }
  instantiate(substitutes) {
    if (! (this.left.instantiate instanceof Function)) {
      Log.ALL("left child does not have an instantiate-method, left =", this.left);
    }
    if (! (this.right.instantiate instanceof Function)) {
      Log.ALL("right child does not have an instantiate-method, right =", this.right);
    }
    return new SymbolicObjectConstraintExp(this.left.instantiate(substitutes), this.operator, this.right.instantiate(substitutes));
  }
}

/*
 * All type of inputFields:
 */

export class SymbolicInputObjectObjectField extends SymbolicExpression {
    processId: number;
    equalsToOperator: string;
    originObjectId: any;
    objectId: any;
    fieldName: any;
    
    constructor(processId, originObjectId, fieldName, objectId) {
      super("SymbolicInputObjectObjectField");
      this.processId = processId;
      this.equalsToOperator = IntEqual;
      this.originObjectId = originObjectId;
      this.objectId = objectId;
      this.fieldName = fieldName;
    }
    toString() {
      return `SymbolicInputObjectObjectField (processId ${this.processId}, originObjectId ${this.originObjectId}, fieldName ${this.fieldName}, objectId ${this.objectId})`;
    }
    instantiate(ignored) {
      return new SymbolicInputObjectObjectField(this.processId, this.originObjectId, this.fieldName, this.objectId);
    }
    isSameSymValue(other) {
      return (other instanceof SymbolicInputObjectObjectField) && this.processId === other.processId && this.originObjectId === other.originObjectId && this.fieldName === other.fieldName && this.objectId === other.objectId;
    }
  }
  
export class SymbolicInputObjectIntField extends SymbolicExpression {
    processId: number;
    equalsToOperator: string;
    originObjectId: any;
    fieldName: any;

    constructor(processId, originObjectId, fieldName) {
      super("SymbolicInputObjectIntField");
      this.processId = processId;
      this.equalsToOperator = IntEqual;
      this.originObjectId = originObjectId;
      this.fieldName = fieldName;
    }
    toString() {
      return `SymbolicInputObjectIntField (processId ${this.processId}, originObjectId ${this.originObjectId}, fieldName ${this.fieldName})`;
    }
    instantiate(ignored) {
      return new SymbolicInputObjectIntField(this.processId, this.originObjectId, this.fieldName);
    }
    isSameSymValue(other) {
      return (other instanceof SymbolicInputObjectIntField) && this.processId === other.processId && this.originObjectId === other.originObjectId && this.fieldName === other.fieldName;
    }
  }
  
export class SymbolicInputObjectStringField extends SymbolicExpression {
    processId: any;
    equalsToOperator: any;
    originObjectId: any;
    fieldName: any;
    constructor(processId, originObjectId, fieldName) {
      super("SymbolicInputObjectStringField");
      this.processId = processId;
      this.equalsToOperator = StringEqual;
      this.originObjectId = originObjectId;
      this.fieldName = fieldName;
    }
    toString() {
      return `SymbolicInputObjectStringField (processId ${this.processId}, originObjectId ${this.originObjectId}, fieldName ${this.fieldName})`;
    }
    instantiate(ignored) {
      return new SymbolicInputObjectStringField(this.processId, this.originObjectId, this.fieldName);
    }
    isSameSymValue(other) {
      return (other instanceof SymbolicInputObjectStringField) && this.processId === other.processId && this.originObjectId === other.originObjectId && this.fieldName === other.fieldName;
    }
  }
  