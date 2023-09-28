import ConfigReader from "../config/user_argument_parsing/Config";
import * as ShouldTestProcess from "../tester/should_test_process";
import AranRemote from "aran-remote";
import AranRemoteCallbackFactory from "./aran_remote_callback_factory";
import groupByArray from "../util/group_by_array";
import Chalk from "chalk";
import * as GTS from "../tester/global_tester_state";
import {TestRunner} from "../tester/test-runners/TestRunner";
import NodeProcess from "../process/processes/NodeProcess";
import {Logger as Log} from "../util/logging";
import {jsPrint} from "@src/util/io_operations";
import RequestedAranRemoteFactory from "@src/instrumentation/RequestedAranRemoteFactory";

export default class AranRemoteFactory {
    private _options: any;

    constructor(private _testRunner: TestRunner) {
        this._setOptions();
    }

    _setOptions(): void {
        this._options = {
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
    }

    /**
     * creates an AranRemote instance that will fork a child process to process remote aran communications.
     * @param serverProcess
     * @param globalInputs
     * @param events
     * @return AranRemote instance
     */
    createAranRemote(serverProcess: NodeProcess, globalInputs, events) {
        if (ConfigReader.config.LISTEN_FOR_PROGRAM) {
            return RequestedAranRemoteFactory(this._testRunner, [], []);
        }

        jsPrint("serverProcess._file: ", serverProcess._file);


        /**
         * AranRemote will fork a process and call this callback function once it was set up. It must return a new
         * CB that AranRemote will execute to configure instrumentation (transform function and advice)
         * @param error
         * @param aran
         */
        const initialCB = (error, aran) => {
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
                this._testRunner.addProcessToKill(aran.child);
            }
            Log.ALL(Chalk.green("Spawning new server Node process"));
            if (ShouldTestProcess.shouldTestServer()) {
                const groupedInputs = groupByArray(globalInputs, "processId");
                const groupedEvents = groupByArray(events, "processId");
                this._testRunner.setProcessInputsAndEvents(serverProcess, groupedInputs, groupedEvents);
                this._testRunner.startNodeProcess(serverProcess);
            } else {
                this._testRunner.serverFinishedSetup();
            }

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

            this._testRunner.setAran(aran);
            const adviceFactory = this._testRunner.getAdviceFactory();
            const factory = new AranRemoteCallbackFactory(this._testRunner);
            return factory.makeCallback(aran, adviceFactory);
        };

        return AranRemote(this._options, initialCB);
    }
}
