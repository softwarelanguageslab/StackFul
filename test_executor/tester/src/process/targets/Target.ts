import Process from "@src/process/processes/Process";
import Event from "@src/tester/Event.js";

/**
 * The Target class models targets that events can fire upon
 */
export default abstract class Target {

    /**
     *
     * @param _processId
     * @param _targetId
     */
    protected constructor(private _processId: number, private _targetId: number) {
    }

    get processId() {
        return this._processId
    }

    get targetId() {
        return this._targetId
    }

    equalsTarget(otherTarget) {
        return this._processId === otherTarget.processId &&
            this._targetId === otherTarget.targetId;
    }

    toEvent(): Event {
        return new Event(this.processId, this.targetId);
    }

    abstract fire(process: Process);
}