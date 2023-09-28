import { AdviceFactory } from "./advice_factory";
import * as GTS from "../tester/global_tester_state";
import { VerifyTestRunner } from "../tester/test-runners/VerifyTestRunner";
import VerifyInterceptor from "./interceptor/VerifyInterceptor";

export default class VerifyAdviceFactory extends AdviceFactory {
    constructor(private _testRunner: VerifyTestRunner) {
        super();
    }

    createIntercepter(_global, _process, _doRegularApply: Function) {
        return new VerifyInterceptor(_global, _process, GTS.globalTesterState, _doRegularApply, this._testRunner);
    }
}
