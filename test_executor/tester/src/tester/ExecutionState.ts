import * as GTS from "../tester/global_tester_state";
import CodePosition from "../instrumentation/code_position"
import EventWithId from "@src/tester/EventWithId.js";

export abstract class ExecutionState {
  constructor(public readonly _type: string) {}
  public abstract duplicate(): ExecutionState;
  public abstract fireEvent(): ExecutionState;

  public abstract updateSerial(newSerial: number, processId: number): ExecutionState;
  public abstract pushFrame(frame: number, processId: number): ExecutionState;
  public abstract popFrame(): ExecutionState;
  public abstract callFrom(serial: number, processId): ExecutionState;
}

export class TargetTriggeredExecutionState extends ExecutionState {

  constructor(protected eventId: number, protected readonly processId: number) {
    super("TARGET_TRIGGERED");
  }

  public toString(): string {
    return `TTES(id:${this.eventId},pid:${this.processId})`;
  }

  public equals(other: ExecutionState): boolean {
    switch (other._type) {
      case "TARGET_TRIGGERED":
        let castedOther = other as TargetTriggeredExecutionState;
        return this.eventId === castedOther.eventId &&
               this.processId === castedOther.processId;
      default: return false;
    }
  }

  public override duplicate(): ExecutionState {
    return new TargetTriggeredExecutionState(this.eventId, this.processId);
  }

  public override fireEvent(): ExecutionState {
    return this;
  }

  public override callFrom(serial: number, processId): ExecutionState {
    const position = new CodePosition(processId, serial);
    return new LocationExecutionState(position, [], GTS.globalTesterState!.getCurrentEventSequence(), null).callFrom(serial, processId);
  }

  public override popFrame(): ExecutionState {
    throw new Error("Should not happen");
  }

  public override pushFrame(frame: number, processId: number): ExecutionState {
    throw new Error("Should not happen");
  }

  public override updateSerial(newSerial: number, processId: number): ExecutionState {
    const position = new CodePosition(processId, newSerial);
    return new LocationExecutionState(position, [], GTS.globalTesterState!.getCurrentEventSequence(), null).updateSerial(newSerial, processId);
  }

}

export class TargetEndExecutionState extends ExecutionState {
  constructor(protected _id: number) {
    super("TARGET_END");
  }
  toString(): string {
    return `TE_ES(${this._id})`;
  }
  public duplicate(): TargetEndExecutionState {
    return new TargetEndExecutionState(this._id);
  }

  fireEvent(): ExecutionState {
    return this;
  }

  callFrom(serial: number, processId): ExecutionState {
    return this;
  }

  popFrame(): ExecutionState {
    return this;
  }

  pushFrame(): ExecutionState {
    return this;
  }

  updateSerial(): ExecutionState {
    return this;
  }
}

export class StackFrame {
  constructor(public readonly arrival: number, public readonly call: number | null) {}
  toString(): string {
    return `${this.arrival}-${this.call}`;
  }
}

export class LocationExecutionState extends ExecutionState {
  protected readonly _stackLength: number;
  constructor(protected _position: CodePosition, protected _stack: StackFrame[], protected _currentEventSequence: EventWithId[], protected _callSiteSerial: number | null) {
    super("BASIC");
    this._stackLength = GTS.globalTesterState!.getBranchStackLength();
  }

  public duplicate(): LocationExecutionState {
    const positionCopy = new CodePosition(this._position.getProcessId, this._position.getSerial);
    return new LocationExecutionState(positionCopy, this._stack.slice(), this._currentEventSequence.slice(), this._callSiteSerial);
  }

  protected getLastEvent(): EventWithId | null {
    return (this.getEventSequence.length > 0) ? this.getEventSequence[this.getEventSequence.length - 1] : null;
  }

  equals(other: ExecutionState): boolean {
    switch (other._type) {
      case "BASIC":
        let castedOther = other as LocationExecutionState;
        const thisLastEvent = this.getLastEvent();
        const otherLastEvent = castedOther.getLastEvent();
        return this._position.equals(castedOther._position) &&
          this._stack.length === castedOther._stack.length &&
          this._stack.every((value, index) => {
            let thisValue = this._stack[index];
            let otherValue = castedOther._stack[index];
            return thisValue.arrival === otherValue.arrival &&
              thisValue.call === otherValue.call;
          }) &&
          // for equality, either both last events must be null or both must be non-null and equal to each other
          ((thisLastEvent === null && thisLastEvent === otherLastEvent) ||
            thisLastEvent !== null && otherLastEvent !== null && thisLastEvent.equals(otherLastEvent));
      default: return false;
    }
  }

  toString() {
    return `ES(${this.getCodePosition}, [${this.getFunctionStack}], ${this.getLastEvent()})`;
  }
  get getCodePosition() {
    return this._position;
  }
  get getFunctionStack() {
    return this._stack;
  }
  get getEventSequence() {
    return this._currentEventSequence;
  }
  get getCallSiteSerial(): number | null {
    return this._callSiteSerial;
  }
  public override callFrom(serial: number, processId): LocationExecutionState {
    if (processId === this.getCodePosition.getProcessId) {
      this._callSiteSerial = serial;
    }
    return this;
  }
  public override pushFrame(frame: number, processId: number): LocationExecutionState {
    if (processId === this.getCodePosition.getProcessId) {
      const callSiteSerial = this._callSiteSerial;
      this._callSiteSerial = null; // Callsite serial has been consumed
      const copy = [...this._stack];
      if (frame !== 1 && callSiteSerial !== null) { // TODO hack?
        const stackItem: StackFrame = new StackFrame(frame, callSiteSerial);
        copy.push(stackItem);
        return new LocationExecutionState(this.getCodePosition, copy, this._currentEventSequence.slice(), this._callSiteSerial);
      } else {
        return this;
      }
    } else {
      return this;
    }
  }
  public override popFrame(): LocationExecutionState {
    const copy = [...this._stack];
    const temp = copy.pop();
    return new LocationExecutionState(this.getCodePosition, copy, this._currentEventSequence.slice(), this._callSiteSerial);
  }
  public override updateSerial(newSerial: number, _: number): LocationExecutionState {
    this._position = this._position.copy(newSerial);
    return this;
  }
  public override fireEvent(): ExecutionState {
    const stack = this.getFunctionStack.slice();
    return new LocationExecutionState(this.getCodePosition, stack, GTS.globalTesterState!.getCurrentEventSequence(), this._callSiteSerial);
  }

}

export function newExecutionState(codePosition: CodePosition): LocationExecutionState {
  return new LocationExecutionState(codePosition, [], GTS.globalTesterState!.getCurrentEventSequence(), null);
}