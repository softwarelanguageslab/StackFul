export default class CodePosition {
  protected _file: string;
  constructor(private _processId: number, private _serial: number) {
    this._file = String(_processId);
  }
  
  get getProcessId(): number {
    return this._processId;
  }

  get getSerial(): number {
    return this._serial;
  }

  toString(): string {
    return `${this.getSerial}@${this.getProcessId}`;
  }

  equals(other: any): boolean {
    console.log(`Comparing ${this.toString()} with ${other.toString()}`);
    return typeof(other) === 'object' && other instanceof CodePosition &&
           this.getProcessId === other.getProcessId && this.getSerial === other.getSerial;
  }

  copy(newSerial): CodePosition {
      return new CodePosition(this.getProcessId, newSerial);
  }
}
