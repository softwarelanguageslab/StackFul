import * as Acorn from "acorn";
import * as Astring from "astring";

import { AdviceFactory } from "./advice_factory";
import ConfigurationReader from "../config/user_argument_parsing/Config";
import * as GTS from "../tester/global_tester_state";
import {Logger as Log} from "@src/util/logging";
import minimize from "./marked_functions";
import NodeProcess from "../process/processes/NodeProcess";
import Process from "../process/processes/Process";
import tainter from "./tainter";
import * as TestRequest from "@src/instrumentation/TestRequestConfiguration";
import { TestRunner } from "../tester/test-runners/TestRunner";
import WebProcess from "../process/processes/WebProcess";

export default class AranRemoteCallbackFactory {
    constructor(private _testRunner: TestRunner) {
    }

    public get testRunner(): TestRunner {
        return this._testRunner
    }

    protected isUninstrumentedWebsiteToBeTested(source: string | number): boolean {
        return ConfigurationReader.config.UNINSTRUMENTED && typeof (source) === "string" && source.includes("__otiluke");
    }

    protected shouldInstrument(script: string, source: string | number): boolean {
        // Does the source file belong to _any_ node process?
        const belongsToNodeProcess: boolean = GTS.globalTesterState!.getNodeProcesses().some((process: NodeProcess) => process.file === source);
        // Does the source file belong to _any_ web process? -> Check if there is a web-process containing this source-file in its list of JS-files.
        const belongsToWebProcess: boolean = GTS.globalTesterState!.getWebProcesses().some((process: WebProcess) => process.getJSFiles().some((file) => file === source));
        return belongsToNodeProcess || belongsToWebProcess ||
               (ConfigurationReader.config.LISTEN_FOR_PROGRAM && source === TestRequest.getRequest()!.URLContainingCode) ||
               this.isUninstrumentedWebsiteToBeTested(source);
    }

  protected makePageLoadedEventHandler(process: WebProcess): Function {
    return function () {
      process.finishedSetup();
      return true;
    }
  }


    makeCallback(aran, adviceFactory: AdviceFactory) {

        /**
         * This callback function is executed by AranRemote and constructs the transform function and Advice instance
         * This function retrieves the Process object by querying the Global Tester State for a process with the specified
         * alias in argm.alias (e.g. server_1). It then creates an advice and transform function that will be used by aran
         * to instrument the code. Only source files belonging to one of the process instances in the GTS process list
         * will be transformed with aran.
         * @param global
         * @param alias
         * @param argm
         */
        const aranRemoteCallback = ({global, alias, argm}: { global: any, alias: string, argm: any}) => {
            tainter.sandbox(global);
            const correspondingProcess: Process | undefined = GTS.globalTesterState!.findProcessByAlias(argm.alias);
            if (correspondingProcess == undefined) {
                throw new Error("Could not find process with alias: " + argm.alias)
            }
            const processId = correspondingProcess.processId;
            const isANodeProcess: boolean = (processId === 0 && !ConfigurationReader.config.LISTEN_FOR_PROGRAM);
            correspondingProcess.initialize(global, (isANodeProcess) ? undefined : global.window, (isANodeProcess) ? undefined : global.document);

            const pointcut = (name: string, _node) => name in advice;
            const transform = (script: string, source: string | number): string => {
                // console.log("in transform of source", source);
                // console.log("script:", script);
                if (this.shouldInstrument(script, source)) {
                    const serial = typeof source === "number" ? source : null;
                    const estree1 = ConfigurationReader.config.USE_MARKED_FUNC ? Acorn.parse(minimize(script, ConfigurationReader.config.USE_MARKED_FUNC), {locations:true}) : Acorn.parse(script, {locations: true});
                    const estree2 = aran.weave(estree1, pointcut, serial);
                    const transformed = Astring.generate(estree2);
                    Log.INS(transformed);
                    if (this.isUninstrumentedWebsiteToBeTested(source)) {
                      global.document.addEventListener("load", this.makePageLoadedEventHandler(correspondingProcess as WebProcess));
                    }
                    return transformed;
                } else {
                    return script;
                }
            };

            const advice = adviceFactory.createAdvice(aran, pointcut, this, this._testRunner, argm, correspondingProcess, global);
            return {transform, advice};
        }

        return aranRemoteCallback;
    }
}
