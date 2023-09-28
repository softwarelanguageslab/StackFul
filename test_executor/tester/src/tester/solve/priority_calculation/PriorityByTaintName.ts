import {Logger as Log} from "@src/util/logging";
import ReadWrite from "../read_write";
import {TaintAnalyzer} from "../taintAnalyzer";

export default class PriorityByTaintName {

    public static calculatePriority(taintAnalyzer: TaintAnalyzer, readWrite: ReadWrite, eventSeq, event): number {
        Log.PRI("Calcultating Taint Named WR Priority");
        const writeSet = readWrite.getWriteSet(eventSeq);
        const unvisitedBranchInfo = taintAnalyzer.getUnvisitedBranchInfo(event);
        Log.PRI("EventSeq", eventSeq.map((e) => e.targetId), "has writeSet", (writeSet) ? writeSet.toString() : "undefined")
        Log.PRI("Event", event.targetId, " has unvisited BranchInfo: ", undefined) // todo: show branchinfo

        // Extract all used variables from all these unvisited branches
        const usesUnion = new Set()
        unvisitedBranchInfo.forEach((branchInfo) => {
            branchInfo.uses.forEach(u => usesUnion.add(u))
        })

        // Compare with writeSet
        if (writeSet) {
            const intersection = new Set([...writeSet].filter(i => usesUnion.has(i)));
            return intersection.size;
        } else {
            return 0;
        }
    }
}

