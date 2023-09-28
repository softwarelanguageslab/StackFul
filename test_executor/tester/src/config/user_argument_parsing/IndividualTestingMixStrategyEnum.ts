const enum modes {
    FIXED   = 1,
    MIX     = 2,
    FIRST_X = 3,
}
export default modes

export function stringToMode(string) {
    const lowerCaseString = string.toLowerCase();
    switch (lowerCaseString) {
        case "fixed":
        case "f":
            return modes.FIXED;
        case "mixed":
        case "mix":
        case "m":
            return modes.MIX;
        case "first_x":
        case "fx":
            return modes.FIRST_X;
        default:
            throw new Error("Unrecognized mode: " + string);
    }
}
