import Chalk from "chalk";

import BackendCommunication from "./BackendCommunication";
import {BackendResult, BackendResultType, InputAndEventsAndPathReceived, InputsAndEventsReceived} from "./BackendResult"
import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import iterationNumber from "@src/tester/iteration_number";
import {jsPrint} from "@src/util/io_operations";
import {Logger as Log} from "@src/util/logging";
import SymbolicFunction from "@src/symbolic_expression/symbolic_function";
import BackendResultState from "@src/backend/BackendResultState";
import Event from "@src/tester/Event";
import ComputedInput from "@src/backend/ComputedInput";
import SharedOutput from "@src/util/output.js";
import {
  ExecutionState,
  LocationExecutionState,
  StackFrame,
  TargetTriggeredExecutionState
} from "@src/tester/ExecutionState.js";
import CodePosition from "@src/instrumentation/code_position.js";
import EventWithId from "@src/tester/EventWithId.js";

// @ts-ignore
function parseExecutionState(json: any): ExecutionState {
  switch (json._type) {
    case "TARGET_TRIGGERED": return new TargetTriggeredExecutionState(json.eventId, json.processId);
    case "BASIC":
      let codePosition = new CodePosition(Number(json._position._file), json._position._serial);
      const stack = json._stack.map(stackFrameJson => new StackFrame(stackFrameJson.arrival, stackFrameJson.call));
      const currentEventSequence = [new EventWithId(json._currentEventSequence.processId, json._currentEventSequence.targetId, json._currentEventSequence.id)];
      return new LocationExecutionState(codePosition, stack, currentEventSequence, json._callSiteSerial);
  }
}

export default class ExploreModeBackendCommunication extends BackendCommunication {
    protected _i: number;
    constructor(inputPort: number, outputPort: number, backendId: number) {
        super(inputPort, outputPort, backendId);
        this._i = 0;
    }

    protected _generateTemplate(exploreType: string) {
        const type = (ConfigurationReader.config.MERGE_PATHS) ? "explore_merge" : "explore";
        // const type = "explore_merge";
        return {backend_id: this._backendId, iteration: iterationNumber.get(), type: type, "explore_type": exploreType};
    }

    protected _getProcessesInfo(allProcesses) {
        function getOtherProcessId() {
          // TODO Hack: only works for programs consisting of exactly 2 processes
          return (allProcesses[0].processId + 1) % 2;
        }
        // var otherProcess = -1;
        // if (allProcesses.length < 2) {
        //   otherProcess = (allProcesses[0].processId + 1) % 2;
        //   const newAllProcesses = [[], []];
        //   newAllProcesses[allProcesses[0].processId] = allProcesses[0];
        //   allProcesses = newAllProcesses;
        // }
        return allProcesses.map(function(process) {
          const id = process.processId === undefined ? getOtherProcessId() : process.processId;
          const totalNrOfEvents = process.getTotalNumberOfTargets ? process.getTotalNumberOfTargets() : 0;
          return { id, totalNrOfEvents };
        });
    }

    wrapMessage(globalPathConstraint, allProcesses, stoppedBeforeEnd: boolean, prescribedEvents: EventWithId[]) {
        const template = this._generateTemplate("get_new_path");
        const processesInfo = this._getProcessesInfo(allProcesses);
        const wrappedMessage = Object.assign(template, { PC: globalPathConstraint,
                                 finished_run: ! stoppedBeforeEnd,
                                 processes_info: processesInfo,
                                 branchCoverageInfo: SharedOutput.getOutput().makeBranchCoverageSnapshot(),
                                 "prescribed_events": prescribedEvents,
                                 eventHandlersCoverageInfo: SharedOutput.getOutput().makeEventHandlersExploredSnapshot()
        });
        return wrappedMessage;
    }

