import childProcess from "child_process";

import {
  CredentialsInformation,
  CredentialsInformationType,
  POSTCredentialsInformation
} from "@src/tester/login/CredentialsInformation";
import Process from "@src/process/processes/Process";
import DOMElementNotFound from "@src/process/DOM/DOMElementNotFound";
import UITarget from "@src/process/targets/UITarget";
import {jsPrint, readFile} from "@src/util/io_operations";
import WebProcess from "@src/process/processes/WebProcess";
import sleep from "thread-sleep";
import SendOverSocket from "@src/backend/socket_comm/SendOverSocket";

export interface CrawlerInformation {
  cookieContent: string;
  redirect: string;
}

export default class CredentialTarget extends UITarget {
  constructor(processId: number, targetId: number, protected readonly credentials: CredentialsInformation) {
    super(processId, targetId);
  }

  protected parseCookie(cookieContent: string): string {
    let parsedCookie: string = "";
    const newlinesSplit = cookieContent.split("\n");
    // console.log("iterating over", newlinesSplit.slice(3));
    for (let cookieLine of newlinesSplit.slice(3)) {
      if (cookieLine === "") {
        continue;
      }
      const tabSplit = cookieLine.split("\t");
      const cookieName = tabSplit[tabSplit.length - 2];
      const cookieValue = tabSplit[tabSplit.length - 1];
      if (cookieValue === undefined || cookieName === undefined) {
        continue;
      }
      parsedCookie += `${cookieName}=${cookieValue}; `
    }
    return parsedCookie;
  }

  private extractUrlFromForm(loginNameElement: HTMLInputElement) {
    const formElement = this.findFormParent(loginNameElement);
    if (formElement === null) {
      console.log("Could not find form parent");
      throw new Error();
    }
    jsPrint("formElement =", formElement);
    const formAction = formElement.action;
    jsPrint("formAction =", formAction);
    return formAction;
  }

  protected findFormParent(element: HTMLElement): HTMLFormElement | null {
    return element.closest('form');
  }

  protected sendToCrawler(information: CrawlerInformation): void {
    const socketFilePath = "/tmp/apax_9568.sock";
    const sendSocket = new SendOverSocket(socketFilePath);
    sendSocket.send(JSON.stringify(information), false);
  }

  protected isJWT(stdout: string): boolean {
    try {
      const parsedJSON = JSON.parse(stdout);
      const authenticationObject = parsedJSON["authentication"];
      if (authenticationObject) {
        const tokenValue = authenticationObject["token"];
        return !!tokenValue; // cast as boolean
      } else {
        return false;
      }
    } catch (e) {
      return false;
    }
  }

  protected handleJWT(process: WebProcess, pathToCookie: string, stdout: string): void {
    const tokenValue = JSON.parse(stdout)["authentication"]["token"];
    const parsedCookie = this.parseCookie(readFile(pathToCookie));
    const completeCookie = parsedCookie + `token=${tokenValue}; `;
    const information = {cookieContent: completeCookie, redirect: process.baseUrl};
    this.sendToCrawler(information);
  }

  protected handleStdout(process: WebProcess, pathToCookie: string, stdout: string): void {
    if (this.isJWT(stdout)) {
      this.handleJWT(process, pathToCookie, stdout);
    } else {
      const parsedCookie = this.parseCookie(readFile(pathToCookie));
      console.log("parsedCookie:", parsedCookie);
      const information = {cookieContent: parsedCookie, redirect: process.baseUrl}; // redirect: stdout }
      this.sendToCrawler(information);
    }
  }

  fire(process: Process) {
    // const loginNameElementId = this.credentials.loginName.elementId;
    // const loginNameElement = process.getElementFromDocument(loginNameElementId) as HTMLInputElement;
    // if (loginNameElement === null) {
    //   throw new DOMElementNotFound(loginNameElementId);
    // }
    // const passwordElementId = this.credentials.passwordInformation.elementId;
    // const passwordElement = process.getElementFromDocument(passwordElementId) as HTMLInputElement;
    // if (passwordElement === null) {
    //   throw new DOMElementNotFound(passwordElementId);
    // }
    // const actionElementId = this.credentials.userAction.elementId;
    // const actionElement: any = process.getElementFromDocument(actionElementId);
    // if (actionElement === null) {
    //   throw new DOMElementNotFound(actionElementId);
    // }

    // const loginNameElementName = loginNameElement.name;
    // const passwordElementName = passwordElement.name;

    let url: string;
    if (this.credentials.type === CredentialsInformationType.form) {
      const loginNameElementId = this.credentials.loginName.elementId;
      const loginNameElement = process.getElementFromDocument(loginNameElementId) as HTMLInputElement;
      if (loginNameElement === null) {
        throw new DOMElementNotFound(loginNameElementId);
      }
      url = this.extractUrlFromForm(loginNameElement);
    } else if (this.credentials.type === CredentialsInformationType.POST) {
      console.log("document.body");
      console.log(process.document.body);
      url = (this.credentials as POSTCredentialsInformation).endpoint;
    } else {
      throw new Error(`Unrecognised credentials type ${this.credentials.type}`);
    }

    const cookieFile = "/Users/mvdcamme/PhD/Projects/JS_Concolic/tester/cookies.txt";

    const command = `curl "${url}" -s --cookie-jar ${cookieFile} -d '${this.credentials.loginName.elementId}=${this.credentials.loginName.elementValue}&${this.credentials.passwordInformation.elementId}=${this.credentials.passwordInformation.elementValue}'`;
    jsPrint(command);
    const that = this;
    childProcess.exec(command, function (error, stdout, stderr) {
      console.log("stdout of curl command was", stdout);
      that.handleStdout(process as WebProcess, cookieFile, stdout);
    });
    jsPrint("command executed");
    sleep(2000);
    // console.log("cookies", readFile(cookieFile));
  }
}