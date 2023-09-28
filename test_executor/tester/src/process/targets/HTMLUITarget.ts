import UITarget, {UITargetJSON} from "@src/process/targets/UITarget";

export interface HTMLUITargetSON extends UITargetJSON {
  processId: number;
  targetId: number;
  specificEventType: string;
}

export default abstract class HTMLUITarget extends UITarget {

  /**
   *
   * @param type
   * @param processId
   * @param targetId
   * @param _htmlElement
   * @param _specificEventType e.g.: mousedown, mouseup, mouseout, mousemove, resize, click,
   */
  constructor(processId, targetId, protected readonly _htmlElement: HTMLElement, protected readonly _specificEventType) {
    super(processId, targetId);
  }

  public get htmlElement() {
    return this._htmlElement
  }

  public get specificEventType() {
    return this._specificEventType;
  }

  toJSON(): HTMLUITargetSON {
    return {
      processId: this.processId,
      targetId: this.targetId,
      specificEventType: this.specificEventType
    }
  }

  toString() {
    return `UITarget(pid: ${this.processId}, tid: ${this.targetId}, htmlElement: ${this.htmlElement}, specificEventType: ${this.specificEventType})`;
  }

}