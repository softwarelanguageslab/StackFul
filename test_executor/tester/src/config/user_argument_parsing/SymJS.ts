export enum SymJSAnalysisEnum {
    "named"              = "named",
    "valued"             = "valued",
    "conditional_valued" = "conditional_valued",
    "taint_named"        = "conditional_valued",

    "event_handler_first" = "event_handler_first",
    "write_read_length"   = "write_read_length",
    "data_races_first"    = "data_races_first"
}

export function stringToAnalysis(name: string): SymJSAnalysisEnum {
    if (name in SymJSAnalysisEnum) {
        return SymJSAnalysisEnum[name];
    } else {
        throw new Error("Unrecognized SymJS analysis option: " + name + ". Valid options are: " +
            Object.keys(SymJSAnalysisEnum).join(", "))
    }
}