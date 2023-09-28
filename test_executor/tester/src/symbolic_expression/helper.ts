import Log from "../util/logging";
import {symbolicIDGenerator} from "./symbolic_id_generator";
import ConfigReader from "../config/user_argument_parsing/Config";
import Chalk from "chalk";
import SymbolicRelationalExp from "./symbolic_relational_exp";
import * as Operators from "./operators";
import { SymbolicInputObjectIntField, SymbolicInputObjectObjectField, SymbolicInputObjectStringField } from "./symbolic_input_object";
import Tainter from "../instrumentation/tainter";
import SymbolicInput from "@src/symbolic_expression/SymbolicInput";
import Process from "@src/process/processes/Process";
import {ExecutionState} from "@src/tester/ExecutionState.js";
import ComputedInput from "@src/backend/ComputedInput.js";
import ExtendedMap from "@src/util/datastructures/ExtendedMap.js";
import {SupportedSymbolicExpressionType} from "@src/symbolic_expression/supported_symbolic_types.js";

export interface SymbolicInputWithConcreteValue {
  concrete: any;
  symbolic: SymbolicInput;
}

function makeSymbolicInputValue(process, generateSymbolicInput: (number) => SymbolicInput, generateAlternative: () => SupportedSymbolicExpressionType): SymbolicInputWithConcreteValue {
    const processInputValues: ExtendedMap<String, ComputedInput[]> = process.getInputs();
    const symbolic = generateSymbolicInput(process.processId);
    const newConcrete = readInputValue(generateAlternative, processInputValues, symbolic.executionState, symbolic.id);
    const result = {concrete: newConcrete, symbolic: symbolic};
    return result;
}

export function makeRandomValue(process: Process, maxValue: number): SymbolicInputWithConcreteValue {
    function generateRandomNumber() {
        const defaultValue = 0; // If the ENABLE_TESTING flag is true, return a deterministic value (0) instead of a non-deterministic one.
        const r = (ConfigReader.config.ENABLE_TESTING) ? defaultValue : Math.floor(Math.random() * maxValue) + 1;
        Log.NDP("Using random concrete value for symbolic input parameter:", r);
        return r;
    }

    function generateSymbolicInputInt(processId: number): SymbolicInput {
        return symbolicIDGenerator.newSymbolicInput(processId);
    }

    const result = makeSymbolicInputValue(process, generateSymbolicInputInt, generateRandomNumber);
    return result;
}

export function generateInputString(process) {
    function generateEmptyString() {
        return "";
    }

    function generateSymbolicInputString(processId) {
        return symbolicIDGenerator.newSymbolicInputString(processId);
    }

    const result = makeSymbolicInputValue(process, generateSymbolicInputString, generateEmptyString);
    return result;
}

function readInputValue(generateAlternative: () => SupportedSymbolicExpressionType,
                        inputValues: ExtendedMap<String, ComputedInput[]>,
                        executionState: ExecutionState,
                        currentSymbolicInputId: number): SupportedSymbolicExpressionType {
    const inputs = inputValues.get(executionState.toString());
    let result: SupportedSymbolicExpressionType | undefined = undefined;
    const firstInput = inputs?.shift();
    if (inputs === undefined) {
      Log.ALL(Chalk.red("No value precomputed for", executionState));
      result = generateAlternative();
    } else if (firstInput === undefined) {
      Log.ALL(Chalk.red("No value precomputed for", executionState));
      result = generateAlternative();
    } else if (firstInput.id > currentSymbolicInputId) {
        result = generateAlternative();
        inputs.unshift(firstInput);
    } else if (firstInput.id < currentSymbolicInputId) {
      Log.ALL(Chalk.red(`SHOULD NOT HAPPEN: ${firstInput.id}, ${currentSymbolicInputId}`));
      throw new Error();
    } else {
      result = firstInput.value;
      Log.NDP("Using precomputed symbolic input parameter:", result);
    }
    return result;
}

