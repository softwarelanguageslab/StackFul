import { AdviceFactory } from "./advice_factory";
import FunctionSummariesAdvice from "./function_summaries_advice";
import {testTracerGetInstance} from "@src/tester/TestTracer/TestTracerInstance";

export default class FunctionSummariesAdviceFactory extends AdviceFactory {
    constructor(protected _functionSummaryHandler) {
        super();
    }
    createAdvice(aran: Promise<any>, pointcut, aranRemoteCallbackFactoryInstance, argm, _process, _global) {
        return new FunctionSummariesAdvice(aran,
                                           pointcut,
                                           aranRemoteCallbackFactoryInstance,
                                           this,
                                           argm,
                                           _process,
                                           _global,
                                           testTracerGetInstance(),
                                           this._functionSummaryHandler);
    }
}
