const util = require("util");

import {setDifference} from "../util/datastructures/ExtendedSet";
import {Logger as Log} from "@src/util/logging";
import Stack from "../util/datastructures/stack";
import {SymbolicScope, SymbolicScopeFactory} from "./SymbolicScope";

interface ISymbolicStore {
  reset(): void;
  getScopes(): any;
  getFilteredScopes(): any;
  popScope(serial): any[];
  pushScope(serial): SymbolicScope;
  topScope(): any;
  readIdentifier(name: string): any;
  writeIdentifier(name: string, concValue, symValue): void;
  makeStoreElement(name, value): { id: any; exp: any };
  makeSnapshot(): any[];
  toIdentifiers(): any;
  diff(otherStore): Set<any>;
}

export class SymbolicStore implements ISymbolicStore {
  protected _symbolicScopeFactory: SymbolicScopeFactory;
  protected _scopes: any;
  constructor() {
    this._symbolicScopeFactory = new SymbolicScopeFactory();
    const globalScope = this._symbolicScopeFactory.makeScope(-1);
    this._scopes = [globalScope];
  }

  reset(): void {
    this._symbolicScopeFactory = new SymbolicScopeFactory();
    const globalScope = this._symbolicScopeFactory.makeScope(-1);
    this._scopes = [globalScope];
  }

  protected _pop() {
    return this._scopes.pop();
  }
  protected _top() {
    return this._scopes[this._scopes.length - 1];
  }

  protected _print(statusMessage): void {
    // console.log(statusMessage);
    // console.log(this._scopes);
  }

  getScopes() {
    return this._scopes.slice();
  }

  getFilteredScopes() {
    return this.getScopes();
  }

  popScope(serial) {
    var poppedScopes: any[] = [];
    var topScope, topSerial;
    Log.STO("In SymStore.popScope, before pop:", this._scopes);
    do {
      topScope = this._pop();
      topSerial = topScope.getSerial;
      poppedScopes.push(topScope);
    } while (topSerial !== serial);
    return poppedScopes;
  }

  pushScope(serial) {
    const newScope = this._symbolicScopeFactory.makeScope(serial);
    this._scopes.push(newScope);
    return newScope;
  }

  topScope(): SymbolicScope | undefined {
    for (let idx = this._scopes.length - 1; idx >= 0; idx--) {
      const item = this._scopes[idx];
      return item;
    }
  }

  readIdentifier(name: string) {
    // Check if identifier has already been defined in a scope.
    // Only write it in the top scope if it has not been defined yet.
    for (let idx = this._scopes.length - 1; idx >= 0; idx--) {
      const item = this._scopes[idx];
      if (item.hasIdentifier(name)) {
        return item.readIdentifier(name);
      }
    }
    return this.topScope()!.readIdentifier(name);
  }

  writeIdentifier(name: string, concValue, symValue): void {
    // Check if identifier has already been defined in a scope.
    // Only write it in the top scope if it has not been defined yet.
    for (let idx = this._scopes.length - 1; idx >= 0; idx--) {
      const item = this._scopes[idx];
      // console.log("Checking for identifier", name, "in scope", item);
      if (item.hasIdentifier(name)) {
        // console.log("Found identifier", name, "in scope", item, "with idx", idx, "of total scopes", this._scopes.length);
        item.writeIdentifier(name, concValue, symValue);
        // console.log("scope:", item);
        return;
      }
    }
    this.topScope()!.writeIdentifier(name, concValue, symValue);
  }

  makeStoreElement(name, value) {
    return {id: name, exp: value};
  }

  protected _toArray(dict) {
    const array: any[] = [];
    for (let k in dict) {
      array.push(this.makeStoreElement(k, dict[k]));
    }
    return array;
  }

  makeSnapshot() {
    let dict = {};
    for (let scope of this._scopes) {
      dict = Object.assign(dict, scope.makeSnapshot());
    }
    return this._toArray(dict);
  }

  toIdentifiers() {
    return this.getFilteredScopes().map((scope) => scope.getIdentifiers());
  }

  diff(otherStore) {
    const ownIdentifiers = new Set([...this.toIdentifiers()]);
    const otherIdentifiers = new Set([...otherStore.toIdentifiers()]);
    return setDifference(ownIdentifiers, otherIdentifiers);
  }
}

/**
 * A stack of symbolic stores, to model scoping. 
 */
export class StoreStack {
  protected _stack: Stack;
  constructor() {
    this._stack = new Stack();
    const initialStore = this.makeNewStore();
    this._pushStore(initialStore);
  }
  _pushStore(store) {
    this._stack.push(store);
    // console.log("pushed, stack size now:", this._stack.length());
  }
  _popStore() {
    const temp = this._stack.pop();
    // console.log("popped, stack size now:", this._stack.length());
    return temp;
  }
  _topStore() {
    return this._stack.top();
  }
  makeNewStore() {
    return new SymbolicStore();
  }
  reset() {
    this._stack = new Stack();
    const initialStore = this.makeNewStore();
    this._pushStore(initialStore);
  }
  topStore() {
    return this._topStore();
  }

  getScopes() {
    return this._topStore().getScopes();
  }
  popScope(serial) {
    return this._topStore().popScope(serial);
  }
  pushScope(serial) {
    return this._topStore().pushScope(serial);
  }
  topScope() {
    return this._topStore().topScope();
  }
  arrive(environment) {
    var diff;
    if (this._stack.isEmpty()) {
      diff = new Set();
    } else {
      const oldStore = this._topStore();
      diff = oldStore.diff(environment);
    }
    Log.STO("Arriving:, diff is");
    Log.STO(diff);
    this._pushStore(environment);
  }
  return() {
    const oldStore = this._popStore();
    var diff;
    if (this._stack.isEmpty()) {
      diff = new Set();
    } else {
      const newStore = this._topStore();
      diff = oldStore.diff(newStore);
    }
    Log.STO("Returning:, diff is");
    Log.STO(diff);
    return diff;
  }
  readIdentifier(name) {
    return this._topStore().readIdentifier(name);
  }
  writeIdentifier(name, concValue, symValue) {
    return this._topStore().writeIdentifier(name, concValue, symValue);
  }
  makeSnapshot() {
    return this._topStore().makeSnapshot();
  }
  toIdentifiers() {
    return this._topStore().toIdentifiers();
  }
}

const singleStoreStack = new StoreStack;

export default singleStoreStack;