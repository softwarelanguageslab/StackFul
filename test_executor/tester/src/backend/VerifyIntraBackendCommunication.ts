import ExploreModeBackendCommunication from "./ExploreModeBackendCommunication";
import iterationNumber from "../tester/iteration_number";
import Chalk from "chalk";
import {Logger as Log} from "../util/logging";
import SharedOutput from "@src/util/output.js";
import EventWithId from "@src/tester/EventWithId.js";

export class VerifyIntraBackendCommunication extends ExploreModeBackendCommunication {
    constructor(inputPort, outputPort, solveProcessId) {
        super(inputPort, outputPort, solveProcessId);
    }

    wrapMessage(globalPathConstraint, allProcesses, stoppedBeforeEnd: boolean, prescribedEvents: EventWithId[]) {
        let otherProcess = -1;
        if (allProcesses.length < 2) {  // TODO Hack: only works for programs consisting of exactly 2 processes
            otherProcess = (allProcesses[0].processId + 1) % 2;
            const newAllProcesses = [[], []];
            newAllProcesses[allProcesses[0].processId] = allProcesses[0];
            allProcesses = newAllProcesses;
        }
        const wrappedMessage = {
            backend_id: this._backendId,
            type: "verify_intra",
            explore_type: "explore",
            iteration: iterationNumber.get(),
            finished_run: ! stoppedBeforeEnd,
            branchCoverageInfo: SharedOutput.getOutput().makeBranchCoverageSnapshot(),
            eventHandlersCoverageInfo: SharedOutput.getOutput().makeEventHandlersExploredSnapshot(),
            "prescribed_events": prescribedEvents,
            PC: globalPathConstraint,
            processes_info: allProcesses.map(function (process) {
                const wrapped = {
                    id: (process.processId === undefined) ? otherProcess : process.processId,
                    totalNrOfEvents: process.getTotalNumberOfTargets ? process.getTotalNumberOfTargets() : 0
                };
                return wrapped;
            })
        };
        return wrappedMessage;
    }

    checkConnectsWithPC(pc1, pc2) {
        const wrapped = {
            backend_id: this._backendId,
            type: "verify_intra",
            vi_type: "connect",
            iteration: iterationNumber.get(),
            pc1: pc1,
            pc2: pc2
        };
        const message = JSON.stringify(wrapped);
        this._sendOverSocket(message);

        function react(parsedJSON) {
            Log.BCK(Chalk.bgBlue("Received input"), parsedJSON);
            switch (parsedJSON.type) {
                case "InputAndEventSequence":
                    return parsedJSON.inputs;
                case "NewInput":
                    return parsedJSON;
                case "UnsatisfiablePath":
                    return false;
                default:
                    throw Error(`Expected result of type NewInput or UnsatisfiablePath, but got ${parsedJSON.type}`);
            }
        }

        return this.expectResult(react);
    }


}