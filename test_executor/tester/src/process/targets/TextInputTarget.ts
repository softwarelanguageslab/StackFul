import SharedOutput from "@src/util/output";
import {makeRandomValue} from "@src/symbolic_expression/helper";
import tainter from "@src/instrumentation/tainter";
import KeyCodeEnum from "@src/process/KeyCodeEnum";
import Chalk from "chalk";
import * as GTS from "@src/tester/global_tester_state";
import Process from "@src/process/processes/Process";
import Logger from "@src/util/logging";
import HTMLUITarget from "@src/process/targets/HTMLUITarget";
import SymbolicInput from "@src/symbolic_expression/SymbolicInput.js";

// keyEventType = keydown, keyup, etc.
export default class TextInputTarget extends HTMLUITarget {

    constructor(processId: number, targetId: number, htmlElement, keyEventType) {
        super(processId, targetId, htmlElement, keyEventType);
    }

    fire(process: Process): void {
        SharedOutput.getOutput().writeEvent(process.processId, this.targetId, "keyboard_event", this.specificEventType);
        const {concrete, symbolic} = makeRandomValue(process, 127);
        const $$randomValue = tainter.taintAndCapture(concrete, symbolic);
        SharedOutput.getOutput().writeInputValue(process.processId, $$randomValue);
        const keyInfo = {
            bubbles: false,
            keyCode: concrete,
            which: concrete,
            altKey: concrete === KeyCodeEnum.ALT_KEY,
            ctrlKey: concrete === KeyCodeEnum.CTRL_KEY,
            metaKey: concrete === KeyCodeEnum.META_KEY_1,
            shiftKey: concrete === KeyCodeEnum.SHIFT_KEY
        };
        const e = new (process.window as any).KeyboardEvent(this.specificEventType, keyInfo);
        process._addKeyPress(this, e, $$randomValue);
        Logger.ALL(Chalk.blue(`Process ${process.alias} fired a keyboard event with value ${concrete}`));
        GTS.globalTesterState!.addSymbolicInput($$randomValue, "Keycode for keyboard-event " + this.specificEventType + " on target " + this.htmlElement.toString());
        this.htmlElement.dispatchEvent(e);
    }
}