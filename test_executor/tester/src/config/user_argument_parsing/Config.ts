import Minimist, {Opts} from "minimist";
import Path from "path";

import BrowsersEnum from "../BrowsersEnum";
import IConfiguration from "./IConfiguration";
import {stringToAnalysis, SymJSAnalysisEnum} from "./SymJS";
import TestingModesEnum, {stringToMode} from "./TestingModesEnum";
import {BenchmarksEnum} from "../inputPrograms/addProgram";
import {stringToApplication} from "../inputPrograms/InputProgram";
import {CATEGORIES} from "@src/util/logging";
import UnknownUserArgumentNameError from "@src/config/user_argument_parsing/UnknownUserArgumentNameError";
import UnknownUserArgumentValueError from "@src/config/user_argument_parsing/UnknownUserArgumentValueError";

const defaultConfig: IConfiguration = <const> {
    APPLICATION: BenchmarksEnum.FS_CALCULATOR,
    BROWSER_TO_USE: BrowsersEnum.JSDOM,
    DEFAULT_TIMEOUT_MS: 1250,
    DEBUG: true,
    ENABLE_TESTING: false, // Perform automated end-to-end testing of the testing process itself.
    INCLUDE_INITIAL_EVENT: false,
    INPUT_SUFFIX: "",
    LOG_ARAN: false,
    MAX_NR_OF_ITERATIONS: 500,
    MAX_NR_OF_INTRA_ITERATIONS: 250,
    MERGE_PATHS: false,
    MODE: TestingModesEnum.SymJS,
    SYMJS_ANALYSIS: SymJSAnalysisEnum.event_handler_first,
    PRIORITY_BASE: SymJSAnalysisEnum.named,
    OUTPUT_SUFFIX: "",
    OUTPUT_REL_PATH: Path.join(__dirname, "../../../../output"),
    TEST_INDIVIDUAL_PROCESSES: false,
    USE_STRINGS_ARGS: false,
    USE_MARKED_FUNC: false,
    INPUT_PORT: 9877, // For backend communications
    OUTPUT_PORT: 9876, // For backend communications
    LOGGING_CATEGORIES: [],
    LISTEN_FOR_PROGRAM: false,
    CREDENTIALS: "",
    UNINSTRUMENTED: false
}

export default class ConfigurationReader {

    private static _config: IConfiguration = Object.assign({}, defaultConfig)
    private static _initialized: boolean = false

    static get config(): IConfiguration {
        if (!ConfigurationReader._initialized) {
            throw new Error("Config was not yet initialized.")
        }
        return ConfigurationReader._config;
    }

    private static loggingStringToCategory(loggingString: string): CATEGORIES {
        switch (loggingString.toUpperCase()) {
            case "ADV": return CATEGORIES.ADV;
            case "ALL": return CATEGORIES.ALL;
            case "BCK": return CATEGORIES.BCK;
            case "CBL": return CATEGORIES.CBL;
            case "EVT": return CATEGORIES.EVT;
            case "FUN": return CATEGORIES.FUN;
            case "INS": return CATEGORIES.INS;
            case "MRG": return CATEGORIES.MRG;
            case "NDP": return CATEGORIES.NDP;
            case "NON": return CATEGORIES.NON;
            case "PRI": return CATEGORIES.PRI;
            case "PSI": return CATEGORIES.PSI;
            case "SCR": return CATEGORIES.SCR;
            case "SOC": return CATEGORIES.SOC;
            case "STO": return CATEGORIES.STO;
            case "VRW": return CATEGORIES.VRW;
            default: throw new UnknownUserArgumentNameError(loggingString)
        }
    }

