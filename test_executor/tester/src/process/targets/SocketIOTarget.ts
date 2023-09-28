import Target from "./Target";
import SharedOutput from "@src/util/output";
import Chalk from "chalk";
import tainter from "@src/instrumentation/tainter";
import Nothing from "@src/symbolic_expression/nothing";
import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import {generateInputString, makeRandomValue} from "@src/symbolic_expression/helper";
import {VerifyIntraGTS} from "@src/tester/verify_intra_gts";
import * as GTS from "@src/tester/global_tester_state";
import SymbolicMessageInput from "@src/symbolic_expression/symbolic_message_input";
import * as SymExpConversion from "@src/symbolic_expression/convert_symbolic_expressions";
import ExecutionStateCollector from "@src/tester/ExecutionStateCollector";
import BranchConstraint from "@src/instrumentation/constraints/BranchConstraint";
import {doRegularApply} from "@src/instrumentation/helper";
import Process from "@src/process/processes/Process";
import Logger from "@src/util/logging";

export default class SocketIOTarget extends Target {
    constructor(processId: number, targetId: number, private _specificEventType, private _callbackId, private _serial) {
        super(processId, targetId);
    }

    get specificEventType() {
        return this._specificEventType;
    }

    get callbackId() {
        return this._callbackId;
    }

    get serial() {
        return this._serial;
    }

    toString() {
        return `SocketIOTarget(pid: ${this.processId}, tid: ${this.targetId}, messageType: ${this._specificEventType})`;
    }

    fire(process: Process): void {
        const messageType = this.specificEventType;
        const $$callback = process.getSocketIOCallback[this.callbackId];
        SharedOutput.getOutput().writeEvent(process.processId, this.targetId, "message", messageType);
        Logger.ALL(Chalk.blue(`Process ${process.alias} preparing to fire a SocketIO event with message "${messageType}"`));
        const $$value2 = tainter.taintAndCapture(undefined, new Nothing());
        const serial = this.serial;
        const nrOfArgs = 10;
        const $$args = new Array(nrOfArgs).fill(0).map((ignored) => {
            if (ConfigurationReader.config.USE_STRINGS_ARGS) {
                const {concrete, symbolic} = generateInputString(this);
                const inputCounter = (<VerifyIntraGTS> GTS.globalTesterState!).newMessageInput(messageType, process.processId);
                const messageInputVariableConcolic = new SymbolicMessageInput(messageType, inputCounter, process.processId);
                const $$messageInputVariable = tainter.taintAndCapture(concrete, messageInputVariableConcolic);
                GTS.globalTesterState!.addMessageInput(symbolic.executionState);
                const $$arg = tainter.taintAndCapture(concrete, symbolic);
                const comparison = SymExpConversion.convertBinStringExpToSymbolic("===", $$arg, $$messageInputVariable);
                Logger.ALL("Using string comparison in _fireSocketIOTarget");
                const executionState = ExecutionStateCollector.getExecutionState(this.processId);
                const constraint: BranchConstraint = new BranchConstraint(process.processId, comparison, true, executionState);
                GTS.globalTesterState!.addCondition(constraint, process.alias);
                return $$arg;
            } else {
                const {concrete, symbolic} = makeRandomValue(process, 500);
                GTS.globalTesterState!.addMessageInput(symbolic.executionState);
                const $$arg = tainter.taintAndCapture(concrete, symbolic);
                const inputCounter = (<VerifyIntraGTS> GTS.globalTesterState!).newMessageInput(messageType, process.processId);
                const messageInputVariableConcolic = new SymbolicMessageInput(messageType, inputCounter, process.processId);
                const $$messageInputVariable = tainter.taintAndCapture(concrete, messageInputVariableConcolic);
                const comparison = SymExpConversion.convertBinExpToSymbolic("===", $$arg, $$messageInputVariable);
                const executionState = ExecutionStateCollector.getExecutionState(process.processId);
                const constraint: BranchConstraint = new BranchConstraint(process.processId, comparison, true, executionState);
                GTS.globalTesterState!.addCondition(constraint, process.alias);
                return $$arg;
            }
        });
        doRegularApply($$callback, $$value2, $$args, serial);
    }
}