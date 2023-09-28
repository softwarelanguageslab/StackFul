import Chalk from "chalk";
import childProcess from "child_process";
import Path from "path";
import ThreadSleep from "thread-sleep";

import {AdviceFactory} from "@src/instrumentation/advice_factory";
import AranRemoteFactory from "@src/instrumentation/aran_remote_factory";
import BackendCommunication from "@src/backend/BackendCommunication";
import BranchConstraint from "@src/instrumentation/constraints/BranchConstraint";
import BrowsersEnum from "@src/config/BrowsersEnum";
import BrowserEnum from "@src/config/BrowsersEnum";
import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import * as CountLines from "@src/instrumentation/count_lines"
import Event from "../Event";
import ExitCodes from "@src/util/exit_codes";
import iterationNumber from "../iteration_number";
import {getSymJSHandler} from "@src/tester/solve/util";
import groupByArray from "@src/util/group_by_array";
import * as GTS from "../global_tester_state";
import {jsPrint} from "@src/util/io_operations";
import {Logger as Log} from "@src/util/logging";
import NodeProcess from "@src/process/processes/NodeProcess";
import Process from "@src/process/processes/Process";
import SharedOutput from "@src/util/output";
import {ShouldStop} from "../ShouldStop";
import * as ShouldTestProcess from "../should_test_process";
import {symbolicIDGenerator} from "@src/symbolic_expression/symbolic_id_generator";
import TestTracer from "../TestTracer/TestTracer";
import {testTracerGetInstance} from "@src/tester/TestTracer/TestTracerInstance";
import WebProcess from "@src/process/processes/WebProcess";
import {CredentialsInformation, optGetCredentialValue, readCredentials} from "@src/tester/login/CredentialsInformation";
import ComputedInput from "@src/backend/ComputedInput";

import * as BC from "../branch_coverage";
import CredentialTarget from "@src/process/targets/CredentialTarget";

const aranRemoteBinPath = Path.join(__dirname, "../../../node_modules/aran-remote/lib/node/bin.js")

function findInputsOfProcess(groupedInputs, processId) {
    return groupedInputs.find(function (inputObject) {
        return inputObject.key === processId;
    });
}

function findAllInputsOfProcess(groupedInputs, processId) {
    return groupedInputs.filter(function (inputObject) {
        return inputObject.key === processId;
    });
}

