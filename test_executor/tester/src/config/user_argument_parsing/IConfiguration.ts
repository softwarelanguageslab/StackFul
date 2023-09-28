import { BenchmarksEnum } from "../inputPrograms/addProgram";
import BrowsersEnum from "../BrowsersEnum";
import {SymJSAnalysisEnum} from "./SymJS";
import TestingModesEnum from "./TestingModesEnum";
import { CATEGORIES } from "@src/util/logging";

export default interface IConfiguration {
    APPLICATION: BenchmarksEnum
    BROWSER_TO_USE: BrowsersEnum
    DEFAULT_TIMEOUT_MS: number
    DEBUG: boolean
    ENABLE_TESTING: boolean
    INCLUDE_INITIAL_EVENT: boolean
    INPUT_SUFFIX: string
    LOG_ARAN: boolean
    MAX_NR_OF_ITERATIONS: number
    MAX_NR_OF_INTRA_ITERATIONS: number
    MERGE_PATHS: boolean
    MODE: TestingModesEnum
    SYMJS_ANALYSIS: SymJSAnalysisEnum
    PRIORITY_BASE: SymJSAnalysisEnum
    OUTPUT_SUFFIX: string
    OUTPUT_REL_PATH: string
    TEST_INDIVIDUAL_PROCESSES: boolean
    USE_STRINGS_ARGS: boolean
    USE_MARKED_FUNC: boolean
    INPUT_PORT: number
    OUTPUT_PORT: number
    LOGGING_CATEGORIES: readonly CATEGORIES[]

    LISTEN_FOR_PROGRAM: boolean;
    CREDENTIALS: string;
    UNINSTRUMENTED: boolean;
}