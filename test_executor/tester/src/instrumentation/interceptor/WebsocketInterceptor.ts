import Chalk from "chalk";

import {generateReplacement} from "./IFunctionReplacement";
import {GlobalTesterState} from "../../tester/global_tester_state";
import {jsPrint} from "../../util/io_operations";
import MessageWrapper from "./MessageWrapper";
import Nothing from "../../symbolic_expression/nothing";
import Process from "../../process/processes/Process";
import tainter from "../tainter";

export class WebsocketInterceptor {

    public static generateSend(process: Process, gts: GlobalTesterState) {

        const replacement = ($$function, $$value2, $$values, serial) => {
            jsPrint(`Process ${process.alias}: handleWSSend`);
            const $$message = $$values[0];

            const emitMessage = () => {
                jsPrint(Chalk.blue(`Process ${process.alias} sending message`));
                const messageId = gts.addUnhandledMessage();
                const overwrittenMessage = JSON.stringify(MessageWrapper.wrapMessage(process.processId, messageId, $$message));
                // todo: check if this is correct
                const result = process.global.WebSocket.send(overwrittenMessage);
                jsPrint(`Process ${process.alias} sent a message`);
                return tainter.taintAndCapture(result, new Nothing());
            }

            const shouldSendNow = gts.getProcesses().every(function (process) {
                return process.hasFinishedSetup()
            });
            if (shouldSendNow) {
                return emitMessage();
            } else {
                jsPrint(Chalk.red(`Process ${process.alias} is postponing the message emit`));
                gts.addUnfinishedMessageEmit(emitMessage);
                return;
            }
        }
        return generateReplacement(process.global.WebSocket.prototype.send, replacement)
    }

}