function sortInputsByInputId(processInputs) {
    function makeSortFunction(key) {
        return function (a, b) {
            if (a[key] > b[key]) {
                return 1;
            } else if (a[key] < b[key]) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    return processInputs.sort(makeSortFunction("id"))
}

export abstract class TestRunner {
    private aranRemoteFactory: any;
    private blackListedAliases: string[];
    private GLOBAL_INPUTS: ComputedInput[];
    private EVENTS: Event[];
    protected _aran: any;
    private processesToKill: any[];
    private _wasActive: boolean;
    protected _isStopping: boolean;
    private _testTracer: TestTracer = testTracerGetInstance();
    private _numberOfLinesInApplication: number;
    private _hasTerminated: boolean;
    private _branchCompletion: any[];
    private _credentials: CredentialsInformation | null;

    protected constructor(public _processes: Process[],
                          protected _backendCommunicator1: BackendCommunication,
                          protected _backendCommunicator2: BackendCommunication,
                          protected _shouldStop: ShouldStop) {
        this.aranRemoteFactory = new AranRemoteFactory(this);

        this.blackListedAliases = [];

        this.GLOBAL_INPUTS = [];
        this.EVENTS = [];

        this._aran = null;
        this.processesToKill = [];
        this._wasActive = true;
        this._isStopping = false;
        this._numberOfLinesInApplication = 0
        this._hasTerminated = false
        this._branchCompletion = []

        const pathToCredentials = ConfigurationReader.config.CREDENTIALS;
        this._credentials = (pathToCredentials !== "") ? readCredentials(pathToCredentials) : null;
    }

    abstract communicateWithBackend(backendCommunicator: BackendCommunication, stoppedBeforeEnd: boolean): any;

    setAran(aran: any): void {
        this._aran = aran;
    }

    wasActive(): boolean {
        return this._wasActive;
    }

    setWasActive(): void {
        this._wasActive = true;
    }

    resetWasActive(): void {
        this._wasActive = false;
    }

    shouldStop(predicateExpression, wasTrue: boolean) {
        const stopNow = this._shouldStop.shouldStop(predicateExpression, wasTrue);
        if (stopNow) {
            jsPrint(Chalk.green("Stopping now"));
            this._endGlobalTestIteration(true)
        }
    }

    isStopping(): boolean {
        return this._isStopping;
    }

    addProcessToKill(process): void {
        this.processesToKill.push(process);
    }

    killChildProcesses(signal?: string): void {
        if (signal == undefined) {
            signal = "SIGTERM";
        }
        this.processesToKill.forEach((process) => {
            process.kill(signal);
        });
    }

    protected eventHandlerFinished(): void {}

    _makeGlobalTesterState(events: Event[], globalInputs: ComputedInput[]): GTS.GlobalTesterState {
        if (this._credentials !== null) {
            const processId = (ConfigurationReader.config.LISTEN_FOR_PROGRAM) ? 0 : 1;
            const targetId = 0;
            const newEvents: Event[] = [new Event(processId, targetId)];
            const newState = new GTS.GlobalTesterState(this._processes, newEvents, globalInputs);
            const newTarget = new CredentialTarget(processId, targetId, this._credentials);
            this._processes[processId].addTarget(newTarget);
            // const newEvents: Event[] = (targetId) ? [{targetId: targetId, processId: processId}] : events;
            return newState;
        } else {
            return new GTS.GlobalTesterState(this._processes, events, globalInputs);
        }
    }

    _relaunchGlobalTesterState(globalInputs: ComputedInput[], events: Event[]): void {
        GTS.setGlobalState(this._makeGlobalTesterState(events, globalInputs));
    }

    getBackendCommunicator(): BackendCommunication {
        if (ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES) {
            if (ShouldTestProcess.shouldExclusivelyTestServer(undefined)) {
                return this._backendCommunicator1;
            } else if (ShouldTestProcess.shouldExclusivelyTestClient(undefined)) {
                return this._backendCommunicator2;
            } else {
                throw new Error("Can use neither client nor server backendcommunicator");
            }
        } else {
            return this._backendCommunicator1;
        }
    }

    public asyncCallCheckToFireNextEvent(): void {
        // Have to bind "this": https://stackoverflow.com/questions/2130241/pass-correct-this-context-to-settimeout-callback
        setTimeout(this._checkToFireNextEvent.bind(this), ConfigurationReader.config.DEFAULT_TIMEOUT_MS);
    }

    receiveSymbolicTestCondition(serial: number, alias: string, constraint: BranchConstraint): void {
        if (!(alias in this.blackListedAliases)) {
            GTS.globalTesterState!.addCondition(constraint, alias);
        }
    }

    /** The server (node) process will run using aran-remote bin launcher. That will result in the code to be instrumented
     * with aran. aran-remote bin communicates with our aran-remote forked process using port 8000
     * @param process the NodeProcess to be instrumented. we use this parameter to extract alias and file information
     */
    startNodeProcess(process: NodeProcess): void {
        // jsPrint("Start Node Process")
        const args = [aranRemoteBinPath, "--host=8000", "--alias=" + process.alias, "--meta-alias=meta", "--", process._file]
        const options = {stdio: [0, 1, 2]}
        // console.group()
        // jsPrint("args: ", args)
        // jsPrint("options: ", options)
        // console.groupEnd()
        const subprocess = childProcess.spawn("node", args, options)
        this.addProcessToKill(subprocess);
    }

    /**
     * The web process will run using either a browser instance or jsdom. We use the aran-remote proxy server instrument
     * the code and communicate with our aran-remote forked process (through port 8000)
     * @param webProcesses
     */
    _startWebProcesses(webProcesses: WebProcess[]) {
        function makeAranUrl(process: WebProcess): string {
            return process.baseUrl + "?META-splitter=8000&META-meta-alias=meta&META-alias=" + process.alias;
            // return "http://localhost:3000/" + "?META-splitter=8000&META-meta-alias=meta&META-alias=" + process.alias;
        }

        function launchFirefox(): string {
            // const bin = "/Applications/Firefox.app/Contents/MacOS/firefox-bin";
            const bin = "/usr/bin/firefox";
            const urls = webProcesses.map((process: WebProcess) => makeAranUrl(process)).join(" -new-tab -url ");
            return bin + " -private -devtools " + urls; // + " -safe-mode -devtools -headless -new-tab -url about:blank "
        }

        function launchPhantomJS(): string {
            const phantomJS = "/Users/mvdcamme/PhD/Projects/browsers/phantomjs/phantomjs-2.1.1-macosx/"
            const bin = Path.join(phantomJS, "bin", "phantomjs");
            const phantomJSFile = Path.join(phantomJS, "examples", "test1.js");
            return `${bin} ${phantomJSFile} "${makeAranUrl(webProcesses[0])}"`;
            // const phantomJSFile = Path.join("/Users/mvdcamme/PhD/Projects/phantomjs/as_node", "index.js");
            // const url = makeAranUrl(webProcesses[0]);
            // return `node ${phantomJSFile} "${url}"`
        }

        function launchPuppeteer(): string {
            const puppeteerHome = "/Users/mvdcamme/PhD/Projects/browsers/puppeteer";
            const nodeBin = "node";
            const puppeteerFile = Path.join(puppeteerHome, "launch_with_proxy.js");
            return `${nodeBin} ${puppeteerFile} "${makeAranUrl(webProcesses[0])}"`;
        }

        function launchChrome(): string {
            const bin = `"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"`;
            const urls = webProcesses.map((process: WebProcess) => `"${makeAranUrl(process)}"`).join(" ");
            return bin + " --incognito --proxy-server=127.0.0.1:8080 " + urls; //--auto-open-devtools-for-tabs
        }

        function launchSafari() {
            const bin = "open -a Safari ";
            const urls = webProcesses.map((process: WebProcess) => process.url).join(" ");
            return bin + urls;
        }

        function launchOpera() {
            const bin = "/Applications/Opera.app/Contents/MacOS/Opera";
            const urls = webProcesses.map((process: WebProcess) => process.url).join(" ");
            return bin + " -proxy-server=127.0.0.1:8080 " + urls;
        }

        function launchJSDom() {
            // const portToUse = (ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES) ? 4000 : 3000;
            const jsDomFile = Path.join(__dirname, "/../../../headless_browsers/jsdom.js");
            // const url = makeAranUrl(webProcesses[0]);
            // return `node ${jsDomFile} ${portToUse} "${url}"`;
            const url = new URL(webProcesses[0].baseUrl);
            const port = url.port;
            const hostname = url.hostname;
            const path = url.pathname;
            // const aranParameters = (path.includes("?META-splitter=8000&META-meta-alias") ? "" : )
            return `node ${jsDomFile} ${port} ${hostname} "${path + "?META-splitter=8000&META-meta-alias=meta&META-alias=" + webProcesses[0].alias}"`;
        }

        if (ShouldTestProcess.shouldExclusivelyTestClient(undefined)) {
            if (webProcesses[0]._serverFile !== undefined) {
                // Launch a node process to execute the non-instrumented server-side of the application.
                this.addProcessToKill(childProcess.spawn("node", [webProcesses[0]._serverFile]));
                Log.ALL("Launched a server process for individual testing of the client");
            }
        }
        if (!ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES || ShouldTestProcess.shouldTestClient()) {
            let command: any = undefined;
            switch (ConfigurationReader.config.BROWSER_TO_USE) {
                case BrowsersEnum.FIREFOX:
                    command = launchFirefox();
                    break;
                case BrowserEnum.PHANTOM_JS:
                    command = launchPhantomJS();
                    break;
                case BrowserEnum.PUPPETEER:
                    command = launchPuppeteer();
                    break;
                case BrowsersEnum.CHROME:
                    command = launchChrome();
                    break; // Hasn't been tested since the rework of aran-remote
                // case BrowsersEnum.SAFARI:   command = launchSafari(); break; Hasn't been tested since the rework of aran-remote
                // case BrowsersEnum.OPERA:    command = launchOpera(); break; Hasn't been tested since the rework of aran-remote
                case BrowsersEnum.JSDOM:
                    command = launchJSDom();
                    break;
                default:
                    throw new Error(`Unknown browser ${ConfigurationReader.config.BROWSER_TO_USE}`);
            }
            Log.ALL("Command =", command);
            const child = childProcess.exec(command,
                (err, stdout, stderr) => {
                    if (err) {
                        console.error(err.message, err.stack);
                    }
                });
            child.on("exit", function (code, signal) {
                // @ts-ignore
                Log.ALL(`${process.alias} process exited with code ${code} and signal ${signal}`);
            });
            child.on("message", (message) => {
                // @ts-ignore
                Log.ALL(`Child process ${process.alias} sent message ${message}`);
            });
            this.addProcessToKill(child);
        }
    }

    setProcessInputsAndEvents(theProcess: any, groupedInputs, groupedEvents) {
        const processInputsFound = findInputsOfProcess(groupedInputs, theProcess.processId);
        const processInputs = (processInputsFound === undefined) ? [] : sortInputsByInputId(processInputsFound.values);
        const targetsToFollow = findAllInputsOfProcess(groupedEvents, theProcess.processId);
        theProcess.setProcessInputs(processInputs);
        theProcess.targetsToFollow = targetsToFollow;
    }

    _initGlobalTestIteration(globalInputs: ComputedInput[], events: Event[]) {
        const groupedInputs = groupByArray(globalInputs, "processId");
        const groupedEvents = groupByArray(events, "processId");
        symbolicIDGenerator.resetIds();
        if (!ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES || ShouldTestProcess.shouldTestClient()) {
            const webProcesses = GTS.globalTesterState!.getWebProcesses();
            if (webProcesses.length > 0) {
                webProcesses.forEach((process) => {
                    this.setProcessInputsAndEvents(process, groupedInputs, groupedEvents);
                });
                this._startWebProcesses(webProcesses);
            }
        } else {
            /*
             * Testing the server process individually: the server process shouldn't be launched here, because it
             * should have already been launched when Aran-Remote was created.
             */
        }
    }

    _endGlobalTestIterationBookkeeping(): void {
        const symJSHandler = getSymJSHandler();
        symJSHandler._process = this._processes[1];

        this._isStopping = true;
        const linesCoveredThisIteration = GTS.globalTesterState!.getLinesCoveredThisIteration();
        SharedOutput.getOutput().getMetrics().addLineCovered(linesCoveredThisIteration);
        SharedOutput.getOutput().getMetrics().setDataRacesDiscovered(this._processes[this._processes.length - 1].getDataRaces().length)
        const metricsThisIteration = SharedOutput.getOutput().endIteration(GTS.globalTesterState!.executionTime(), GTS.globalTesterState!.getNodeProcesses()[0]);
        this._testTracer.testMetrics(metricsThisIteration);
        Log.ALL(`Metrics for this iteration: ${JSON.stringify(metricsThisIteration)}`);
        GTS.globalTesterState!.getProcesses().forEach((process) => this.blackListedAliases.push(process.alias));
    }

    _endGlobalTestIteration(stoppedBeforeEnd: boolean): void {
        if (this.isStopping()) {
            return;
        }
        const symJSHandler = getSymJSHandler()
        symJSHandler._process = this._processes[1];

        // store executed event- and branch sequences
        let branchDict = BC.storeBranchCoverage(symJSHandler);

        // store for every event sequence whether there are still branch sequences left to explore
        let currentState = symJSHandler._currentState;
        this._branchCompletion = BC.storeBranchCompletion(symJSHandler, currentState, this._branchCompletion, branchDict);

        // store variables that are used in multiple event handlers
        symJSHandler._sharedVariables = BC.analyzeSharedVariables(symJSHandler);
        Log.ALL(Chalk.green("Shared variables:"));
        Log.ALL(symJSHandler._sharedVariables);

        
        //Event Racer
        const p = this._processes[this._processes.length - 1];
        p.runEventRacer(symJSHandler);
        symJSHandler._nrOfPossibleEventSequences  = p.countEventSeqs();
        Log.DRF(Chalk.green(`nr of possible event sequences: ${symJSHandler._nrOfPossibleEventSequences}`));
        //

        this._endGlobalTestIterationBookkeeping();
        this.communicateWithBackend(this.getBackendCommunicator(), stoppedBeforeEnd);
    }

    stopExploringFunction(concreteFrame, lastFrame) {
    }

    errorDiscovered(process, errorMessage) {
    }

    _mayFireNextEvent(): boolean {
        const result = !this.wasActive() && GTS.globalTesterState!.getProcesses().every(function (process) {
            return process.hasFinishedSetup();
        })
        this.resetWasActive();
        return result;
    }

    _handlePossibleMayTerminate(): void {
        const wasActive = this.wasActive();
        jsPrint("_handlePossibleMayTerminate");
        if (this._mayFireNextEvent()) {
            jsPrint("_handlePossibleMayTerminate: _mayFireNextEvent = true");
            const maybeEmitMessageCallback = GTS.globalTesterState!.getUnfinishedMessageEmit();
            if (maybeEmitMessageCallback) {
                jsPrint("_handlePossibleMayTerminate: _mayFireNextEvent = true, maybeEmitMessageCallback = true");
                /*
                 * Before we can trigger the next event, we have to take care of some remaining message emit.
                 */
                jsPrint(Chalk.green(`Handling postponed message callback`));
                this.asyncCallCheckToFireNextEvent();
                maybeEmitMessageCallback();
            }

            this.eventHandlerFinished();

            if (GTS.globalTesterState!.hasNextEvent()) {
                jsPrint("_handlePossibleMayTerminate: _mayFireNextEvent = true, GTS.globalTesterState!.hasNextEvent() = true");
                /*
                 * Processes finished setting up and have also seemingly finished handling the previous event,
                 * so we can fire a new event, if one is available.
                 */
                this.asyncCallCheckToFireNextEvent();
                GTS.globalTesterState!.fireNextEvent();
            } else if (! ConfigurationReader.config.LISTEN_FOR_PROGRAM) {
                jsPrint("_handlePossibleMayTerminate: _mayFireNextEvent = true, GTS.globalTesterState!.hasNextEvent() = false");
                /* No new event available, stop this test iteration. */
                this._endGlobalTestIteration(false);
            } else {
                // Listening for input, should never start a new iteration
            }
        } else {
            jsPrint("_handlePossibleMayTerminate: _mayFireNextEvent = false");
            jsPrint(`Can't fire next event: nr of messages" ${GTS.globalTesterState!.unhandledMessages.size} ; WAS_ACTIVE: ${wasActive}`);
            /* Processes are still processing some event, so wait until they have finished. */
            this.asyncCallCheckToFireNextEvent();
        }
    }

    _checkToFireNextEvent(): void {
        if (GTS.globalTesterState!.allProcessesFinishedSetup()) {
            this._handlePossibleMayTerminate();
        } else if (ConfigurationReader.config.LISTEN_FOR_PROGRAM) {
            Log.ALL(Chalk.blue(`Tester running in listen-mode, not waiting for all programs to set up`));
            this._handlePossibleMayTerminate();
        } else {
            /* Processes haven't finished setting up yet. */
            Log.ALL(Chalk.red(`Not all processes finished setting up yet, waiting a bit longer`));
            this.asyncCallCheckToFireNextEvent();
        }
    }

    _doOneTestIteration(): void {
        this._initGlobalTestIteration(this.GLOBAL_INPUTS, this.EVENTS);
        this.asyncCallCheckToFireNextEvent();
    }

    serverFinishedSetup(): void {
        Log.ALL(Chalk.green("Server finished setting up"));
        this._doOneTestIteration();
    }

    _stopTesting(exitCode: ExitCodes): void {
        testTracerGetInstance().testStopped(exitCode);
        process.exit(exitCode);
    }

    _successFullyFinishedTesting(): void {
        Log.ALL(Chalk.bgGreen("FINISHED TESTING PROGRAM"));
        this.getBackendCommunicator().sendTerminationMessage();
        this._stopTesting(ExitCodes.success);
    }

    _setNrOfLinesGlobal(): void {
        const lineCache = CountLines.cache
        if (lineCache && lineCache.size != 0) {
          // lineCache.forEach(this._logMapElements);
          let maxValue;
          for (var [key, value] of lineCache) {
             maxValue = (!maxValue || maxValue < value) ? value : maxValue;
          }
          this._numberOfLinesInApplication = maxValue;
        }
      }

    /**
     * Perform an iteration
     * @param globalInputs
     * @param events
     * @protected
     */
    protected main(globalInputs: ComputedInput[], events: Event[]): void {
        this._isStopping = false;
        iterationNumber.inc();
        const symJSHandler = getSymJSHandler();
        const p = this._processes[this._processes.length - 1];
        p._resetEventRacer()

        SharedOutput.getOutput().getMetrics().setPrecomputedInputs(globalInputs);
        SharedOutput.getOutput().getMetrics().setEventSequence(events);
        const metrics = SharedOutput.getOutput().getMetrics()._metrics;
        const nrOfLinesCovered = metrics.length > 0 ? metrics[metrics.length - 1]["linesCovered"] : 0
        if (!this._numberOfLinesInApplication) this._setNrOfLinesGlobal();

        // Stop condition: nr of lines
        Log.ALL(Chalk.green(`nrOfLines: ${this._numberOfLinesInApplication}`));
        Log.ALL(Chalk.green(`nrOfLinesCovered: ${nrOfLinesCovered}`));
        if (this._numberOfLinesInApplication && (this._numberOfLinesInApplication <= nrOfLinesCovered)) {
            Log.ALL(Chalk.green("ALL LINES COVERED"));
            symJSHandler._allLinesCovered = true;
            // this._stopTesting(ExitCodes.success);
        }
        symJSHandler._totalNrOfLines = this._numberOfLinesInApplication;
        symJSHandler._lineCoverageProgress.push(nrOfLinesCovered);

        const nrOfDataRaces = p.getDataRaces().length
        symJSHandler._dataRaceCoverageProgress.push(nrOfDataRaces);
        Log.DRF(Chalk.green(`dataRaceProgress: ${symJSHandler._dataRaceCoverageProgress}`));

        if (!this._hasTerminated && this._shouldStopTesting()) {
            Log.ALL(Chalk.green("TESTER DECIDED TO STOP TESTING"));
            // this._stopTesting(ExitCodes.success);
            this._hasTerminated = true;
          }

        if (iterationNumber.get() > ConfigurationReader.config.MAX_NR_OF_ITERATIONS) {
            Log.ALL(Chalk.green("MAX NUMBER OF ITERATIONS REACHED"));
            this._stopTesting(ExitCodes.success);
            return;
        }
        if (ConfigurationReader.config.TEST_INDIVIDUAL_PROCESSES) {
            events.forEach(function (event) {
                event.processId = ShouldTestProcess.shouldExclusivelyTestServer(undefined) ? 0 : 1;
            });
        }
        if (ConfigurationReader.config.LISTEN_FOR_PROGRAM) {
            events.forEach(function (event) {
                event.processId = 0;
            });
        }
        Log.ALL("\n\n\nSTARTING NEW TEST ITERATION\n");
        Log.ALL(`globalInputs = ${JSON.stringify(globalInputs)}`);
        Log.ALL(`events = [${events.map((e) => e.toString())}]`);

        this._testTracer.testNewIteration();
        this._testTracer.testGlobalInputs(globalInputs);
        this._testTracer.testEvents(events);

        this.GLOBAL_INPUTS = globalInputs;
        this.EVENTS = events;

        this._relaunchGlobalTesterState(globalInputs, events);

        if (this.processesToKill.length > 0) {
            Log.ALL(Chalk.green("Killing child processes"));
            ThreadSleep(2000);
        }

        // Run the server process
        if (this._processes[0].isRequested) {
            this.aranRemoteFactory.createAranRemote(this._processes[0], globalInputs, events);
            // this.serverFinishedSetup();
        } else {
            this.aranRemoteFactory.createAranRemote(this._processes[0], globalInputs, events);
        }
    }

    _shouldStopTesting(): boolean {
        const symJSHandler = getSymJSHandler();
        const lineProgress = symJSHandler._lineCoverageProgress;
        const raceProgress = symJSHandler._dataRaceCoverageProgress;
    
        const iterNr = iterationNumber.get()
        const nrOfEvents = symJSHandler._eventHandlers.length;
        var nrOfPosEventSeqs = symJSHandler._nrOfPossibleEventSequences;
        const linesCovered = lineProgress[lineProgress.length - 1];
        const totalLines = this._numberOfLinesInApplication;
        const sharedVars = symJSHandler._sharedVariables;
    
        const dormantRaceIters = symJSHandler.countDormantIterations(raceProgress);
        const dormantLineIters = symJSHandler.countDormantIterations(lineProgress);
    
        function countEvents(dict) { // from the shared variables, get the number of events that do not happen before each other
          var events = new Set();
          for (var [key, value] of Object.entries(dict)) {
            var eventIds = key.split(",").map(e => parseInt(e));
            for (const ev of eventIds)
              events.add(ev);
          }
          return events.size;
        }
        var nrOfEventsInSharedVars = countEvents(sharedVars);
        if (nrOfEventsInSharedVars == 0) nrOfEventsInSharedVars = 1;
        if (nrOfEventsInSharedVars == nrOfEvents) nrOfEventsInSharedVars -= 1;
        if (nrOfPosEventSeqs > 500) nrOfPosEventSeqs = 500;
    
        Log.DRF(Chalk.green(`nrOfEventsInSharedVars: ${nrOfEventsInSharedVars}`));
    
        if (linesCovered == totalLines && dormantRaceIters > dormantLineIters/2 && dormantLineIters > (totalLines / nrOfEventsInSharedVars)) {
          Log.DRF(Chalk.bgGreen(`ALL LINES COVERED AND TOO MANY DORMANT RACE ITERATIONS (${dormantRaceIters}), TERMINATING TESTER`));
          return true;
        }
    
        // (e/(e-sv)) * (sqrt(p)) * (tl/lc)^2
        const c = (nrOfEvents / (nrOfEvents - nrOfEventsInSharedVars)) * Math.sqrt(nrOfPosEventSeqs) * Math.pow(totalLines / linesCovered, 2);
    
        if (linesCovered < totalLines && Math.min(dormantLineIters, dormantRaceIters) > Math.max(c, 30)) {
          Log.DRF(Chalk.bgGreen("NOT ALL LINES COVERED BUT TOO MANY DORMANT ITERATIONS, TERMINATING TESTER"));
          return true;
        } else {
          Log.DRF(Chalk.green(`dormant iters: ${Math.min(dormantLineIters, dormantRaceIters)}, treshold before terminating: ${Math.max(c, 30)} (c: ${c})`));
        }
    
        return false;
      }

    optGetCredentialsInformation(idField: string): string | null {
        if (this._credentials !== null) {
            const result = optGetCredentialValue(this._credentials, idField);
            if (result !== null) {
                jsPrint("found a credential input, value will be", result);
            }
            return result;
        } else {
            return null;
        }
    }

    // Entry point to launch the TestRunner
    abstract startEverything();

    abstract getAdviceFactory(): AdviceFactory;
}