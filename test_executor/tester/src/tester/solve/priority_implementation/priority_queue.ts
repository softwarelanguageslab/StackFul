import Chalk from "chalk";

import {Logger as Log} from "../../../util/logging";
import Priority from "./priority";
import {testTracerGetInstance} from "@src/tester/TestTracer/TestTracerInstance";

/* Based on: https://www.geeksforgeeks.org/implementation-priority-queue-javascript/ */

export class QElement {
    public element: any;
    public priority: any;
    constructor(element, priority) {
      this.element = element;
      this.priority = priority;
    }
}
  
export class PriorityQueue {
    _items: any[];
    private _elementComparisonFunction: any;
    maxPriority: any;
    minPriority: any;

    constructor(elementComparisonFunction) {
      // An array is used to implement priority
      this._items = [];
      this._elementComparisonFunction = elementComparisonFunction;
        this.maxPriority = new Priority(+Infinity, -Infinity);
        this.minPriority = new Priority(-Infinity, +Infinity);
    }

    // enqueue function to add element
    // to the queue as per priority
    enqueue(element: any, priority: any): void {
      // creating object from queue element
      const qElement: QElement = new QElement(element, priority);
      var contain: boolean = false;

      for (let i = 0; i < this._items.length; i++) {
        if (this._elementComparisonFunction(element, this._items[i].element)) {
          if (! priority.equalTo(this._items[i].priority)) {
            Log.ALL(Chalk.red(`Inserting an existing element into priority queue with a different priority: old: ${this._items[i].priority}, new: ${priority}`));
          }
          return;
        }
      }

      // iterating through the entire
      // item array to add element at the
      // correct location of the Queue
      for (let i = 0; i < this._items.length; i++) {
          if (qElement.priority.greaterThan(this._items[i].priority)) {
              // Once the correct location is found it is enqueued
              this._items.splice(i, 0, qElement);
              contain = true;
              break;
          }
      }

      // if the element have the highest priority
      // it is added at the end of the queue
      if (!contain) {
          this._items.push(qElement);
      }
    }

    // Dequeue method to remove element from the queue
    // If the queue is empty returns undefined
    dequeue(): any {
        if (this.isEmpty()) {
          return undefined;
        }
        const nextQElement = this._items.shift();
        Log.ALL(Chalk.blue(`Dequeueing element ${nextQElement.element} with priority ${nextQElement.priority}`));
        testTracerGetInstance().testStateDequeued(nextQElement.element, nextQElement.priority);
        return nextQElement.element;
    }

    size(): number {
      return this._items.length;
    }

    isEmpty(): boolean {
      return this.size() === 0;
    }

    forEach(f): void {
      this._items.forEach(f);
    }
}
