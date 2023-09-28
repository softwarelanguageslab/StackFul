import {jsPrint} from "../util/io_operations";

export class MergingState {
  constructor(protected _label: number, protected _branchSequence: any, protected _symbolicStoreSnapshot: any) {
  }
}

export class MergingStatesStore {
  protected _map: Map<string, number>;
  constructor() {
    this._map = new Map<string, number>();
  }
  addMergingState(serial: number, branchSequence, symbolicStoreSnapshot): void {
    const newMergingState = new MergingState(serial, branchSequence, symbolicStoreSnapshot);
    this._map[serial] = newMergingState;
  }
}

const singleMergingStateStore = new MergingStatesStore();

export default singleMergingStateStore;