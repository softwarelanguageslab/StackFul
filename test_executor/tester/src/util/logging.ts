import {jsPrint} from "./io_operations";

export enum CATEGORIES {
    BCK = 0x000001,  // Communication with the backd
    PRI = 0x000002,  // SymJS Priority calculation / chae
    SCR = 0x000004,  // SymJS States cread
    SOC = 0x000008,  // Socket emits/receis
    VRW = 0x000010,  // SymJS read/write
    CBL = 0x000020,  // Async callback loop (used by the TestRunn)
    NDP = 0x000040,  // Non-determinist input parameters cread
    PSI = 0x000080,  // FunctionSummaries Path information prind
    FUN = 0x000100,  // FunctionSummaries Functions entered/left
    ADV = 0x000200,  // Logs from advice functions
    DRF = 0x000400,  // Data-races-first heuristic
    EVT = 0x000800,  // Events triggered
    STO = 0x001000,  // Symbolic store
    INS = 0x002000,  // Instrumented JavaScript code
    MRG = 0x004000,  // Operations related to state-merging operations
    ALL = ADV | BCK | CBL | DRF | FUN | INS | MRG | NDP | PRI | PSI | SCR | SOC | STO | VRW | EVT,
    NON = 0,
}

class LogHelper {

    private toLog: number = CATEGORIES.NON; //change the logging categories in Config.ts

    private logCategoriesToNumber(flags: readonly CATEGORIES[]): number {
        var result = 0;
        flags.forEach(flag => result |= flag);
        return result;
    }

    setLogCategories(flags: readonly CATEGORIES[]): void {
        const toNumber = this.logCategoriesToNumber(flags);
        this.toLog = toNumber;
    }

    _log(flag: number, ...rest): void {
        if ((flag & this.toLog) !== 0) {
            jsPrint(...rest);
        }
    }

    /**
     * log to all categories
     */
    ALL(...args) {
        jsPrint(...args);
    }

    /**
     * log communication with backend
     */
    BCK(...args) {
        this._log(CATEGORIES.BCK, ...args);
    }

    /**
     * log async callback loop (used by the TestRunner)
     */
    CBL(...args) {
        this._log(CATEGORIES.CBL, ...args);
    }

    /**
     * log events triggered
     */
    EVT(...args) {
        this._log(CATEGORIES.EVT, ...args);
    }

    /**
     * log function entered/left
     */
    FUN(...args) {
        this._log(CATEGORIES.FUN, ...args);
    }

    /**
     * log instrumented JavaScript code
     */
    INS(...args) {
        this._log(CATEGORIES.INS, ...args);
    }

    /**
     * log operations related to state-merging operations
     */
    MRG(...args) {
        this._log(CATEGORIES.MRG, ...args);
    }

    /**
     * log Non-determinist input parameters created
     */
    NDP(...args) {
        this._log(CATEGORIES.NDP, ...args);
    }

    /**
     * log a SymJS priority calculation / change
     */
    PRI(...args) {
        this._log(CATEGORIES.PRI, ...args);
    }

    /**
     * log path information
     */
    PSI(...args) {
        this._log(CATEGORIES.PSI, ...args);
    }

    /**
     * log states created
     */
    SCR(...args) {
        this._log(CATEGORIES.SCR, ...args);
    }

    /**
     * log socket emits/receives
     */
    SOC(...args) {
        this._log(CATEGORIES.SOC, ...args);
    }

    /**
     * log symbolic store operations
     */
    STO(...args) {
        this._log(CATEGORIES.STO, ...args);
    }

    /**
     * log variables read/written
     */
    VRW(...args) {
        this._log(CATEGORIES.VRW, ...args);
    }

    /**
     * logs from advice functions
     */
    ADV(...args) {
        this._log(CATEGORIES.ADV, ...args)
    }

    DRF(...args) {
        this._log(CATEGORIES.DRF, ...args)
    }
}

// Singleton
export const Logger = new LogHelper();
export default Logger;
