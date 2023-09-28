import {generateReplacement, IFunctionReplacement} from "./IFunctionReplacement";
import {jsPrint} from "@src/util/io_operations";
import tainter from "../tainter";
import MessageWrapper from "./MessageWrapper";
import Nothing from "../../symbolic_expression/nothing";
import Chalk from "chalk";
import Log from "../../util/logging";
import Util from "util";
import Process from "../../process/processes/Process";
import {GlobalTesterState} from "@src/tester/global_tester_state";
import SymbolicBool from "../../symbolic_expression/symbolic_bool";
import SymbolicInt from "../../symbolic_expression/symbolic_int";
import SymbolicString from "../../symbolic_expression/symbolic_string";
import SymbolicRegExp from "../../symbolic_expression/symbolic_reg_exp";
import SymbolicInputInt from "../../symbolic_expression/symbolic_input_int";
import SymbolicInputString from "../../symbolic_expression/symbolic_input_string";
import SymbolicArithmeticExp from "../../symbolic_expression/symbolic_arithmetic_exp";
import SymbolicRelationalExp from "../../symbolic_expression/symbolic_relational_exp";
import SymbolicUnaryExp from "../../symbolic_expression/symbolic_unary_exp";
import SymbolicLogicalBinExpression from "../../symbolic_expression/symbolic_logical_bin_expression";
import SymbolicStringExpression from "../../symbolic_expression/symbolic_string_expression";
import SymbolicEventChosen from "../../symbolic_expression/symbolic_event_chosen";
import {doRegularApply} from "../helper";
import {
    ExecutionState,
    LocationExecutionState,
    StackFrame,
    TargetTriggeredExecutionState
} from "@src/tester/ExecutionState.js";
import CodePosition from "@src/instrumentation/code_position.js";
import EventWithId from "@src/tester/EventWithId.js";
import noIdentifier from "@src/instrumentation/NoIdentifier.js";

export default class SocketIOInterceptor {
    constructor(protected _process: Process, protected gts: GlobalTesterState) {
    }

    public generateOn = (): IFunctionReplacement | undefined => {
        try {
            const _global = this._process.global
            const socketPrototype = _global.process.mainModule.require('socket.io/lib/socket.js').prototype;
            const handler = this.handleOn();
            return generateReplacement(socketPrototype.on, handler)
        } catch (e) {
            jsPrint("Is a node process, but doesn't require socket-io");
            return undefined
        }
    }

    public generateEmit = (): IFunctionReplacement | undefined => {
        try {
            const _global = this._process.global
            const socketPrototype = _global.process.mainModule.require('socket.io/lib/socket.js').prototype
            const handler = this.handleEmit()
            return generateReplacement(socketPrototype.emit, handler);
        } catch (_e) {
            return undefined
        }
    }

    private handleEmit() {
        return ($$function, $$value2, $$values, serial) => {
            const socket = tainter.cleanAndRelease($$value2);
            const $$type = $$values[0];
            let $$message = $$values.slice(1);
            if ($$message === undefined) {
                $$message = tainter.taintAndCapture(undefined, new Nothing());
            }

            const emitMessage = () => {
                const type = tainter.cleanAndRelease($$type);
                const messageId = this.gts.addUnhandledMessage();
                const overwrittenMessage = MessageWrapper.wrapMessage(this._process.processId, messageId, $$message);
                // jsPrint(Chalk.blue(`Process ${this._process.alias} emitting message of type ${type}`));
                const concreteEmit = socket.emit(type, overwrittenMessage);
                // Log.SOC(`overwrittenMessage = ${Util.inspect(overwrittenMessage, {depth: 4})}`);
                return tainter.taintAndCapture(concreteEmit, new Nothing());
            }

            const shouldSendNow = this.gts.getProcesses().every(function (process) {
                return process.hasFinishedSetup()
            });

            if (shouldSendNow) {
                return emitMessage();
            } else {
                jsPrint(Chalk.red(`Process ${this._process.alias} is postponing the message emit`));
                this.gts.addUnfinishedMessageEmit(emitMessage);
                return;
            }
        }
    }

    protected handleMessageReceive(wrappedMessage, $$callback, serial: number) {
        // jsPrint(`Process ${this._process.alias}: handleMessageReceive:`, Util.inspect(wrappedData, { depth: 5}));
        const wrappedDataArray = MessageWrapper.getMessageValues(wrappedMessage)
        const messageId = MessageWrapper.getMessageId(wrappedMessage)
        jsPrint(`Process ${this._process.alias} received message`);
        Log.SOC(`handleMessageReceive: ${JSON.stringify(wrappedDataArray)}`);
        const array$$wrappedData: any[] = [];
        for (let wrappedData of wrappedDataArray) {
            const $$wrappedData = this.addToWrappers(wrappedData);
            array$$wrappedData.push($$wrappedData);
        }
        this.gts.deleteUnhandledMessage(messageId);
        Log.SOC("handleMessageReceive, $$wrappedData =", array$$wrappedData);
        const $$callBacked = doRegularApply($$callback, tainter.taintAndCapture(undefined, undefined), array$$wrappedData, serial);
        return tainter.cleanAndRelease($$callBacked);
    }

