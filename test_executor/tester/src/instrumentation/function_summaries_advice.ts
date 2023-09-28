import Chalk from "chalk";
import tainter, { dirty } from "./tainter";
import Advice from "./advice";
import {Logger as Log} from "@src/util/logging";
import FunctionSummariesAdviceFactory from "./function_summaries_advice_factory";
import FunctionSummaryHandler from "@src/tester/function_summary/function_summary_handler";
import Process from "@src/process/processes/Process";
import { TestRunner } from "@src/tester/test-runners/TestRunner";
import Interceptor from "./interceptor/Interceptor";
import TestTracer from "@src/tester/TestTracer/TestTracer";

export default class FunctionSummariesAdvice extends Advice {
    private _functionSummaryHandler: FunctionSummaryHandler;

    constructor(aran: Promise<any>,
                pointcut: Function,
                aranRemoteCallbackFactoryInstance,
                adviceFactory: FunctionSummariesAdviceFactory,
                argm: any,
                _process: Process,
                _global,
                testTracer: TestTracer,
                functionSummaryHandler: FunctionSummaryHandler) {
        super(aran, pointcut, aranRemoteCallbackFactoryInstance, adviceFactory, argm, _process, _global, testTracer);
        this._functionSummaryHandler = functionSummaryHandler;
    }

    apply($$function: dirty, $$value2: dirty, $$values: dirty[], serial: number) {
        const returnValue = super.apply($$function, $$value2, $$values, serial);
        const cleanedFunction = tainter.cleanAndRelease($$function);
        this._functionSummaryHandler.callFunction(cleanedFunction, this._aran.nodes[serial]);
        return returnValue;
    }

    argument(_value, idx, serial) {
        this._nodeCovered(serial);
        var superArgument = super.argument(_value, idx, serial);
        if (this._isUserParameter(idx, serial)) {
            superArgument = this._functionSummaryHandler.userParameterEncountered(superArgument, idx, this._process, serial);
        }
        return superArgument;
    }

    arrival($callee, newTarget, thisValue, argumentsValue, serial) {
        super.arrival($callee, newTarget, thisValue, argumentsValue, serial);
        if (this._ES.refersToFunDeclaration(serial)) {
            Log.FUN(Chalk.blue("entering user function:"), `[${this._aran.nodes[serial].loc.start.line}-${this._aran.nodes[serial].loc.end.line}]`);
            this._functionSummaryHandler.arrivedInFunction(this._process, tainter.release($callee), argumentsValue, serial, this._aran.nodes);
        }
    }

    return($$consumed, serial) {
        var $$superConsumed = super.return($$consumed, serial);
        if (this._containedInsideValidFunction(serial)) {
            Log.FUN(Chalk.blue("leaving user function"), `return @ L${this._aran.nodes[serial].loc.start.line}`);
            $$superConsumed = this._functionSummaryHandler.returned(this._process, $$superConsumed);
        }
        return $$superConsumed;
    }

    test($$test, serial) {
        if (this._functionSummaryHandler.isBacktracking()) {
            return super.test($$test, serial);
        } else {
            this._nodeCovered(serial);
            return tainter.cleanAndRelease($$test);
        }
    }
}
