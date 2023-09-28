import SharedOutput from "@src/util/output";
import {makeRandomValue} from "@src/symbolic_expression/helper";
import tainter from "@src/instrumentation/tainter";
import * as GTS from "@src/tester/global_tester_state";
import Chalk from "chalk";
import Process from "@src/process/processes/Process";
import Logging from "@src/util/logging";
import HTMLUITarget from "@src/process/targets/HTMLUITarget";

// mouseEventType = mousedown, mouseup, etc.
export default class ClickableTarget extends HTMLUITarget {
    constructor(processId: number, targetId: number, htmlElement, mouseEventType) {
        super(processId, targetId, htmlElement, mouseEventType);
        this.toString = function () {
            return `ClickableTarget(pid: ${this.processId}, tid: ${this.targetId}, type: ${this.specificEventType})`;
        }
    }

    fire(process: Process): void {
        SharedOutput.getOutput().writeEvent(process.processId, this.targetId, "clickable_event", this.specificEventType);
        const coordX = makeRandomValue(process, 500);
        const $$randomValue1 = tainter.taintAndCapture(coordX.concrete, coordX.symbolic);
        const coordY = makeRandomValue(process, 500);
        const $$randomValue2 = tainter.taintAndCapture(coordY.concrete, coordY.symbolic);
        let mouseButton;
        switch (this.specificEventType) {
            case "contextmenu":
                mouseButton = 2;
                break;
            case "auxclick":
                mouseButton = 1;
                break;
            default:
                mouseButton = 0;
        }
        const e = process.document.createEvent("MouseEvents");
        Logging.EVT(`Clicking on coordinate (${coordX.concrete};${coordY.concrete})`);
        process._addMouseClick(this, e, $$randomValue1, $$randomValue2);
        e.initMouseEvent(this.specificEventType, false, false, process.window,
                 0, 0, 0, coordX.concrete, coordY.concrete,
                false, false, false, false, mouseButton, null as EventTarget | null); // Store symbolic values of x and y
        GTS.globalTesterState!.addSymbolicInput($$randomValue1, "X-coordinate for mouse-event " + this.specificEventType + " on target " + this.htmlElement.toString());
        GTS.globalTesterState!.addSymbolicInput($$randomValue2, "Y-coordinate for mouse-event " + this.specificEventType + " on target " + this.htmlElement.toString());
        SharedOutput.getOutput().writeInputValue(process.processId, $$randomValue1);
        SharedOutput.getOutput().writeInputValue(process.processId, $$randomValue2);
        this.htmlElement.dispatchEvent(e);
        Logging.ALL(Chalk.blue(`emitted event on coordinate (${coordX.concrete};${coordY.concrete})`));
    }
}