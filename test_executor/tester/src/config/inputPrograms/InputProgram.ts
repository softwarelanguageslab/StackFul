import IConfiguration from "../user_argument_parsing/IConfiguration";
import Process from "@src/process/processes/Process";
import {BenchmarksEnum} from "./addProgram";

export class InputProgram {
    public processes: Process[] = []
    public name: BenchmarksEnum
    
    constructor(name: BenchmarksEnum, processes: Process[]) {
        this.name = name
        this.processes = processes
    }
}

export const inputPrograms: InputProgram[] = [];
export function addInputProgram(p: InputProgram) {
    inputPrograms.push(p)
}

/**
 * Converts string to uppercase
 * @param string
 */
export function stringToApplication(appName: string) {
    return BenchmarksEnum[appName.toUpperCase()];
}

export function chooseApplication(config: IConfiguration): InputProgram | undefined {
    const applicationName: string = (config.ENABLE_TESTING) ? applicationToFixture(config.APPLICATION) : config.APPLICATION;
    return inputPrograms.find((p) => {
        return p.name === applicationName
    })
}

/**
 * Retrieve fixture from application
 * @param application
 */
export function applicationToFixture(application: BenchmarksEnum): BenchmarksEnum {
    switch (application) {
        case BenchmarksEnum.MAIN:
            return BenchmarksEnum.FIXT_MAIN;
        case BenchmarksEnum.FS_WHITEBOARD:
            return BenchmarksEnum.FIXT_FS_WHITEBOARD;
        case BenchmarksEnum.FS_VERIFY_INTRA:
            return BenchmarksEnum.FIXT_FS_VERIFY_INTRA;
        case BenchmarksEnum.NAMES_1:
            return BenchmarksEnum.FIXT_NAMES_1;
        case BenchmarksEnum.NAMES_2:
            return BenchmarksEnum.FIXT_NAMES_2;
        case BenchmarksEnum.FS_2BUTTONS:
            return BenchmarksEnum.FIXT_2BUTTONS;
        case BenchmarksEnum.MERGING1:
            return BenchmarksEnum.MERGING1;
        case BenchmarksEnum.MERGING2:
            return BenchmarksEnum.MERGING2;
        case BenchmarksEnum.MERGING3:
            return BenchmarksEnum.MERGING3;
        case BenchmarksEnum.MERGING4:
            return BenchmarksEnum.MERGING4;
        case BenchmarksEnum.MERGING5:
            return BenchmarksEnum.MERGING5;
        case BenchmarksEnum.MERGING6:
            return BenchmarksEnum.MERGING6;
        case BenchmarksEnum.MERGING7:
            return BenchmarksEnum.MERGING7;
        default:
            throw new Error("No equivalent fixture for given application");
    }
}
