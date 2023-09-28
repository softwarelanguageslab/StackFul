const Chalk = require("chalk");

import EventGraph from "@src/process/EventRacer/eventGraph";
import {Logger as Log} from "./logging";

export default class CountEventSeqs {
    eg: EventGraph;
    nrOfPossibleEventSequences: number;
    res: number;
    handledSet: Set<unknown>;
    
    constructor(eg: EventGraph) {
        this.eg = eg; //event graph
        this.nrOfPossibleEventSequences = 0;

        // aux variables
        this.res = 1;
        this.handledSet = new Set();
    }

    countNrOfPossibleEventSequences() {
        if (this.eg) {
            const splittedEGbyRoots = this.eg.splitEGbyRoots();

            var events = new Array(); // array of values from 0 to n with n nodes in event graph
            for (let i = 0; i < this.eg.getNrOfVertices(); i++) {
                events.push(i);
            }

            if (splittedEGbyRoots.length > 1) {
                const roots = this.eg.getRoots();
                const newRoot = events.length;
                this.eg.addVertex(newRoot); // temporarily add minimum node to event graph
                events = [newRoot].concat(events);
                for (const root of roots) {
                    this.eg.addEdge(newRoot, root);
                }
                this.countAux(events, newRoot);
                this.eg.AdjList.delete(newRoot); // remove minimum node
            } else {
                this.countAux(events, events[0]);
            }

            this.nrOfPossibleEventSequences = this.res;
            this.res = 1;
            this.handledSet = new Set();
        } else {
             Log.ALL(Chalk.red("[CoverageMetrics] no event graph defined!")); 
            }
    }

    countAux(events, root) {
        const eg = this.eg;
        const rootChildren: any[] = eg.getOutgoingEdgeNodes(root);
        var availableSlots = events.length;
        if (events.length < 1) {
            return this.res;
        }
        if (rootChildren.length > 1) {
            // events = events.slice(1);
            events.splice(events.indexOf(root), 1);
            availableSlots = events.length;
            for (const rc of rootChildren) {
                const rcc = [...new Set(eg.getAllChildren(rc))];
                const braidLength = rcc.length + 1;
                if (!this.handledSet.has(events)) {
                    // console.log(this.res + " * " + availableSlots + "C" + braidLength + "     (" + "events: " + events +")");
                    this.res = this.res * choose(availableSlots, braidLength);
                    this.handledSet.add(events);
                }
                const nextEvents = events.filter(e => [rc].concat(rcc).includes(e));
                this.countAux(nextEvents, rc);
            }
        } else {
            events = events.slice(1);
            this.countAux(events, events[0]);
        }
    }
}

function choose(n, r) {  // nCr
  return fact(n) / (fact(r) * fact(n - r));
}

function fact(n) {  // n!
  let res = 1;
  for (let i = 1; i <= n; i++) {
    res = res * i;
  }
  return res;
}
