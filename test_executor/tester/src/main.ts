import AranRemote from "aran-remote"

import ExitCodes from "./util/exit_codes";

// Initialize singleton objects that depend on the config
// Parse command-line arguments to construct tester-config
// Make sure command-line arguments are always parsed before anything else (including e.g., loading other files) is done.
import ConfigurationReader from "./config/user_argument_parsing/Config";
const config = ConfigurationReader.handleArguments(process.argv.slice(2));

import Logger, { CATEGORIES } from "./util/logging";
Logger.setLogCategories(config.LOGGING_CATEGORIES);

import {testTracerInit} from "./tester/TestTracer/TestTracerInstance";
testTracerInit();

import {chooseApplication, InputProgram} from "./config/inputPrograms/InputProgram";
import {addPrograms} from "./config/inputPrograms/addProgram";
import ConcolicTester from "./tester/concolic_tester";
import codeBodyToApplication from "@src/config/inputPrograms/FromCodeBody";
import ConfigReader from "@src/config/user_argument_parsing/Config";
import RequestedAranRemoteFactory from "./instrumentation/RequestedAranRemoteFactory";
import TestRunnerFactory from "@src/tester/TestRunnerFactory";
import ReadSocket from "@src/backend/socket_comm/ReadSocket";

import * as TestRequest from "@src/instrumentation/TestRequestConfiguration";

let application: InputProgram | undefined;
if (config.LISTEN_FOR_PROGRAM) {
	const socketFilePath = "/tmp/apax_9856.sock";
	const readSocket = new ReadSocket(socketFilePath);
	const receivedRequestString = readSocket.read(100000);
	console.log(receivedRequestString);

	const receivedRequest = JSON.parse(receivedRequestString) as TestRequest.ReceivedRequest;
	TestRequest.setRequest(receivedRequest);

	// "OriginalURL":"http://localhost:3000/"
	application = codeBodyToApplication(receivedRequest.OriginalURL);


	const testRunnerFactory = new TestRunnerFactory(ConfigReader.config.MODE);
	const backendCommunicator1 = testRunnerFactory.makeBackend(ConfigReader.config.INPUT_PORT, ConfigReader.config.OUTPUT_PORT, 0);
	const backendCommunicator2 = (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) ? testRunnerFactory.makeBackend(ConfigReader.config.INPUT_PORT, ConfigReader.config.OUTPUT_PORT, 1) : backendCommunicator1;
	const testRunner = testRunnerFactory.makeTestRunner(application.processes, backendCommunicator1, backendCommunicator2);
	testRunner.startEverything();
	// const ar = RequestedAranRemoteFactory(testRunner, [], []);
} else {
	// Check whether the selected application actually exists before proceeding with the rest of the initialisation
	addPrograms()
	application = chooseApplication(config);
	if (!application) {
		console.log(`Unknown application: ${config.APPLICATION}`);
		process.exit(ExitCodes.applicationNotFound);
	}

	// Start the tester
	const concolicTester = new ConcolicTester(application!, config.MODE);
	concolicTester.start();
}