function readInputObjectValue(generateAlternative, inputValues, fieldInputValues, currentSymbolicInputId, objectId) {
    const maybeHead = inputValues.shift();
    var result: any = undefined;
    var addedNewObject = false; //did we generate an object based on a computed logical address --> addInputObject or did we generate an alternative (addInputObject = true) 
    var computedLogicalAddress = 0; //just some default value
    if (maybeHead === undefined) {
      Log.ALL(Chalk.red("No value precomputed for", currentSymbolicInputId));
      result = generateAlternative();
    } else if (maybeHead.id > currentSymbolicInputId) {
      result = generateAlternative();
      inputValues.unshift(maybeHead);
    } else if (maybeHead.id < currentSymbolicInputId) {
      Log.ALL(Chalk.red(`SHOULD NOT HAPPEN: ${maybeHead.id}, ${currentSymbolicInputId}`));
      throw new Error();
    } else {
      addedNewObject = true;
      computedLogicalAddress = maybeHead.value;
      const {resultObject, existingObject} = symbolicIDGenerator.addInputObject(computedLogicalAddress);
      //checkUnsatisfiable(existingObject, computedLogicalAddress, fieldInputValues);
      result = resultObject;
      Log.NDP("converting logicalAddress: ", computedLogicalAddress);
      Log.NDP("to actual object being", result);
    }
  
    var toDoArray: any = [];
  
    function applyField(obj) { //the latter is for cascading objectfield initialisations (a = b and b.name = 1) --> a.name = 1
      if (('originObjectId' in obj && objectId === obj.originObjectId) || (obj.processId !== -1 && addedNewObject == true && computedLogicalAddress == obj.originObjectlogicalAddress)) {
        
        if (obj.fieldType === "int") {
          Log.ALL(Chalk.magenta("adding object INT field"));
          result[obj.fieldName]= Tainter.taintAndCapture(obj.value, new SymbolicInputObjectIntField(obj.processId, obj.originObjectId, obj.fieldName));
      
        } else if (obj.fieldType === "string") {
          Log.ALL(Chalk.magenta("adding object STRING field"));
          result[obj.fieldName]= Tainter.taintAndCapture(obj.value, new SymbolicInputObjectStringField(obj.processId, obj.originObjectId, obj.fieldName));
        
        } else if (obj.fieldType === "object") {
          Log.ALL(Chalk.magenta("adding object OBJECT field"));
          const {resultObject, existingObject} = symbolicIDGenerator.addInputObject(obj.value);
          //checkUnsatisfiable(existingObject, computedLogicalAddress, fieldInputValues);
          const concrete = resultObject;
          const symbolic = new SymbolicInputObjectObjectField(obj.processId, obj.originObjectId, obj.fieldName, symbolicIDGenerator.newSpecificObjectId(obj.processId,obj.objectId));
          result[obj.fieldName]= Tainter.taint(concrete, symbolic); //taint and no capture
          toDoArray.unshift(result[obj.fieldName]); //now we also have to apply fields of this new objectfield
          toDoArray.unshift(obj.value);
        }
        obj.processId = -1; //trick to indicate that we are done with this field
      } else {
        Log.ALL(Chalk.magenta("field does not relate with this object"));
      }
    }
  
    if (result !== null) {
      fieldInputValues.forEach(applyField);
    } else {
      Log.ALL(Chalk.magenta("wanted to apply field to null object"));
    }
    const endResult = result
    while (toDoArray.length !== 0) {
      Log.ALL(Chalk.magenta("applyField to cascading objectField of Object"));
      var objectField = toDoArray.pop();
      result = objectField.base;
      computedLogicalAddress = toDoArray.pop();
      objectId = objectField.symbolic.objectId;
      addedNewObject = true;
      if (result !== null) {
        fieldInputValues.forEach(applyField);
      } else {
        Log.ALL(Chalk.magenta("wanted to apply field to null objectField"));
      }
    }
    return endResult;
  }

export function generateInputObjectField(process, concreteObject, symbolicObject, fieldName) {
  var symbolic = new SymbolicInputObjectIntField(process.processId, symbolicObject.objectId, fieldName);
  var objectBoolean = false;

  switch ((fieldName.substring(0,4)).toLowerCase()) {
      case "str_":
        symbolic = new SymbolicInputObjectStringField(process.processId, symbolicObject.objectId, fieldName);
        if (concreteObject[fieldName] === undefined) {
          concreteObject[fieldName] = "";
        }
        break;
      case "int_":
        symbolic = new SymbolicInputObjectIntField(process.processId, symbolicObject.objectId, fieldName);
        if (concreteObject[fieldName] === undefined) {
          concreteObject[fieldName] = Math.floor(Math.random() * 10000) + 1;
        }
        break;
      case "obj_":
        objectBoolean = true;
        symbolic = symbolicIDGenerator.newSymbolicInputObjectObjectField(process.processId, symbolicObject.objectId, fieldName);
        if (concreteObject[fieldName] === undefined) {
          concreteObject[fieldName] = {};
        }
        break;
      default:
        if (concreteObject[fieldName] === undefined) {
          concreteObject[fieldName] = Math.floor(Math.random() * 10000) + 1;
        }
        break;
    }
  const result = {concrete: concreteObject[fieldName], symbolic: symbolic, objectBoolean: objectBoolean};
  return result;
}

//Generating an input object (call from apply in "advice.js")
export function generateInputObject(process) {
    function generateSymbolicInputObject(processId) { return symbolicIDGenerator.newSymbolicInputObject(processId); }
    const processInputValues = process.getInputs();
    const processFieldInputValues = process.getFieldInputs();
    const symbolic = generateSymbolicInputObject(process.processId); //This is analog with "makeSymbolicInputValue"
    const newConcrete = readInputObjectValue(function () {return {}}, processInputValues, processFieldInputValues, symbolic.id, symbolic.objectId);
    const result = {concrete: newConcrete, symbolic: symbolic};
    return result;
  }

export function generateReturnValueConstrained(processId, functionId, symbolicValue, timesCalled) {
    const returnVariable = symbolicIDGenerator.newFunctionReturn(processId, functionId, symbolicValue, timesCalled);
    const comparison = new SymbolicRelationalExp(returnVariable, Operators.IntEqual, symbolicValue);
    return {
        returnVariable,
        comparison
    };
}