const Chalk = require("chalk");
import {jsPrint} from "../util/io_operations"
import findInSet from "../util/datastructures/ExtendedSet";

// build dictionary of executed event- and branch-sequences
export function storeBranchCoverage(symJSHandler) {
  var branchDict: object = {};
  var duplicatesDict: object = {};
  const exercisedStates = symJSHandler._exercisedStates;
  for (let i in exercisedStates) {
    let state = exercisedStates[i];
    let eventSeq = state.getEventSequence();
    let branchSeq = state.getBranchSequence();
    let targets = eventSeq.map(t => t.targetId);

    if (targets.length > 0) { // targets is empty only on first iteration (event registration)
      if (branchDict[targets] == undefined) { // if dictionary has no entry
        branchDict[targets] = [];
        duplicatesDict[targets] = 0;
      }
      if (branchDict[targets].includes(branchSeq)) { // only store unique branch sequences, otherwise increment duplicate count
        duplicatesDict[targets] += 1;
      } else {
        branchDict[targets].push(branchSeq);
      }
    }
  }
  for (const item of Object.entries(duplicatesDict)) { // only print when at least one duplicate branch sequence for the event sequence
    let events = item[0];
    let dupes = item[1];
    if (dupes > 0)
      jsPrint(Chalk.green(`duplicatesDict | [${events}], duplicate branch sequences: ${dupes}`));
  }
  return branchDict;
}

// Store event sequences, the branches that were executed for these event sequences and whether or not there are still branch sequences left to explore
export function storeBranchCompletion(symJSHandler, state, branchCompletion, executedBranches) {
  let queue = symJSHandler._priorityQueue._items.map(e => e.element); // priority queue of SymJSStates
  let eventSeq = state.getEventSequence(); // event sequence of current state being executed by tester
  let eventSeqIds = eSeqToIds(eventSeq);

  branchCompletion = updateOthers(queue, branchCompletion, executedBranches);

  let obj = getObjFromSeq(branchCompletion, eventSeqIds);
  if (obj) { // if event sequence already has entry in branchCompletion dictionary
    return setBranchesLeftToExplore(branchCompletion, queue, obj, eventSeqIds, executedBranches);
  } else { // if event sequence does not have entry in branchCompletion dictionary
    if (eventSeqIds.length > 0) {
      const filteredQueue = queue.filter(function(s) { return eventSeqEquals(eSeqToIds(s.getEventSequence()), eventSeqIds) });
      const oldBranches = executedBranches[eventSeqIds];
      const otherBranches = filteredQueue.map(s => s.getBranchSequence());
      var compObj = {
        eventSeq: eventSeqIds,
        branchesLeftToExplore: hasUniqueBranches(otherBranches, oldBranches),
        oldBranchSequences: oldBranches
      };
      branchCompletion.push(compObj); // add new object
      return branchCompletion;
    } else {
      return branchCompletion;
    }
  }
}

function setBranchesLeftToExplore(branchCompletion, queue, obj, eventSeqIds, executedBranches) {
  const filteredQueue = queue.filter(function(s) { return eventSeqEquals(eSeqToIds(s.getEventSequence()), eventSeqIds) }); // filter to get only states with same event sequence as current state
  const oldBranches = executedBranches[eventSeqIds];
  const otherBranches = filteredQueue.map(s => s.getBranchSequence());
  obj.branchesLeftToExplore = hasUniqueBranches(otherBranches, oldBranches); // determine if priority queue still has new branch sequences to explore for this event sequence
  obj.oldBranchSequences = oldBranches;
  return branchCompletion;
}

// loop over queue and update the 'branchesLeftToExplore' property for each event sequence
function updateOthers(queue, branchCompletion, executedBranches) {
  for (let i = 0; i < branchCompletion.length; i++){
    let obj = branchCompletion[i];
    let eventSeq = obj.eventSeq;
    setBranchesLeftToExplore(branchCompletion, queue, obj, eventSeq, executedBranches);
  }
  return branchCompletion;
}

function eSeqToIds(eventSeq) { // event sequence to array of target Ids ([Target, Target] --> [0, 1])
  if (eventSeq == []) {
    return eventSeq;
  } else {
    return eventSeq.map(t => t.targetId);
  }
}

