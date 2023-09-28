import Process from "./Process"
import ConfigurationReader from "../../config/user_argument_parsing/Config"
import ApplicationTypeEnum from "../ApplicationTypeEnum";
import HTMLUITarget from "@src/process/targets/HTMLUITarget";

export default class WebProcess extends Process {
    private _originalJavaScriptFiles: string[];
    private _javaScriptfiles: string[];
    private _file: any;
    public url!: string;

    /**
     *
     * @param originalAlias original alias for the process.
     * @param processId
     * @param kindOfProcess
     * @param _serverFile The (NodeJS) file that acts as the server code.
     * When exclusively testing the client, this file is still executed (although not instrumented)
     * so that the browser can access the client-side code
     * @param baseUrl
     * @param javaScriptFiles
     * @inheritDoc
     */
    constructor(originalAlias, processId, kindOfProcess, public readonly _serverFile: string | undefined, public readonly baseUrl: string, javaScriptFiles: string[]) {
        super(originalAlias, processId, ApplicationTypeEnum.WEB, kindOfProcess, "JS_file");
        this._originalJavaScriptFiles = javaScriptFiles.slice();
        this._javaScriptfiles = javaScriptFiles.map(this.makeFullJSFilesNames);
        this._file = this.url;
    }

    get file() {
        return this._file
    }

    revertToInterTesting(): void {
        this._javaScriptfiles = this._originalJavaScriptFiles.map(this.makeFullJSFilesNames);
    }
    /**
     * Constructs an url from filename using port, input_suffix and test_individual_process configuration values
     * Example usage: main.js -> http://localhost:3000/main_injected.js (iff Config.TEST_INDIVIDUAL_PROCESSES = false, and Config.INPUT_SUFFIX = "injected")
     * @param name
     */
    private makeFullJSFilesNames = (name) => {
        const split = name.split(".");
        const extension = (split.length > 0) ? ("." + split.pop()) : "";
        // If split.length > 0, the last item of the array (= the extension) has been removed, via the destructive .pop operation.
        const nameBeforeExtension = (split.length > 0) ? (split.join(".")) : name;
        const port = (ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES) ? 4000 : 3000;
        const nameBeforeExtensionWithLocalhost = "http://localhost:" + port + "/" + nameBeforeExtension;
        return this.appendFilePath(nameBeforeExtensionWithLocalhost, ConfigurationReader.config.INPUT_SUFFIX, extension)
    }

    getJSFiles(): string[] {
        return this._javaScriptfiles;
    }

    addHTMLTarget(target: HTMLUITarget): number | false {
        if (!this._targetsDiscovered.isHtmlElementRegistered(target.htmlElement)) {
            return this.addTarget(target);
        } else {
            return false;
        }
    }
}