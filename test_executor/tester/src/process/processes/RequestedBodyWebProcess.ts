import WebProcess from "@src/process/processes/WebProcess";
import ConfigurationReader from "@src/config/user_argument_parsing/Config";

export default class RequestedBodyWebProcess extends WebProcess {
    constructor(originalAlias, javaScriptFiles: string[], baseUrl: string) {
        super(originalAlias, 0, 0, undefined, baseUrl, javaScriptFiles);
        this.isRequested = true;
    }
}