import Advice from "./advice";
import FunctionSummariesAdvice from "./function_summaries_advice";
import Interceptor from "./interceptor/Interceptor";
import * as GTS from "@src/tester/global_tester_state";
import Process from "@src/process/processes/Process";
import {TestRunner} from "@src/tester/test-runners/TestRunner";
import {testTracerGetInstance} from "@src/tester/TestTracer/TestTracerInstance";

export class AdviceFactory {
    createAdvice(aran: Promise<any>,
                 pointcut: Function,
                 aranRemoteCallbackFactoryFactoryInstance: any,
                 testRunner: TestRunner,
                 argm,
                 _process: Process,
                 _global) {
        return new Advice(aran,
                          pointcut,
                          aranRemoteCallbackFactoryFactoryInstance,
                          this,
                          argm,
                          _process,
                          _global,
                          testTracerGetInstance());
    }
    createIntercepter(_global, _process: Process, doRegularApply: Function): Interceptor {
        return new Interceptor(_global, _process, GTS.globalTesterState, doRegularApply);
    }
}





