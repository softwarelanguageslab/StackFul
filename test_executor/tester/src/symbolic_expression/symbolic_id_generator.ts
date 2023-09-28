import SymbolicReturnValue from "./symbolic_return_value";
import SymbolicFunctionInput from "./symbolic_function_input";
import * as SymTypes from "./supported_symbolic_types";
import SymbolicInputInt from "./symbolic_input_int";
import SymbolicInputString from "./symbolic_input_string";
import { SymbolicInputObject, SymbolicInputObjectObjectField } from "./symbolic_input_object";
import ExecutionStateCollector from "@src/tester/ExecutionStateCollector.js";
import {ExecutionState} from "@src/tester/ExecutionState.js";

/*
  Keeps track of incrementally generated ids per process and provides factory function
  for creating symbolic expressions with these generated ids
 */
export class SymbolicIdGenerator {
    // Maps process ids to a new collection of execution states to input ids
    private _ids: Map<number, Map<string, number>> = new Map();
    private _functionIds!: Map<string, number>;
    logicalAddressToObjectMap!: Map<any, any>;
    specificObjectIds!: Set<unknown>;
    objectIds!: any[];

    constructor() {
        this.resetIds();
    }

    resetIds(): void {
        this.logicalAddressToObjectMap = new Map();
        this.logicalAddressToObjectMap.set(0, null);
        this.specificObjectIds = new Set();
        this.objectIds = [];
        this._ids = new Map();
        this._functionIds = new Map();
    };

    /**
     * Generates or increments last id for processId
     * @param processId
     * @param executionState
     * @private
     * @return newly generated id
     */
    private newId(processId: number, executionState: ExecutionState): number {
      const existingMap = this._ids.get(processId);
      const key = executionState.toString();
      if (existingMap === undefined) {
          let newMap = new Map<string, number>();
          newMap.set(key, 1);
          this._ids.set(processId, newMap);
          return 0;
        } else {
          let optEntry = existingMap.get(key);
          let currentId = (optEntry === undefined) ? 0 : optEntry;
          existingMap.set(key, currentId + 1);
          return currentId;
        }
    };

    newSpecificObjectId(processId, objectId) {
        this.specificObjectIds.add(objectId);
        return objectId;
      }

      newObjectId(processId) {
        let current = (this.objectIds[processId] === undefined) ? 0 : this.objectIds[processId];
        while (this.specificObjectIds.has(current)) {
          current += 1;
        }
        this.objectIds[processId] = current + 1;
        console.log(current);
        return current;
      };

      addInputObject(logicalAddress) {
        var objectInMap = this.logicalAddressToObjectMap.get(logicalAddress);
        var existingObject = true; //the object is acquired from the map, an already existing object is thus returned
        if (objectInMap === undefined) {
          existingObject = false;
          const newObject = {};
          console.log("new object in map");
          this.logicalAddressToObjectMap.set(logicalAddress, newObject);
          objectInMap = newObject;
        }
        return {resultObject:objectInMap, existingObject: existingObject};
      }

    /**
     * Forms a key using processId and functionId, looks up the key in the functionId map and
     * generates and stores a new Id by incrementing it with 1 (0 if none)
     * @param processId
     * @param functionId
     * @private
     * @return new Id associated with processId__functionId
     */
    private _addFunctionId(processId, functionId): number {
        const key = processId + "__" + functionId;
        const previousId: number  = this._functionIds.has(key) ? this._functionIds.get(key)! : -1;
        const newId = previousId + 1;
        this._functionIds.set(key, newId);
        return newId;
    }

    newFunctionReturn(processId, functionId, value, timesCalled) {
        return new SymbolicReturnValue(processId, functionId, value, timesCalled);
    }

    newFunctionSummaryBooleanInput(processId, functionId, initialTimesCalled) {
        const newId = this._addFunctionId(processId, functionId);
        return new SymbolicFunctionInput(processId, functionId, newId, initialTimesCalled, SymTypes.SupportedSymbolicExpressionTypes.Boolean);
    }

    newFunctionSummaryIntInput(processId, functionId, initialTimesCalled) {
        const newId = this._addFunctionId(processId, functionId);
        return new SymbolicFunctionInput(processId, functionId, newId, initialTimesCalled, SymTypes.SupportedSymbolicExpressionTypes.Int);
    }

    newFunctionSummaryStringInput(processId, functionId, initialTimesCalled) {
        const newId = this._addFunctionId(processId, functionId);
        return new SymbolicFunctionInput(processId, functionId, newId, initialTimesCalled, SymTypes.SupportedSymbolicExpressionTypes.String);
    }

    newSymbolicInput(processId): SymbolicInputInt {
        const executionState = ExecutionStateCollector.getExecutionState(processId).duplicate();
        console.log("Created new symbolic input int with execution state", executionState);
        return new SymbolicInputInt(processId, executionState, this.newId(processId, executionState));
    };

    newSymbolicInputString(processId): SymbolicInputString {
        const executionState = ExecutionStateCollector.getExecutionState(processId).duplicate();
        console.log("Created new symbolic input string with execution state", executionState);
        return new SymbolicInputString(processId, executionState, this.newId(processId, executionState));
    };

    newSymbolicInputObject(processId) {
        const executionState = ExecutionStateCollector.getExecutionState(processId).duplicate();
        return new SymbolicInputObject(processId, this.newId(processId, executionState), this.newObjectId(processId));
      };

      newSymbolicInputObjectObjectField(processId, originObjectId, fieldName) {
        return new SymbolicInputObjectObjectField(processId, originObjectId, fieldName, this.newObjectId(processId))
      };

}

export const symbolicIDGenerator = new SymbolicIdGenerator();

