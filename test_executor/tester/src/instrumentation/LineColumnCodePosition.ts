export default class LineColumnCodePosition {
  constructor(public readonly line: number, public readonly column: number, public readonly file: string) { }

  toString(): string {
    return `{file:${this.file};line:${this.line};column:${this.column}}`;
  }
}