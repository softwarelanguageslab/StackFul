import ConfigReader from "../config/user_argument_parsing/Config";
import iterationNumber from "./iteration_number";

const NR_OF_COMPONENTS = 2;
const CLIENT_COMPONENT_ID = 1;
const SERVER_COMPONENT_ID = 0;

/**
 *  An instrumented client-process should be launched if:
 * - The tester was asked to test a full-stack application (instead of performing individual testing)
 * - The tester was asked to perform individual testing of the client-side of the application
 * @param specificIteration
 */
export function shouldTestClient(iteration = iterationNumber.get()): boolean {
    // return !Config.TEST_INDIVIDUAL_PROCESSES || ((iterationNumber.get() % NR_OF_COMPONENTS) === CLIENT_COMPONENT_ID);
    return (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) || (iteration > ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS);
}

export function shouldExclusivelyTestClient(iteration = iterationNumber.get()): boolean {
    // return Config.TEST_INDIVIDUAL_PROCESSES && ((iterationNumber.get() % NR_OF_COMPONENTS) === CLIENT_COMPONENT_ID);
    return ConfigReader.config.TEST_INDIVIDUAL_PROCESSES && (iteration > ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS);
}

/**
 * An instrumented server-process should be launched if:
 * - The tester was asked to test a full-stack application (instead of performing individual testing)
 * - The tester was asked to perform individual testing of the server-side of the application
 */
export function shouldTestServer(iteration = iterationNumber.get()): boolean {
    return (!ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) || (iteration <= ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS);
}

export function shouldExclusivelyTestServer(iteration = iterationNumber.get()): boolean {
    // return Config.TEST_INDIVIDUAL_PROCESSES && ((iterationNumber.get() % NR_OF_COMPONENTS) === CLIENT_COMPONENT_ID);
    return ConfigReader.config.TEST_INDIVIDUAL_PROCESSES && (iteration <= ConfigReader.config.MAX_NR_OF_INTRA_ITERATIONS);
}


