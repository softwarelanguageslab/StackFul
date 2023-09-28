import extendedSet, {setUnion, setDifference} from "../util/datastructures/ExtendedSet";

interface ScopeTriple {
  name: string;
  base: any;
  symbolic: any;
}

export class SymbolicScope {
  protected _scope: any;
  constructor(protected _serial: number, protected _id: number) {
    this._scope = {};
  }
  get getSerial() {
    return this._serial;
  }
  get getId() {
    return this._id;
  }
  public hasIdentifier(name: string): boolean {
  	return this._scope[name] !== undefined;
  }
  public readIdentifier(name: string) {
    return this._scope[name];
  }
  public writeIdentifier(name: string, concValue, symValue) {
    this._scope[name] = {name: name, base: concValue, symbolic: symValue};
  }
  public getIdentifiers() {
    return Object.keys(this._scope);
  }

  public makeSnapshot() {
    return Object.assign({}, this._scope);
  }

  public toString(): string {
    return `{Scope ${this._serial}}`;
  }

  public diff(otherScope): any {
    const ownIdentifiers = new Set(...this.getIdentifiers());
    const otherIdentifiers = new Set(...otherScope.getIdentifiers());
    return setUnion(ownIdentifiers, otherIdentifiers);
  }
}

export class SymbolicScopeFactory {
  protected _scopeId: number;
  constructor() {
    this._scopeId = 0;
  }
  public makeScope(serial: number): SymbolicScope {
    const newScope = new SymbolicScope(serial, this._scopeId);
    this._scopeId++;
    return newScope;
  }
}