    handleBackendResult(parsedJSON): BackendResult {
        Log.BCK("ExploreModeBackendCommunication.handleBackendResult, parsedJSON =", parsedJSON);
        switch (parsedJSON.type) {
            case "SymbolicTreeFullyExplored":
                return {type: BackendResultType.Other, state: BackendResultState.FINISHED};
            case "InputAndEventSequence":
                const globalInputs: ComputedInput[] = parsedJSON.inputs.inputs.map(globalInput => {
                  const parsedES = globalInput.executionState;
                  // console.log("parsedES =", parsedES);
                  // let codePosition = new CodePosition(Number(parsedES._position._file), parsedES._position._serial);
                  // const stack = parsedES._stack.map(stackFrameJson => new StackFrame(stackFrameJson.arrival, stackFrameJson.call));
                  // const currentEventSequence = [new EventWithId(parsedES._currentEventSequence.processId, parsedES._currentEventSequence.targetId, parsedES._currentEventSequence.id)];
                  // const dummyES = new LocationExecutionState(codePosition, stack, currentEventSequence, parsedES._callSiteSerial);
                  const executionState = parseExecutionState(parsedES);
                  // console.log("executionState =", executionState);
                  const input: ComputedInput = {executionState: executionState, type: globalInput.type, id: globalInput.id, value: globalInput.value, processId: globalInput.processId};
                  return input;
                });
                const events: Event[] = parsedJSON.events;
                if (!events.every((e) => e.processId !== undefined && e.targetId !== undefined)) {
                    throw new Error("Received events did not have a process- and target-id");
                }
                events.forEach(function (e) { // Add .toString method to each event
                    e.toString = function () {
                        return `{pid:${this.processId};tid:${this.targetId}}`
                    }
                });
                Log.BCK("ExploreModeBackendCommunication.handleBackendResult, events =", events);
                return {type: BackendResultType.InputsAndEventsReceived,
                        state: BackendResultState.SUCCESS,
                        inputs: globalInputs,
                        events: events} as InputsAndEventsReceived;
            case "NewInput":
                return {type: BackendResultType.InputsAndEventsAndPathReceived,
                        state: BackendResultState.SUCCESS,
                        inputs: parsedJSON.inputs,
                        events: [],
                        path: parsedJSON.path} as InputAndEventsAndPathReceived;
            default:
                return {type: BackendResultType.Other, state: BackendResultState.INVALID};
        }
    }

    sendPathConstraint(globalPathConstraint, allProcesses, stoppedBeforeEnd: boolean, prescribedEvents: EventWithId[]) {
        const wrapped = this.wrapMessage(globalPathConstraint, allProcesses, stoppedBeforeEnd, prescribedEvents);
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);
    }

    getTryMergeBackendResult(): BackendResult {
        function handleTryMergeBackendResult(parsedJSON): BackendResult {
            switch (parsedJSON.type) {
                case "ActionSuccessful":
                  jsPrint(Chalk.bgGreen("MERGE SUCCESSFUL"));
                  return {type: BackendResultType.Other, state: BackendResultState.SUCCESS};
                case "ActionNotApplied":
                  jsPrint(Chalk.bgBlue("MERGE NOT APPLIED:"), parsedJSON.reason);
                  return {type: BackendResultType.Other, state: BackendResultState.INVALID};
                case "ActionFailed":
                  jsPrint(Chalk.bgRed("MERGE FAILED"));
                  return {type: BackendResultType.Other, state: BackendResultState.INVALID};
                default: throw new Error(parsedJSON);
            }
        }
        Log.BCK("BackendCommunication.getTryMergeBackendResult");
        return this.expectResult(handleTryMergeBackendResult);
    }

  _sanitizeStore(symbolicStore): any[] {
    const newStore: any[] = [];
    symbolicStore.forEach((value) => {
      if (! (value.exp.symbolic instanceof SymbolicFunction) && (value.id !== "this")) {
        newStore.push(value);
      }
    });
    return newStore;
  }

  sendTryMerge(executionState, globalPathConstraint, allProcesses, prescribedEvents): BackendResult {
    this._i++;
    jsPrint(Chalk.blue(`Attempting merge nr. ${this._i} with execution state ${executionState}`));
    const template = this._generateTemplate("try_merge");
    const processesInfo = this._getProcessesInfo(allProcesses);
    // const sanitizedStore = this._sanitizeStore(symbolicStore);
    const wrapped = Object.assign(template,
      {"i": this._i,
       "execution_state": executionState,
       "prescribed_events": prescribedEvents,
       "PC": globalPathConstraint,
       processes_info: processesInfo});
    const message = JSON.stringify(wrapped);
    this._sendOverSocket(message);
    return this.getTryMergeBackendResult();
  }
}