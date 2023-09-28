import {Logger as Log} from "@src/util/logging";
import Chalk from "chalk";

export class AssertionFailed extends Error {
    constructor(message: string) {
        super(message);
    }
}

/**
 * Check assertion predicate and throw error + exit application when false
 * @param pred
 * @param message
 */
export default function assert(pred: boolean, message: string) {
    if (!pred) {
        Log.ALL(Chalk.bgRed(message));
        throw new AssertionFailed("ASSERTION FAILED: " + message);
    }
}