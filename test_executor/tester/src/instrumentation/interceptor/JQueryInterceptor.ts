import {generateReplacement, IFunctionReplacement} from "./IFunctionReplacement";
import tainter from "../tainter";
import {generateInputString} from "../../symbolic_expression/helper";
import * as GTS from "../../tester/global_tester_state";
import Process from "../../process/processes/Process";
import {GlobalTesterState} from "../../tester/global_tester_state";
import {doRegularApply} from "../helper";

export default class JQueryInterceptor {
    public static generateEvent(process: Process, gts: GlobalTesterState): IFunctionReplacement[] {
        const jqueryEvents = ["click", "mousedown", "mouseup", "keypress", "keydown", "keyup"];

        const replacement = (jqueryEvent) => {
            return ($$function, $$value2, $$values, serial) => {
                const htmlElement = (tainter.cleanAndRelease($$value2))[0];
                process._maybeRegisterEvent(jqueryEvent, htmlElement, gts);
                return doRegularApply($$function, $$value2, $$values, serial);
            }
        }

        return jqueryEvents.map(
            (jqueryEvent) =>
                generateReplacement(process.global.window.jQuery.prototype[jqueryEvent], replacement(jqueryEvent)))

    }

    public static generateVal(process: Process): IFunctionReplacement {
        const replacement = ($$function, $$value2, $$values, serial) => {
            if ($$values.length === 0) {
                const {concrete, symbolic} = generateInputString(process);
                const $$result = tainter.taintAndCapture(concrete, symbolic);
                GTS.globalTesterState!.addSymbolicInput($$result, "Reading value from jQuery text inputfield " + tainter.cleanAndRelease($$value2).toString());
                return $$result;
            } else {
                return doRegularApply($$function, $$value2, $$values, serial);
            }
        }
        return generateReplacement(process.global.window.jQuery.prototype.val, replacement);
    }
}