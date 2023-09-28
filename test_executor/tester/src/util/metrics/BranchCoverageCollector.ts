import {Node} from "estree";

import * as ES from "estraverse";

import CodePosition from "@src/instrumentation/code_position";
import AranNode from "@src/instrumentation/AranNode";
import LineColumnCodePosition from "@src/instrumentation/LineColumnCodePosition.js";

class Branch {
  protected elseBranchTaken: boolean = false;
  protected thenBranchTaken: boolean = false;

  public tookBranch(wasTrue: boolean): void {
    if (wasTrue) {
      this.thenBranchTaken = true;
    } else {
      this.elseBranchTaken = true;
    }
  }

  public tookElseBranch(): boolean {
    return this.elseBranchTaken;
  }

  public tookThenBranch(): boolean {
    return this.thenBranchTaken;
  }

  public isCovered(): boolean {
    return this.elseBranchTaken && this.thenBranchTaken;
  }
}

class BranchCoverageVisitor extends ES.Controller {

  constructor(protected readonly branches: Map<string, Branch>, protected readonly processId: number) {
    super();
  }

  enter(node: Node, _: Node | null): ES.VisitorOption | Node | void {
    if (node.type === "IfStatement") {
      const visitor: BranchCoverageVisitor = (this as any).visitor;
      const serial = (node as any).AranSerial; // Serial field added by Aran
      const position = new CodePosition(visitor.processId, serial);
      visitor.branches.set(JSON.stringify(position), new Branch());
    }
  };

}

export interface BranchCoverageEntry {
  codePosition: CodePosition;
  elseBranch: boolean;
  thenBranch: boolean;
}

export default class BranchCoverageCollector {
  // Set of processes whose AST has already been added to the map of branches
  protected readonly rootReads: Set<number> = new Set();
  protected readonly branches: Map<string, Branch> = new Map();
  protected readonly branchPositions: Map<string, LineColumnCodePosition> = new Map();

  public makeSnapshot(): BranchCoverageEntry[] {
    const array: BranchCoverageEntry[] = [];
    for (let [codePositionString, branch] of this.branches) {
      let codePosition = JSON.parse(codePositionString) as CodePosition;
      const entry: BranchCoverageEntry = {codePosition, elseBranch: branch.tookElseBranch(), thenBranch: branch.tookThenBranch()};
      array.push(entry);
    }
    return array;
  }

  public tookBranch(position: CodePosition, filePosition: LineColumnCodePosition, wasTrue: boolean): void {
    const key = JSON.stringify(position)
    this.branches.get(key)?.tookBranch(wasTrue);
    this.branchPositions.set(key, filePosition);
  }

  public includeAST(root: AranNode, processId: number): void {
    if (!this.rootReads.has(root.AranSerial)) {
      const newVisitor = new BranchCoverageVisitor(this.branches, processId);
      ES.traverse(root, newVisitor);
      this.rootReads.add(root.AranSerial);
    }
  }

  public getUniqueBranchesPartiallyVisited(): number {
    let counter = 0;
    for (let [_, branch] of this.branches) {
      if (branch.tookThenBranch()) {
        counter++;
      }
      if (branch.tookElseBranch()) {
        counter++;
      }
    }
    return counter;
  }

  public getBranchCoverage(): number {
    let branchesCovered = 0;
    let totalNrOfBranches = 0;
    for (let branch of this.branches) {
      totalNrOfBranches++;
      if (branch[1].isCovered()) {
        branchesCovered++;
      }
    }
    const branchCoverage = (totalNrOfBranches === 0) ? 0 : branchesCovered / totalNrOfBranches;
    return branchCoverage;
  }
}