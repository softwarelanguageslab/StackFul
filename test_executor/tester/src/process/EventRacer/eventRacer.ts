import Chalk from "chalk";
import EventGraph from "./eventGraph"
import {Logger as Log} from "../../util/logging";

const POST = "post";
const FORK = "fork";
const DEQ = "deq";
const END = "end";
const READ = "read";
const WRITE = "write";

export class EventRacer {
  sharedVars: any;
  trace: any;
  vectorClocks: any;
  eventGraph: any;
  analysisState: any[];
  detectedDataRaces: any[];
  coveredRaces: any[];
  nodeToEventIdMap: {};
  currentEvent: null;
  iterationNumber: number;
  currentTrace: any;

  constructor() {
    this.sharedVars = {};
    this.trace = {};

    this.vectorClocks = {};
    this.eventGraph = new EventGraph();
    this.analysisState = [this.vectorClocks, this.eventGraph];

    this.detectedDataRaces = [];
    this.coveredRaces = [];

    // auxilliary variables
    this.nodeToEventIdMap = {}; // node id to index within vector clocks
    this.currentEvent = null;
    this.iterationNumber = 0;
    this.currentTrace = this.trace[this.iterationNumber];
  }

  detectRaces() {
    for (var [key, value] of Object.entries(this.sharedVars)) {
      var eventIds = key.split(",").map(e => parseInt(e));
      for (const ev1 of eventIds) {
        for (const ev2 of eventIds) {
          if (ev1 == ev2 || this.happensBefore(ev1, ev2) || this.happensBefore(ev2, ev1)) {
            continue;
          } else {
            for (const sharedVar of value as any) {
              const relOps = this.countOccurrences(ev1, ev2, sharedVar, this.currentTrace);
              if (relOps.writeEv1 > 0 && relOps.writeEv2 > 0) {
                this.addDataRace("write(" + ev1 + ", " + sharedVar + ")", "write(" + ev2 + ", " + sharedVar + ")");
              }
              if (relOps.readEv1 > 0 && relOps.writeEv2 > 0) {
                this.addDataRace("read(" + ev1 + ", " + sharedVar + ")", "write(" + ev2 + ", " + sharedVar + ")");
              }
              if (relOps.writeEv1 > 0 && relOps.readEv2 > 0) {
                this.addDataRace("write(" + ev1 + ", " + sharedVar + ")", "read(" + ev2 + ", " + sharedVar + ")");
              }
            }
          }
        }
      }
    }
    this.raceCoverage();
  }
  countOccurrences(ev1, ev2, sharedVar, trace) {
    var res = { 'readEv1': 0, 'readEv2': 0, 'writeEv1': 0, 'writeEv2': 0 };
    const dummyReadEv1 = "read(" + ev1 + ", " + sharedVar + ")";
    const dummyReadEv2 = "read(" + ev2 + ", " + sharedVar + ")";
    const dummyWriteEv1 = "write(" + ev1 + ", " + sharedVar + ")";
    const dummyWriteEv2 = "write(" + ev2 + ", " + sharedVar + ")";
    for (const traceEl of trace) {
      switch (traceEl) {
        case dummyReadEv1:
          res.readEv1 += 1;
          break;
        case dummyReadEv2:
          res.readEv2 += 1;
          break;
        case dummyWriteEv1:
          res.writeEv1 += 1;
          break;
        case dummyWriteEv1:
          res.writeEv1 += 1;
          break;
      }
    }
    return res;
  }
  addDataRace(op1, op2) {
    const races = JSON.stringify(this.detectedDataRaces);
    const newRace = JSON.stringify([op1, op2]);
    const newRaceRev = JSON.stringify([op2, op1]);
    var included = races.indexOf(newRace);
    var includedRev = races.indexOf(newRaceRev);
    if (included == -1 && includedRev == -1)
      this.detectedDataRaces.push([op1, op2]);
  }
  // We say that a race R = (a, b) is covered by race S = (c, d) if the following conditions hold:
  //  1. ev(a) happens before ev(c), and
  //  2. d happends before b
  raceCoverage() {
    for (var r of this.detectedDataRaces) {
      const a = r[0];
      const b = r[1];
      // const evA = this.getEventIdFromTraceOp(a);
      // const evB = this.getEventIdFromTraceOp(b);
      for (var s of this.detectedDataRaces) {
        const c = s[0];
        const d = s[1];
        // const evC = this.getEventIdFromTraceOp(c);
        // const evD = this.getEventIdFromTraceOp(d);
        if (a == c && b == d) // a race cannot cover itself
          continue;
        if (this.occursBefore(a, c) && this.occursBefore(d, b)) {
          this.coveredRaces.push(r);
          this.coveredRaces = [...new Set(this.coveredRaces)]; // remove duplicate races
          Log.DRF(Chalk.blue(`race [${r}] is covered by race [${s}]`));
        } else {
        }
      }
    }
  }
  getEventIdFromTraceOp(op) {
    return parseInt(op.slice(op.indexOf("(") + 1, op.indexOf(")")).split(", ")[0]);
  }
  getVariableFromTraceOp(op) {
    return op.slice(op.indexOf("(") + 1, op.indexOf(")")).split(", ")[1];
  }
  constructEventGraph() {
    for (let i = 0; i < this.currentTrace.length; i++) {
      const traceEl = this.currentTrace[i];
      const eventId = this.getEventIdFromTraceEl(traceEl);
      if (traceEl.startsWith(POST) || traceEl.startsWith(DEQ)) {
        this.eventGraph.addVertex(eventId); // add node to graph for every executed event
      }
      if (traceEl.startsWith(FORK)) { // find which event forked this event
        var nodeFrom;
        const nodeTo = eventId;
        for (let j = i - 1; j >= 0; j--) { // go backwards in trace and find deq of event that forked this event
          var traceElFrom = this.currentTrace[j];
          var eventIdFrom = this.getEventIdFromTraceEl(traceElFrom);
          if (traceElFrom.startsWith(DEQ)) {
            nodeFrom = eventIdFrom;
            break;
          }
        }
        if (nodeFrom == null) {
          Log.DRF(Chalk.bgRed(`IMPOSSIBLE | No nodeFrom found for FORK(${eventId})`));
        } else {
          const incomingNodes = this.eventGraph.getIncomingEdgeNodes(nodeFrom);
          this.eventGraph.addVertex(nodeTo);
          if (!incomingNodes.includes(nodeTo)) {
            this.eventGraph.addEdge(nodeFrom, nodeTo);
          } else {
            Log.DRF(Chalk.bgBlue(`Skipped adding edge ${nodeFrom} -> ${nodeTo} to avoid cycle`));
          }
        }
      }
    }
    this.eventGraph.printGraph();
  }
  // 'deq(1)' --> 1 (only use for simple trace elements, not for operations)
  getEventIdFromTraceEl(traceEl) {
    return parseInt(traceEl.match(/(\d+)/)[0]);
  }
  // based on the graph, create vector clock for each of the graph's nodes
  constructVectorClocks() {
    const vcLength = this.eventGraph.getNrOfVertices();
    for (let i = 0; i < vcLength; i++) { // create vector clocks for each node
      const eventId: any = Array.from(this.eventGraph.AdjList.keys())[i];
      let vc = Array(vcLength).fill(0);
      this.nodeToEventIdMap[eventId] = i;
      vc[i] = 1;
      this.vectorClocks[eventId] = vc;
    }
    for (let i = 0; i < vcLength; i++) { // fill in vector clocks for each node
      const eventId: any = Array.from(this.eventGraph.AdjList.keys())[i];
      let vc = this.vectorClocks[eventId];
      const incomingEdgeNodes = this.eventGraph.getIncomingEdgeNodes(eventId);
      if (incomingEdgeNodes.length > 0) {
        for (const node of incomingEdgeNodes) {
          let nodeVC = this.vectorClocks[node];
          vc = this.vcUnion(vc, nodeVC);
        }
      }
      this.vectorClocks[eventId] = vc;
    }
  }
  vcUnion(vc1, vc2) {
    let vc = vc1.slice();
    for (let i = 0; i < vc1.length; i++) {
      if ((vc1[i] + vc2[i]) > 0) {
        vc[i] = 1;
      }
    }
    return vc;
  }
  isEventSequencePossible(eventSeq: any[]) {
    const incomingEdges = eventSeq.map(id => this.eventGraph.getIncomingEdgeNodes(id));
    let possible = true;
    for (const idx in eventSeq) {
      if (!(incomingEdges[idx].length == 0 || incomingEdges[idx].some(e => eventSeq.includes(e))))
        possible = false;
    }
    return possible;
  }
  isBranchingEvent(event) {
    for (const [from, to] of this.eventGraph.AdjList) {
      if (from == event && to.length > 1)
        return true;
    }
    return false;
  }
  //  "returns true when a occurs before b in the trace"
  occursBefore(a, b) {
    const evA = this.getEventIdFromTraceOp(a);
    const evB = this.getEventIdFromTraceOp(b);
    if (evA != evB) {
      return this.happensBefore(evA, evB);
    } else {
      const idxA = this.currentTrace.indexOf(a);
      const idxB = this.currentTrace.indexOf(b);
      if (idxA == -1 || idxB == -1)
        return false;
      return idxA < idxB;
    }
  }
  // a and b are eventIds, a happens before b when a is connected to b in the eventGraph
  happensBefore(a, b) {
    if (a == b)
      return false;
    const vcIdxA = this.nodeToEventIdMap[a];
    const vcIdxB = this.nodeToEventIdMap[b];
    const vcB = this.vectorClocks[b];
    if (vcIdxA != null && vcIdxB != null && vcB != null) {
      return vcB[vcIdxA] == 1;
    } else {
      return false;
    }
  }
  getEventGraph() {
    return this.eventGraph;
  }
  getNrOfDataRaces() {
    return this.detectedDataRaces.length;
  }
  // build trace (POST = addEventListener, FORK = event created within another event, DEQ = event dispatched, END = event finishes)
  newEventDiscovered(eventId) {
    if (this.isDynamicListener()) { // if event listener is declared within another event listener
      this.currentTrace.push(FORK + '(' + eventId + ')');
      Log.DRF(Chalk.bgBlue(`FORK(${eventId})`));
    } else {
      this.currentTrace.push(POST + '(' + eventId + ')');
      Log.DRF(Chalk.bgBlue(`POST(${eventId})`));
    }
  }
  eventDispatched(eventId) {
    this.currentTrace.push(DEQ + '(' + eventId + ')');
    Log.DRF(Chalk.bgBlue(`DEQ(${eventId})`));
    this.currentEvent = eventId;
  }
  eventEnded(eventId) {
    this.currentTrace.push(END + '(' + eventId + ')');
    Log.DRF(Chalk.bgBlue(`END(${eventId})`));
    this.currentEvent = null;
  }
  readSharedVariable(v): void {
    if (this.currentEvent != null) {
      this.currentTrace.push(READ + '(' + this.currentEvent + ', ' + v + ')'); // trace element: read(1, varName)
      Log.DRF(Chalk.bgBlue(`READ(${this.currentEvent}, ${v})`));
    }
  }
  writeSharedVariable(v): void {
    if (this.currentEvent != null) {
      this.currentTrace.push(WRITE + '(' + this.currentEvent + ', ' + v + ')');
      Log.DRF(Chalk.bgBlue(`WRITE(${this.currentEvent}, ${v})`));
    }
  }
  isDynamicListener() {
    for (let i = this.currentTrace.length - 1; i > -1; i--) {
      const traceEl = this.currentTrace[i];
      if (traceEl.startsWith(DEQ))
        return true;
      if (traceEl.startsWith(END))
        return false;
    }
  }
  printDebugs() {
    Log.DRF(Chalk.bgGreen("Trace:"));
    Log.DRF(this.currentTrace);

    Log.DRF(Chalk.bgGreen("Vector clocks: "));
    Log.DRF(Object.keys(this.vectorClocks).length);

    Log.DRF(Chalk.bgGreen("Detected data races: "));
    Log.DRF(this.detectedDataRaces);

    Log.DRF(Chalk.bgGreen("Covered data races: "));
    Log.DRF(this.coveredRaces);
  }
  reset() {
    this.iterationNumber += 1;
    this.currentTrace = [];
    // this.trace = [];
    this.nodeToEventIdMap = {};
    this.vectorClocks = {};
    // this.eventGraph = new EventGraph();
  }
}
