const enum modes {
    BruteForce        = "explore",
    FunctionSummaries = "function_summaries",
    SymJS             = "solve",
    Verify            = "verify"
}

export default modes;

export function stringToMode(string) {
    const lowerCaseString = string.toLowerCase();
    switch (lowerCaseString) {
        case "explore":
        case "e":
            return modes.BruteForce;
        case "function_summaries":
        case "fs":
        case "f":
            return modes.FunctionSummaries;
        case "solve":
        case "s":
            return modes.SymJS;
        case "verify":
        case "v":
            return modes.Verify;
        default:
            throw new Error("Unrecognized mode: " + string);
    }
}
