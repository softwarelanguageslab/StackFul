import {BenchmarksEnum} from "@src/config/inputPrograms/addProgram";
import {InputProgram} from "@src/config/inputPrograms/InputProgram";
import * as IO from "@src/util/io_operations";
import RequestedBodyWebProcess from "@src/process/processes/RequestedBodyWebProcess";

interface RequestToExplore {
    url: string
}

export default function codeBodyToApplication(requestUrl: string): InputProgram {
    const webProcess = new RequestedBodyWebProcess("test_alias", [], requestUrl)
    return new InputProgram(BenchmarksEnum.REQUESTED, [webProcess])
}