    private addToWrappers(object) {
        if (typeof object.base === "object" && object) {
            for (let key in object.base) {
                const $$res = this.addToWrappers(object.base[key]);
                object.base[key] = $$res;
            }
            Reflect.setPrototypeOf(object.base, tainter.capture(Reflect.getPrototypeOf(object.base)));
        }
        const convertedSymbolic = this.valueToSymbolic(object.symbolic);
        const $$res = tainter.taint(object.base, convertedSymbolic);
        return $$res;
    }

    // @ts-ignore
    private parseExecutionState(json: any): ExecutionState {
        switch (json._type) {
            case "TARGET_TRIGGERED": return new TargetTriggeredExecutionState(json.eventId, json.processId);
            case "BASIC":
                let codePosition = new CodePosition(Number(json._position._file), json._position._serial);
                const stack = json._stack.map(stackFrameJson => new StackFrame(stackFrameJson.arrival, stackFrameJson.call));
                const eventSequenceLength = json._currentEventSequence.length;
                let currentEventSequence: EventWithId[] = [];
                if (eventSequenceLength > 0) {
                    const lastEvent = (json._currentEventSequence.length === 0) ? [] : json._currentEventSequence[eventSequenceLength - 1];
                    currentEventSequence = [new EventWithId(lastEvent.processId, lastEvent.targetId, lastEvent.id)];
                }
                return new LocationExecutionState(codePosition, stack, currentEventSequence, json._callSiteSerial);
        }
    }

    private valueToSymbolic(symValue) {
        if (!symValue) {
            return new Nothing();
        }
        const identifier = symValue._identifier ? symValue._identifier : noIdentifier;
        switch (symValue.type) {
            case "SymbolicBool":
                return new SymbolicBool(symValue.b).setIdentifier(identifier);
            case "SymbolicInt":
                return new SymbolicInt(symValue.i).setIdentifier(identifier);
            case "SymbolicString":
                return new SymbolicString(symValue.s).setIdentifier(identifier);
            case "SymbolicRegExp":
                return new SymbolicRegExp(symValue.e).setIdentifier(identifier);
            case "SymbolicInputInt": {
                const newES = this.parseExecutionState(symValue.executionState);
                return new SymbolicInputInt(symValue.processId, newES, symValue.id).setIdentifier(identifier);
            }
            case "SymbolicInputString": {
                const newES = this.parseExecutionState(symValue.executionState);
                return new SymbolicInputString(symValue.processId, newES, symValue.id).setIdentifier(identifier);
            }
            // For some reason, we have to make a *new* array here and copy symValue.args's contents
            case "SymbolicArithmeticExp":
                return new SymbolicArithmeticExp(symValue.operator, [...symValue.args].map(this.valueToSymbolic.bind(this))).setIdentifier(identifier);
            case "SymbolicRelationalExp":
                return new SymbolicRelationalExp(this.valueToSymbolic(symValue.left), symValue.operator, this.valueToSymbolic(symValue.right)).setIdentifier(identifier);
            case "SymbolicUnaryExp":
                return new SymbolicUnaryExp(symValue.operator, this.valueToSymbolic(symValue.argument));
            case "SymbolicLogicalBinExpression":
                return new SymbolicLogicalBinExpression(this.valueToSymbolic(symValue.left), symValue.operator, this.valueToSymbolic(symValue.right)).setIdentifier(identifier);
            // Again, make a *new* array here and copy symValue.args's contents
            case "SymbolicStringOperationExp":
                return new SymbolicStringExpression(symValue.operator, [...symValue.args].map(this.valueToSymbolic.bind(this))).setIdentifier(identifier);
            case "SymbolicRegularExpression":
                throw new Error("valueToSymbolic of a SymbolicRegularExpression: not yet implemented");
            case "SymbolicEventChosen":
                // Todo: check if target may be undefined
                return new SymbolicEventChosen(symValue.id, symValue.processIdChosen, symValue.eventIdChosen, symValue.totalNrOfProcesses, symValue.totalNrOfEvents, symValue.target).setIdentifier(identifier);
            case "Nothing":
                return new Nothing();
            default:
                throw new Error(`valueToSymbolic: unrecognized type: ${symValue.type}`);
        }
    }

    private handleOn() {
        return ($$function, $$value2, $$values, serial) => {
            const concrete = tainter.cleanAndRelease($$value2);
            const array$$value = $$values;
            const messageType = tainter.cleanAndRelease(array$$value[0]);
            jsPrint(`Process ${this._process.alias} in handleSocketOnInvocation 1: ${messageType}`);
            const $$callback = array$$value[1];
            const callBack = tainter.cleanAndRelease($$callback);

            const overwrittenCallBack = (data) => {
                if (MessageWrapper.isWrappedMessage(data)) {
                    return this.handleMessageReceive(data, $$callback, -1);
                } else {
                    jsPrint(Chalk.red("Process", this._process.alias, ": Message is not marked. Comes from an outside source?", "message type =", messageType, "; data =", data));
                    return callBack(...data);
                }
            }

            const concreteResult = concrete.on(messageType, overwrittenCallBack);
            const $$result = tainter.taintAndCapture(concreteResult, new Nothing());
            jsPrint(`Process ${this._process.alias} registering an event-handler for messages of type ${messageType}`);
            return $$result;
        }
    }
}