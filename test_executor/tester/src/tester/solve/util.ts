import SymJSHandler from "../solve/symjs_handler";
import ConfigReader from "../../config/user_argument_parsing/Config";
import * as ShouldTestProcess from "../should_test_process";

export const symJSHandlerClient: SymJSHandler = new SymJSHandler();
export const symJSHandlerServer: SymJSHandler = new SymJSHandler();

export function getSymJSHandler(): SymJSHandler {
    if (ConfigReader.config.TEST_INDIVIDUAL_PROCESSES) {
        if (ShouldTestProcess.shouldTestServer()) {
            return symJSHandlerServer;
        } else {
            return symJSHandlerClient;
        }
    } else {
        return symJSHandlerClient;
    }
}