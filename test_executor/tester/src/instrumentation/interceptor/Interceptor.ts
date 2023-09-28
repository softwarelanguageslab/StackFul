import {jsPrint} from "../../util/io_operations";
import tainter from "../tainter";
import Chalk from "chalk";
import {generateReplacement, IFunctionReplacement} from "./IFunctionReplacement";
import RandomInterceptor from "./RandomInterceptor";
import SocketIOInterceptor from "./SocketIOInterceptor";
import {WebsocketInterceptor} from "./WebsocketInterceptor";
import Process from "../../process/processes/Process";
import JQueryInterceptor from "./JQueryInterceptor";
import {doRegularApply} from "../helper";

/**
 * Initializes _table of intercepted functions
 * Is used by Advice to keep register and retrieve intercepted functions
 */
export default class Interceptor {
    protected _table: IFunctionReplacement[] = [];
    private _socketIOEmitHandler: any;
    private _socketIOOnHandler: any;
    protected socketIOInterceptor: SocketIOInterceptor;

    constructor(protected _global: any, protected _process: Process, protected _globalTesterState, protected _doRegularApply: Function) { //, protected _getSymJSHandler) {
        this.socketIOInterceptor = new SocketIOInterceptor(this._process, this._globalTesterState);
        this._initialiseFunctions();
    }

    /**
     * Check if there is an intercepted function for this function reference
     * @param $$function
     */
    shouldInterceptFunction($$function): boolean {
        const shouldIntercept = this._getInterceptedFunction($$function) !== undefined;
        return shouldIntercept;
    }

    /**
     * Retrieve replacement function
     * @param $$function original function reference
     */
    getReplacement($$function) {
        return this._getInterceptedFunction($$function)!.replacement;
    }

    /**
     * Add Function Replacement to Interceptor registry only when it does not exist yet.
     * @param repl replacement to register
     * @private
     */
    private _addReplacement(repl: IFunctionReplacement): void {
        // Check if the function hasn't already been added.
        if (!this.shouldInterceptFunction(repl.function)) {
            this._table.push(repl);
        }
    }

    /**
     * Adds initial function replacement definitions in our table.
     * @private
     */
    private _initialiseFunctions() {
        // Random number
        this._table.push(RandomInterceptor.generate(this._process))

        // Socket.io
        const replOn = this.socketIOInterceptor.generateOn();
        const replEmit = this.socketIOInterceptor.generateEmit()
        this._socketIOEmitHandler = replEmit?.replacement
        this._socketIOOnHandler = replOn?.replacement
        if (this._process.isANodeProcess()) {
            if (replOn) this._addReplacement(replOn);
            if (replEmit) this._addReplacement(replEmit);
        }

        // Websockets
        if (this._process.global.WebSocket) {
            this._addReplacement(WebsocketInterceptor.generateSend(this._process, this._globalTesterState));
        }

        // window: eventTarget and HTMLElement addEventListener calls
        if (this._process.global.window && this._process.global.window.HTMLElement) {
            const replacement = ($$function, $$value2, $$values, serial) => {
                const htmlElement = tainter.cleanAndRelease($$value2);
                const typeOfEvent = tainter.cleanAndRelease($$values[0]);
                this._process._maybeRegisterEvent(typeOfEvent, htmlElement, this._globalTesterState);
                return doRegularApply($$function, $$value2, $$values, serial);
            }

            this._addReplacement(generateReplacement(this._process.global.window.EventTarget.prototype.addEventListener, replacement));
            this._addReplacement(generateReplacement(this._process.global.window.HTMLElement.prototype.addEventListener, replacement));
        }
    }

    /**
     * Called by Advice after it received page loaded signal. This will add additional function replacements
     * that are only available once libraries have loaded.
     */
    pageLoaded() {
        if (this._process.global.window?.jQuery) {
            JQueryInterceptor.generateEvent(this._process, this._globalTesterState).forEach(
                repl => this._addReplacement(repl))
            this._addReplacement(JQueryInterceptor.generateVal(this._process));
        }

        if (this._process.global.window?.io?.Socket.prototype.emit) {
            this._addReplacement(generateReplacement(this._process.global.window.io.Socket.prototype.emit, this._socketIOEmitHandler));
        } else {
            jsPrint(Chalk.red("socket.emit will not be intercepted"));
        }
        if (this._process.global.window?.io?.Socket.prototype.on) {
            this._addReplacement(generateReplacement(this._process.global.window.io.Socket.prototype.on, this._socketIOOnHandler));
        } else {
            jsPrint(Chalk.red("socket.on will not be intercepted"));
        }
    }

    addSocketEmitReplacement(socketEmit) {
        this._addReplacement(generateReplacement(socketEmit, this._socketIOEmitHandler));
    }

    private _getInterceptedFunction($$function): IFunctionReplacement | undefined {
        const fun = tainter.cleanAndRelease($$function);
        return this._table.find(function (repl, _index) {
            return repl.function === fun;
        });
    }

}

