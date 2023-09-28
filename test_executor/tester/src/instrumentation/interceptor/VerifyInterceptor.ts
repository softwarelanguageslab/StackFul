import Interceptor from "./Interceptor";
import Process from "../../process/processes/Process";
import { VerifyTestRunner } from "../../tester/test-runners/VerifyTestRunner";
import VerifySocketIOInterceptor from "./VerifySocketIOInterceptor";

export default class VerifyInterceptor extends Interceptor {
    public socketIOInterceptor: VerifySocketIOInterceptor;
    constructor(_global: any, _process: Process, globalTesterState, doRegularApply: Function, _testRunner: VerifyTestRunner) {
        super(_global, _process, globalTesterState, doRegularApply);
        this.socketIOInterceptor = new VerifySocketIOInterceptor(this._process, this._globalTesterState, _testRunner);
    }
}