    static handleArguments(...args): IConfiguration {
        const minimistOptions: Opts = {
            alias: {
                "a": "application",
                "d": "debug",
                "explore-events": "explore-events",
                "i": "individual",
                "input-suffix": "input-suffix",
                "intra-iterations": "intra-iterations",
                "l": "log",
                "listen": "listen",
                "log-aran": "log-aran",
                "m": "mode",
                "max": "max-iterations",
                "merge": "merging",
                "output-suffix": "output-suffix",
                "symjs-analysis": "symjs-analysis",
                "t": "test",
                "use-string-args": "use-string-args",
                "default-timeout": "default-timeout-ms"
            },
            boolean: ["d", "explore-events", "i", "listen", "log-aran", "merge", "t", "uninstrumented", "use-string-args"],
            string: ["a", "credentials", "input-suffix", "l", "m", "output-rel-path", "output-suffix", "symjs-analysis"],
            unknown: function(unknownArg: string): boolean {
                throw new UnknownUserArgumentNameError(unknownArg);
            }
        }
        const usersConfig: any = {};
        const options = Minimist(...args, minimistOptions);
        if (options.m || options.mode) {
            usersConfig.MODE = stringToMode(options.m || options.mode);
        }
        if (options.a || options.application) {
            usersConfig.APPLICATION = stringToApplication(options.a || options.application);
        }
        if (options.d || options.debug) {
            usersConfig.DEBUG = true;
        }
        if (options.t || options.test) {
            usersConfig.ENABLE_TESTING = true;
        }
        if (options.i || options.individual || usersConfig.MODE === TestingModesEnum.Verify) {
            usersConfig.TEST_INDIVIDUAL_PROCESSES = true;
        }
        if (options.max || options["max-iterations"]) {
            usersConfig.MAX_NR_OF_ITERATIONS = options.max || options["max-iterations"];
        }
        if (options.l) {
            const userArgument = options.l;
            if (typeof(userArgument) === "string") {
                const loggingCategory = this.loggingStringToCategory(userArgument);
                usersConfig.LOGGING_CATEGORIES = [loggingCategory];
            } else if (Array.isArray(userArgument)) {
                let loggingCategories = (userArgument as string[]).map(this.loggingStringToCategory);
                if (loggingCategories.includes(CATEGORIES.NON)) {
                    loggingCategories = [];
                }
                usersConfig.LOGGING_CATEGORIES = loggingCategories;
            } else {
                throw new UnknownUserArgumentValueError("l", userArgument);
            }
        }
        if (options["log-aran"]) {
            usersConfig.LOG_ARAN = true;
        }
        if (options["explore-events"]) {
            usersConfig.INCLUDE_INITIAL_EVENT = true;
        }
        if (options["input-suffix"]) {
            usersConfig.INPUT_SUFFIX = options["input-suffix"];
        }
        if (options["output-suffix"]) {
            usersConfig.OUTPUT_SUFFIX = options["output-suffix"];
        }
        if (options["intra-iterations"]) {
            usersConfig.MAX_NR_OF_INTRA_ITERATIONS = options["intra-iterations"];
        }
        if (options["uninstrumented"]) {
            usersConfig.UNINSTRUMENTED = true;
        }
        if (options["use-string-args"]) {
            usersConfig.USE_STRINGS_ARGS = true;
        }
        if (options["merge"] || options["merging"]) {
            usersConfig.MERGE_PATHS = true;
        }
        if (options["symjs-analysis"]) {
            usersConfig.SYMJS_ANALYSIS = stringToAnalysis(options["symjs-analysis"]);
        }
        if (options["output-rel-path"]) {
            usersConfig.OUTPUT_REL_PATH = options["output-rel-path"]
        }
        if (options["listen"]) {
            usersConfig.LISTEN_FOR_PROGRAM = true
            usersConfig.UNINSTRUMENTED = true;
        }
        if (options["credentials"]) {
            usersConfig.CREDENTIALS = options["credentials"]
        }
        if (options["default-timeout"] || options["default-timeout-ms"]) {
            usersConfig.DEFAULT_TIMEOUT_MS = options["default-timeout"] || options["default-timeout-ms"];
        }
        ConfigurationReader._config = Object.assign({}, defaultConfig, usersConfig);
        ConfigurationReader._initialized = true;
        return ConfigurationReader._config;
    }
}

export const startTime = Date.now();