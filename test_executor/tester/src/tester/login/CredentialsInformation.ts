import FixedInput from "./FixedInput";
import {readJSON} from "@src/util/io_operations";
import FixedEvent from "@src/tester/login/FixedEvent";

export enum CredentialsInformationType {
    POST = "POST",
    form = "form",
}

export interface CredentialsInformation {
    type: CredentialsInformationType;
    loginName: FixedInput;
    passwordInformation: FixedInput;
}

export interface POSTCredentialsInformation extends CredentialsInformation {
    endpoint: string
}

export interface FormCredentialsInformation extends CredentialsInformation {
}

export function readCredentials(path: string): CredentialsInformation {
    const jsonContent = readJSON(path);
    return jsonContent;
}

export function optGetCredentialValue(information: CredentialsInformation, idField: string): string | null {
    if (information.loginName.elementId === idField) {
        return information.loginName.elementValue;
    } else if (information.passwordInformation.elementId === idField) {
        return information.passwordInformation.elementValue;
    } else {
        return null;
    }
}