function eventSeqEquals(seq1, seq2) { // check equality between two event sequences
  if (seq1 === seq2) return true;
  if (seq1 == null || seq2 == null) return false;
  if (seq1.length != seq2.length) return false;

  for (let i = 0; i < seq1.length; i++){
    if (seq1[i] != seq2[i]) return false;
  }
  return true;
}

function getObjFromSeq(dict, seq) { // get obj in array of objects based on 'eventSeq' property
  var foundObj;
  dict.forEach(function(obj) {
    if (eventSeqEquals(obj.eventSeq, seq))
      foundObj = obj;
  });
  return foundObj;
}

function hasUniqueBranches(currentIterationBranches, executedBranches) { // check if currentIterationBranches has branches not included in executedBranches
  const m = new Set(currentIterationBranches.concat(executedBranches));
  const r = [...m];
  return r.length > executedBranches.length;
}



export function analyzeSharedVariables(symJSHandler) {
  const events = symJSHandler._eventHandlers;

  const readWrite = symJSHandler._readWrite;

  const intersection: object = {};

  for (let idx in events) {
    const event = events[idx];
    const readSet = readWrite.getReadSet(event);

    for (let idx2 in events) {
      if (idx === idx2)
        continue;
      var writeSet = readWrite.getWriteSet([events[idx2]]);
      if (writeSet == null) {
        writeSet = readWrite.getWriteSetIncluding(events[idx2]);
      }
      var extraWriteSet = readWrite.getWriteSetIncluding(events[idx2]);

      if (readSet && writeSet) {
        writeSet.forEach((lastWrittenValue, variable) => {
          const valuesReadForVariable = readSet.get(variable);
          if (valuesReadForVariable && !variable.startsWith('__anonymous__') && findInSet(valuesReadForVariable, (valueRead) => {
            return !valueRead.isSameSymValue(lastWrittenValue);
          })) {
            const key: any = [event.targetId, events[idx2].targetId];
            if (intersection[key]) {
              intersection[key].add(variable);
            } else {
              intersection[key] = new Set();
              intersection[key].add(variable);
            }
          }
        });
      }
      if (readSet && extraWriteSet) {
        extraWriteSet.forEach((lastWrittenValue, variable) => {
          const valuesReadForVariable = readSet.get(variable);
          if (valuesReadForVariable && !variable.startsWith('__anonymous__') && findInSet(valuesReadForVariable, (valueRead) => {
            return !valueRead.isSameSymValue(lastWrittenValue);
          })) {
            const key: any = [event.targetId, events[idx2].targetId];
            if (intersection[key]) {
              intersection[key].add(variable);
              jsPrint(`[extrawriteset] added ${variable} to ${key}`);
            } else {
              intersection[key] = new Set();
              intersection[key].add(variable);
              jsPrint(`[extrawriteset] added ${variable} to ${key}`);
            }
          }
        });
      }
    }
  }
  return getEventHandlersFromIntersection(intersection);
}

function getEventHandlersFromIntersection(intersection: object) {
  let variables: any[] = Object.keys(intersection).map(function(key) { return [...intersection[key]] }); // get shared variables between all event handlers
  variables = [].concat.apply([], variables); // flatten array
  const t = new Set(variables);
  variables = [...t] // remove duplicates
  const result = {};

  // group by shared variable
  for (const sharedVar of variables) {
    let newKey: any = new Set();
    for (let [key, value] of Object.entries(intersection)) {
      if ([...value].includes(sharedVar)) {
        key.split(",").forEach(ehId => newKey.add(ehId));
      }
    }
    newKey = [...newKey].sort();
    if (result[newKey]) {
      result[newKey].push(sharedVar);
    } else {
      result[newKey] = [sharedVar];
    }
  }
  return result;
}


// getEventHandlersFromIntersection:
// from:
// { '0,1': Set { 'shared1' },
//   '0,2': Set { 'shared1' },
//   '1,0': Set { 'shared1' },
//   '1,2': Set { 'shared1' },
//   '2,0': Set { 'shared2' },
//   '2,1': Set { 'shared2' } }

// to:
// { [0,1,2]: Set { 'shared1' },
//   [0,1,2]: Set { 'shared2' } }



