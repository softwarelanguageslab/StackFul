import Target from "./Target";

export interface UITargetJSON {
    processId: number;
    targetId: number;
}

export default abstract class UITarget extends Target {

    /**
     *
     * @param type clickable, window,
     * @param processId
     * @param targetId
     */
    constructor(processId, targetId) {
        super(processId, targetId);
    }

    toJSON(): UITargetJSON {
        return {
            processId: this.processId,
            targetId: this.targetId
        }
    }

    toString() {
        return `UITarget(pid: ${this.processId}, tid: ${this.targetId})`;
    }
}


