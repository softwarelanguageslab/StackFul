import Process from "./Process"
import ApplicationTypeEnum from "../ApplicationTypeEnum";
import ConfigurationReader from "../../config/user_argument_parsing/Config";

export default class NodeProcess extends Process {
    _file: string;

    /**
     * Initializes node process. Constructs file to execute by using originalFile,
     * @param _originalFile JS _file to execute without extension.
     * @param displayName optional short name to refer to the JS file, default is "index.js"
     * @inheritDoc
     */
    constructor(originalAlias: string, private _originalFile: string, processId: number, kindOfProcess, displayName?) {
        super(originalAlias, processId, ApplicationTypeEnum.NODE, kindOfProcess, displayName || "index.js");
        this._file = this.appendFilePath(_originalFile, ConfigurationReader.config.INPUT_SUFFIX, ".js");
    }

    get file() {
        return this._file;
    }

    /**
     * Changes JS file to test by removing the suffix and using extension .js
     */
    revertToInterTesting(): void {
        this._file = this.appendFilePath(this._originalFile, "", ".js");
    }
}