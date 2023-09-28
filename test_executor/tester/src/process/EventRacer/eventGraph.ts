import Chalk from "chalk";
import {Logger as Log} from "../../util/logging";

export default class EventGraph {
  AdjList: Map<any, any>;
  constructor() {
    this.AdjList = new Map();
  }
  addVertex(v) {
    if (!this.AdjList.has(v))
      this.AdjList.set(v, []);
  }
  hasVertex(v) {
    return this.AdjList.has(v);
  }
  addEdge(v, w) {
    if (!this.AdjList.get(v).includes(w))
      this.AdjList.get(v).push(w);
  }
  getNrOfVertices() {
    return this.AdjList.size;
  }
  getIncomingEdgeNodes(eventId) {
    var incomingEdgeNodes: any[] = [];
    this.AdjList.forEach((value, key, map) => {
      if (value.includes(eventId))
        incomingEdgeNodes.push(key);
    });
    return incomingEdgeNodes;
  }
  getOutgoingEdgeNodes(eventId) {
    var outgoingEdgeNodes:any [] = [];
    this.AdjList.forEach((value, key, map) => {
      if (key == eventId)
        outgoingEdgeNodes.push(value);
    });
    return [].concat.apply([], outgoingEdgeNodes);
  }
  getAllIncomingNodes(eventId) {
    return [...new Set(this.getAllIncomingNodesAux(eventId, []))];
  }
  getAllIncomingNodesAux(eventId, acc) {
    this.AdjList.forEach((value, key, map) => {
      if (value.includes(eventId)) {
        acc.push(key);
        this.getAllIncomingNodesAux(key, acc);
      }
    });
    return acc;
  }
  getAllChildren(eventId) {
    const that = this;
    var outgoingEdgeNodes: any[] = [];
    this.AdjList.forEach((value, key, map) => {
      if (key == eventId) {
        outgoingEdgeNodes.push(value);
        for (const i of value)
          outgoingEdgeNodes.push(that.getAllChildren(i));
      }
    });
    var res = outgoingEdgeNodes.filter(el => {
      return el != null && el != '';
    });
    return [].concat.apply([], res); // flatten
  }
  getMaximumNodes() {
    var maxNodes: any[] = [];
    const events = new Array(this.getNrOfVertices());
    for (let i = 0; i < events.length; i++)
      events[i] = i;
    for (const evt of events) {
      if (this.getOutgoingEdgeNodes(evt).length == 0)
        maxNodes.push(evt);
    }
    return maxNodes;
  }
  getRoots() {
    var roots: any[] = [];
    this.AdjList.forEach((value, key, map) => {
      if (this.getIncomingEdgeNodes(key).length == 0)
        roots.push(key);
    });
    return roots;
  }
  splitEGbyRoots() {
    return this.getRoots().map(e => [...new Set([e].concat(this.getAllChildren(e)))]);
  }
  printGraph() {
    var get_keys = this.AdjList.keys();

    Log.DRF(Chalk.bgGreen('EventGraph:'));
    for (var i of get_keys) {
      var get_values = this.AdjList.get(i);
      var conc = "";
      if (get_values.length == 0)
        continue;
      for (var j of get_values)
        conc += j + ", ";

      Log.DRF(Chalk.green(i + " -> " + conc));
    }
  }
}

module.exports = EventGraph;
