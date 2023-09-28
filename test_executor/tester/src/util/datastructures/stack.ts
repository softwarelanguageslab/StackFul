/**
 * Custom stack implementation with some specific foreach functions
 */
export default class Stack {
  private _stack: any[] = []

  constructor() {
  }

  push(element) {
    this._stack.push(element);
  }

  pop() {
    if (this._stack.length <= 0) { throw new Error("Stack is empty"); }
    return this._stack.pop();
  }
  top() {
    const length = this.length();
    if (length <= 0) { throw new Error("Stack is empty"); }
    return this._stack[length - 1];
  }
  length() {
    return this._stack.length;
  }
  isEmpty() {
    return this.length() <= 0;
  }
  index(idx) {
    if (idx >= this.length()) { throw new Error("Stack is empty"); }
    return this._stack[idx];
  }
  bottomToTopForEach(fun) {
    this._stack.forEach(fun);
  }
  topToBottomForEach(fun) {
    this._stack.slice().reverse().forEach(fun);
  }
  toString() {
    return "[" + this._stack.join(", ") + "]";
  }
}
