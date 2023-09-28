import AranRemote from "aran-remote"
import Chalk from "chalk";

import AranRemoteCallbackFactory from "@src/instrumentation/aran_remote_callback_factory";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import * as GTS from "@src/tester/global_tester_state";
import {Logger as Log} from "../util/logging";

const _options = {
    _: [],
    'node-port': 8000,
    'browser-port': 8080,
    alias: 'meta',
    argmpfx: 'META-',
    splitter: '_META_',
    log: ConfigReader.config.LOG_ARAN,
    synchronous: true,
    "use-debug": ConfigReader.config.DEBUG
};

/**
 * creates an AranRemote instance that will fork a child process to process remote aran communications.
 * @param _testRunner
 * @param globalInputs
 * @param events
 * @return AranRemote instance
 */
export default function createAranRemote(_testRunner, globalInputs, events) {
    /**
     * AranRemote will fork a process and call this callback function once it was set up. It must return a new
     * CB that AranRemote will execute to configure instrumentation (transform function and advice)
     * @param error
     * @param aran
     */
    const initialCB = (error, aran) => {
        console.log("in initialCB");
        if (error) {
            Log.ALL(Chalk.red("AranRemote error: ", error.message, error.stack))
        }
        if (aran.child) {
            process.on("SIGINT", () => {
                aran.child.kill("SIGINT");
            });
            process.on("SIGTERM", () => {
                aran.child.kill("SIGTERM");
            });
            _testRunner.addProcessToKill(aran.child);
        }
        _testRunner.serverFinishedSetup();

        aran.then(() => {
            aran.orchestrator.terminate()
        }, (error) => {
            Log.ALL("in aran.then() callback", error);
            throw error;
        });
        aran.orchestrator.then(() => {
            Log.ALL("Success")
        }, (error) => {
            if (error.message !== "Destroyed by the user") {
                throw error;
            }
        });
        aran.onterminate = (alias) => {
            GTS.globalTesterState!.removeProcess();
            if (GTS.globalTesterState!.hasRemainingProcesses()) {
                aran.terminate(aran.alias);
            }
        };

        _testRunner.setAran(aran);
        const adviceFactory = _testRunner.getAdviceFactory();
        const factory = new AranRemoteCallbackFactory(_testRunner);
        return factory.makeCallback(aran, adviceFactory);
    };

    return AranRemote(_options, initialCB);
}