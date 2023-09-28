import Process from "@src/process/processes/Process";
import HTMLUITarget from "@src/process/targets/HTMLUITarget";

// windowEventType = onresize, onload, etc.
export default class WindowTarget extends HTMLUITarget {
    constructor(processId, targetId, htmlElement, windowEventType) {
        super(processId, targetId, htmlElement, windowEventType);
        this.toString = function () {
            return `WindowTarget(pid: ${this.processId}, tid: ${this.targetId}, type: ${this.specificEventType})`;
        }
    }

    fire(process: Process): void {
        const resizeEvent = process.document!.createEvent('UIEvents');
        resizeEvent.initUIEvent(this.specificEventType, true, false, process.window, 0);
        process.window.dispatchEvent(resizeEvent);
    }
}