import * as CheckStringsTT from "./CheckDebuggingStringsTestTracer";
import TestTracer from "./TestTracer";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import {BenchmarksEnum} from "@src/config/inputPrograms/addProgram";
import TestingModesEnum from "@src/config/user_argument_parsing/TestingModesEnum";
import * as TT from "@src/tester/TestTracer/TestTracerAssertions";

/**
 * Find assertion items corresponding to current application and mode
 */
function getAssertionItems() {
    function makeErrorMessage() {
        return `No equivalent fixture for application ${ConfigReader.config.APPLICATION} and mode ${ConfigReader.config.MODE}`;
    }

    if (ConfigReader.config.MODE === TestingModesEnum.BruteForce) {
        switch (ConfigReader.config.APPLICATION) {
            case BenchmarksEnum.MAIN:
                return TT.MAIN_BRUTE;
            case BenchmarksEnum.FS_WHITEBOARD:
                return TT.FS_WHITEBOARD_BRUTE;
            case BenchmarksEnum.FS_VERIFY_INTRA:
                return TT.FS_VERIFY_INTRA_BRUTE;
            default:
                throw new Error(makeErrorMessage());
        }
    } else if (ConfigReader.config.MODE === TestingModesEnum.SymJS) {
        switch (ConfigReader.config.APPLICATION) {
            case BenchmarksEnum.MAIN:
                return TT.MAIN_SYMJS;
            case BenchmarksEnum.FS_WHITEBOARD:
                return TT.FS_WHITEBOARD_SYMJS;
            case BenchmarksEnum.NAMES_1:
                return TT.NAMES_1_SYMJS;
            case BenchmarksEnum.NAMES_2:
                return TT.NAMES_2_SYMJS;
            case BenchmarksEnum.FS_2BUTTONS:
                return TT.FS_2BUTTONS_SYMJS;
            default:
                throw new Error(makeErrorMessage());
        }
    } else {
        throw new Error(makeErrorMessage());
    }
}

var testTracerInstace: TestTracer = new TestTracer();

export function testTracerInit(): void {
    testTracerInstace = new TestTracer();
    if (ConfigReader.config.ENABLE_TESTING) {
        if (ConfigReader.config.MERGE_PATHS) {
            switch (ConfigReader.config.APPLICATION) {
                case BenchmarksEnum.MERGING1:
                    testTracerInstace = CheckStringsTT.merging_experiments1TT; break;
                case BenchmarksEnum.MERGING2:
                    testTracerInstace = CheckStringsTT.merging_experiments2TT; break;
                case BenchmarksEnum.MERGING3:
                    testTracerInstace = CheckStringsTT.merging_experiments3TT; break;
                case BenchmarksEnum.MERGING4:
                    testTracerInstace = CheckStringsTT.merging_experiments4TT; break;
                case BenchmarksEnum.MERGING5:
                    testTracerInstace = CheckStringsTT.merging_experiments5TT; break;
                case BenchmarksEnum.MERGING6:
                    testTracerInstace = CheckStringsTT.merging_experiments6TT; break;
                case BenchmarksEnum.MERGING7:
                    testTracerInstace = CheckStringsTT.merging_experiments7TT; break;
                default:
                    testTracerInstace._items = getAssertionItems();
            }
        } else {
            switch (ConfigReader.config.APPLICATION) {
                case BenchmarksEnum.MERGING1:
                    testTracerInstace = CheckStringsTT.merging_experiments1_no_mergingTT; break;
                case BenchmarksEnum.MERGING2:
                    testTracerInstace = CheckStringsTT.merging_experiments2_no_mergingTT; break;
                case BenchmarksEnum.MERGING3:
                    testTracerInstace = CheckStringsTT.merging_experiments3_no_mergingTT; break;
                case BenchmarksEnum.MERGING4:
                    testTracerInstace = CheckStringsTT.merging_experiments4_no_mergingTT; break;
                case BenchmarksEnum.MERGING5:
                    testTracerInstace = CheckStringsTT.merging_experiments5_no_mergingTT; break;
                case BenchmarksEnum.MERGING6:
                    testTracerInstace = CheckStringsTT.merging_experiments6_no_mergingTT; break;
                case BenchmarksEnum.MERGING7:
                    testTracerInstace = CheckStringsTT.merging_experiments7_no_mergingTT; break;
                default:
                    testTracerInstace._items = getAssertionItems();
            }
        }
    } else {
        // Ignore test inputs
        testTracerInstace.auxTest = (itemType, ...actualInputs) => {};
    }
}

export function testTracerGetInstance(): TestTracer {
    return testTracerInstace!;
}