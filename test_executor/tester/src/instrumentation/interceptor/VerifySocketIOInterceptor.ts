import Chalk from "chalk";
import Util from "util";

import ExecutionStateCollector from "../../tester/ExecutionStateCollector"
import { jsPrint } from "../../util/io_operations";
import MessageWrapper from "./MessageWrapper";
import { Logger as Log } from "../../util/logging";
import * as GTS from "../../tester/global_tester_state";
import * as SymExpConversion from "../../symbolic_expression/convert_symbolic_expressions";
import Interceptor from "./Interceptor";
import SymbolicMessageInput from "../../symbolic_expression/symbolic_message_input";
import ConfigReader from "../../config/user_argument_parsing/Config";
import BranchConstraint from "../constraints/BranchConstraint";
import { VerifyIntraGTS } from "../../tester/verify_intra_gts";
import { GlobalTesterState } from "../../tester/global_tester_state";
import Nothing, {isNothing} from "../../symbolic_expression/nothing";
import Process from "../../process/processes/Process";
import SocketIOInterceptor from "./SocketIOInterceptor";
import tainter from "../tainter";
import { VerifyTestRunner } from "../../tester/test-runners/VerifyTestRunner";

export default class VerifySocketIOInterceptor extends SocketIOInterceptor {
    constructor(_process: Process, gts: GlobalTesterState, private _testRunner: VerifyTestRunner) {
        super(_process, gts)
    }

    private _socketIOOn = ($$function, $$value2, $$values, serial) => {
        const that = this;
        const concrete = tainter.cleanAndRelease($$value2);
        const array$$value = $$values;
        const messageType = tainter.cleanAndRelease(array$$value[0]);
        jsPrint(`Process ${this._process.alias} in handleSocketOnInvocation 1: ${messageType}`);
        const $$callback = array$$value[1];
        const callBack = tainter.cleanAndRelease($$callback);

        const overwrittenCallBack = (data) => {
            Log.SOC(`VerifyIntercepter.overwrittenCallBack, data =`, Util.inspect(data, {depth: 4}));
            if (MessageWrapper.isWrappedMessage(data)) {
                this._testRunner.incomingMessage(that._process, messageType);
                return this.handleMessageReceive(data, $$callback, -1);
            } else {
                jsPrint(Chalk.red(`Process ${that._process.alias}: Message is not marked. Comes from an outside source? Message type = ${messageType}; data = ${data}`));
                return callBack(data);
            }
        }

        const concreteResult = concrete.on(messageType, overwrittenCallBack);
        const $$result = tainter.taintAndCapture(concreteResult, new Nothing());
        jsPrint(`Process ${that._process.alias} registering an event-handler for messages of type ${messageType}`);
        return $$result;
    }

    private _handleMessageEmit = ($$function, $$value2, $$values, serial) => {
        const that: VerifySocketIOInterceptor = this;
        const socket = tainter.cleanAndRelease($$value2);
        const $$type = $$values[0];
        var $$message = $$values.slice(1);
        if ($$message === undefined) {
            $$message = tainter.taintAndCapture(undefined, new Nothing());
        }

        const emitMessage = () => {
            const type = tainter.cleanAndRelease($$type);
            for (let $$messageArg of $$message) {
                const outputCounter = (<VerifyIntraGTS> GTS.globalTesterState!).newMessageOutput(type);
                const messageOutput = new SymbolicMessageInput(type, outputCounter, that._process.processId);
                const $$messageOutputVariable = tainter.taintAndCapture(tainter.cleanAndRelease($$messageArg), messageOutput);
                let comparison;
                if (ConfigReader.config.USE_STRINGS_ARGS) {
                    comparison = SymExpConversion.convertBinStringExpToSymbolic("===", $$messageArg, $$messageOutputVariable);
                    jsPrint("_handleMessageEmit.emitMessage using string comparison");
                } else {
                    comparison = SymExpConversion.convertBinExpToSymbolic("===", $$messageArg, $$messageOutputVariable);
                }
                if ($$messageArg && !isNothing($$messageArg.symbolic) && $$messageArg.symbolic !== undefined) {
                    const processId = that._process.processId;
                    const executionState = ExecutionStateCollector.getExecutionState(processId);
                    const constraint: BranchConstraint = new BranchConstraint(that._process.processId, comparison, true, executionState);
                    GTS.globalTesterState!.addCondition(constraint, that._process.alias);
                }
            }
            const messageId = this.gts.addUnhandledMessage();
            const overwrittenMessage = MessageWrapper.wrapMessage(that._process.processId, messageId, $$message);
            jsPrint(Chalk.blue(`Process ${that._process.alias} emitting message of type ${type}`));
            const concreteEmit = socket.emit(type, overwrittenMessage);
            Log.SOC(`overwrittenMessage = ${Util.inspect(overwrittenMessage, {depth: 4})}`);
            return tainter.taintAndCapture(concreteEmit, new Nothing());
        }

        const shouldSendNow = this.gts.getProcesses().every((process) => {
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