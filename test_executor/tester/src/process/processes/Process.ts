import Chalk from "chalk";
import CountEventSeqs from "@src/util/countEventSeqs";
import tainter, { dirty } from "@src/instrumentation/tainter";
import Nothing from "@src/symbolic_expression/nothing";
import {IntEqual} from "@src/symbolic_expression/operators";
import SymbolicInt from "@src/symbolic_expression/symbolic_int";
import SymbolicRelationalExp from "@src/symbolic_expression/symbolic_relational_exp";
import * as GTS from "@src/tester/global_tester_state";
import {GlobalTesterState} from "@src/tester/global_tester_state";
import IterationNumber from "@src/tester/iteration_number";
import {getSymJSHandler} from "@src/tester/solve/util";
import {testTracerGetInstance} from "@src/tester/TestTracer/TestTracerInstance";
import ApplicationTypeEnum from "../ApplicationTypeEnum";
import { EventRacer } from "../EventRacer/eventRacer";
import KeyEvent from "../events/KeyEvent";
import MouseClickEvent from "../events/MouseClickEvent";
import KeyCodeEnum from "../KeyCodeEnum";
import PathConditionStore from "../PathConditionStore";
import ClickableTarget from "../targets/ClickableTarget";
import Target from "../targets/Target";
import TextInputTarget from "../targets/TextInputTarget";
import UITarget from "../targets/UITarget";
import WindowTarget from "../targets/WindowTarget";
import SymJSHandler from "@src/tester/solve/symjs_handler";
import {Logger as Log} from "@src/util/logging";
import TargetStorage from "@src/process/targets/TargetStorage";
import TargetNotFoundError from "@src/process/targets/TargetNotFoundError";
import ComputedInput from "@src/backend/ComputedInput.js";
import ExtendedMap from "@src/util/datastructures/ExtendedMap.js";
import {LocationExecutionState, TargetTriggeredExecutionState} from "@src/tester/ExecutionState.js";
import CodePosition from "@src/instrumentation/code_position.js";
import SymbolicInput from "@src/symbolic_expression/SymbolicInput.js";
import EventWithId from "@src/tester/EventWithId.js";
import ExecutionStateCollector from "@src/tester/ExecutionStateCollector.js";

const duplicateInputs = (inputs: ComputedInput[]) => inputs.slice();

export default abstract class Process {
    protected _errorConditions: PathConditionStore = new PathConditionStore();
    protected _alias!: string;
    protected _processInputs: ExtendedMap<String, ComputedInput[]> = new ExtendedMap();
    protected _UIMouseEvents!: WeakMap<object, MouseClickEvent>; // key is JS window.Event object (external)
    protected _UIKeyboardEvents!: WeakMap<object, KeyEvent>; // key is JS window.Event object (external)
    protected _targetsDiscovered!: TargetStorage;
    private _socketIOCallbacks!: any[];
    protected _hasFinishedSetup!: boolean;
    protected _document: Document | null = null;
    protected _window: Window | null = null;
    protected _originalProcessInputs: ExtendedMap<String, ComputedInput[]> = new ExtendedMap();
    protected _global: any;
    protected _eventRacer: EventRacer = new EventRacer();
    public _processFieldInputs: any[] = [];

    public isRequested: boolean = false;

    /**
     *
     * @param _originalAlias original alias for the process. This is used to construct the alias as a concatenation between alias and the iteration number
     * @param _processId unique id
     * @param _applicationType node or web JS application
     * @param _kindOfProcess
     * @param _displayName short name to refer to the JS file
     * @protected
     */
    protected constructor(private _originalAlias: string, private _processId: number, private _applicationType: ApplicationTypeEnum, private _kindOfProcess: number, private _displayName: string) {
        this.reset();
    }

    get processId(): number {
        return this._processId
    }

    get alias(): string {
        return this._alias
    }

    get global() {
        return this._global
    }

    get displayName(): string {
        return this._displayName
    }

    get applicationType(): ApplicationTypeEnum
    {
        return this._applicationType
    }

    get kindOfProcess(): number {
        return this._kindOfProcess
    }

    get document(): Document {
        return this._document!;
    }

    get window(): Window {
        return this._window!;
    }

    isANodeProcess(): boolean {
        return this._applicationType === ApplicationTypeEnum.NODE;
    }

    /**
     * Prepare members for a new iteration
     */
    reset(): void {
        this._alias = this._originalAlias + "_" + IterationNumber.get();
        this._processInputs = new ExtendedMap();
        this._UIMouseEvents = new WeakMap();
        this._UIKeyboardEvents = new WeakMap();
        this._targetsDiscovered = new TargetStorage();
        this._socketIOCallbacks = [];
        this._hasFinishedSetup = false;
        this._document = null;
        this._window = null;
    }

    // sortInputsByInputId(processInputs) {
    //     function makeSortFunction(key) {
    //       return function(a, b) {
    //         if (a[key] > b[key]) {
    //           return 1;
    //         } else if (a[key] < b[key]) {
    //           return -1;
    //         } else {
    //           return 0;
    //         }
    //       }
    //     }
    //     return processInputs.sort(makeSortFunction("id"))
    //   }

    setProcessInputs(inputs: ComputedInput[]): void {
        let currentEntry = 0;
        let tempArray: any[] = [];

        inputs.forEach(input => {
            // const es = new LocationExecutionState(new CodePosition(input.executionState.getCodePosition.getProcessId, input.executionState.getCodePosition.getSerial), input.executionState.getFunctionStack, input.executionState.getEventSequence, input.executionState.getCallSiteSerial);
            const key = input.executionState.toString();
            console.log(`setting key ${key} for value ${input.value}`)
            const existingInputs = this._processInputs.get(key);
            const existingInputsOrNew = (existingInputs === undefined) ? [] : existingInputs;
            existingInputsOrNew.push(input);
            this._processInputs.set(key, existingInputsOrNew);
        });
      
        // //all inputObjectFields are transmitted to the frontend in a different way, they have to be separated from other kinds of input using this function
        // function separateByID(obj) {
        //   if ('fieldName' in obj && typeof(obj.fieldName) === 'string') {
        //     tempArray[currentEntry++] = obj;
        //     return false;
        //   } else {
        //     return true;
        //   }
        // }
        // this._processInputs = this.sortInputsByInputId(inputs.filter(separateByID));
        this._processFieldInputs = tempArray;
        this._originalProcessInputs = this._processInputs.duplicate(duplicateInputs);
    }

    getInputs(): Map<String, ComputedInput[]> {
        return this._processInputs;
    }

    getFieldInputs() {
        return this._processFieldInputs;
    }

    getOriginalInputs(): ExtendedMap<String, ComputedInput[]> {
        return this._originalProcessInputs.duplicate(duplicateInputs);
    }

    getSocketIOCallback(callbackId: number): any {
        return this._socketIOCallbacks[callbackId];
    }

    // Stores error condition and error related information
    errorDiscovered(error, eventSequence, codePosition): void {
        const PC = GTS.globalTesterState!.getGlobalPathConstraint();
        const messageInputMap = GTS.globalTesterState!.getMessageInputMap();
        this._errorConditions.addCondition(PC, eventSequence, error, codePosition, messageInputMap);
    }

    getErrorStore(): PathConditionStore {
        return this._errorConditions;
    }

    getElementFromDocument(elementId: string): HTMLElement | null {
        return this._document!.getElementById(elementId);
    }

    abstract revertToInterTesting(): void

    /**
     * append filePath, suffix and extension
     * @param filePath
     * @param optSuffix
     * @param extensionOrUndefined
     */
    protected appendFilePath(filePath, optSuffix, extensionOrUndefined): string {
        const extension = extensionOrUndefined || "";
        const suffix = (optSuffix !== "") ? "_" + optSuffix : "";
        return filePath + suffix + extension;
    }

    addTarget(target: Target): number {
        const targetId = this._targetsDiscovered.pushTarget(target);
        console.log("Process.addTarget targetId:", targetId, "of process", this.processId);
        return targetId;
    }

    /**
     * Register keypress event
     * @param _UITarget Event target the key press event was fired on
     * @param event JS window.KeyboardEvent instance
     * @param wrappedKeyCode The key code of the fired event
     * @private
     */
    _addKeyPress(_UITarget: UITarget, event: any, wrappedKeyCode): void {
        console.log("Process._addKeyPress, wrappedKeyCode.symbolic =", (wrappedKeyCode.symbolic as SymbolicInput).executionState.toString());
        const uiEvent = new KeyEvent(_UITarget, event, wrappedKeyCode.base, wrappedKeyCode.symbolic);
        this._UIKeyboardEvents.set(event, uiEvent);
    }

    /**
     * Retrieve registered keyboard event related information
     * @param event JS window.KeyboardEvent instance (
     */
    getKeyboardEvent(event: any): KeyEvent | undefined {
        return this._UIKeyboardEvents.get(event);
    }

    /**
     * Register mouse click event
     * @param _UITarget
     * @param event JS Event object
     * @param wrappedX
     * @param wrappedY
     * @private
     */
    _addMouseClick(_UITarget: UITarget, event: any, wrappedX, wrappedY): void {
        const uiEvent: MouseClickEvent = new MouseClickEvent(_UITarget, event, wrappedX.base, wrappedY.base, wrappedX.symbolic, wrappedY.symbolic);
        this._UIMouseEvents.set(event, uiEvent);
    }

    /**
     * Retrieve registered mouse event related information
     * @param event
     */
    getMouseEvent(event): MouseClickEvent | undefined {
        return this._UIMouseEvents.get(event);
    }

    /**
     * Generates a mouse event
     * @param $$object
     * @param $$key
     * @param _serial
     */
    handleMouseEventGet($$object: dirty, $$key: dirty, _serial: number): dirty {
        const object = tainter.cleanAndRelease($$object);
        const key = tainter.cleanAndRelease($$key);
        const result = object[key];
        if (key === "clientX" || key === "clientY") {
            const actualEvent = (object.originalEvent) ? object.originalEvent : object; // If it's a jQuery-event, use the event's originalEvent
            const maybeMouseEvent = this.getMouseEvent(actualEvent); // Check if the accessed mouse event is an existing one
            if (maybeMouseEvent) {
                const recordedBase = (key === "clientX") ? maybeMouseEvent.baseX : maybeMouseEvent.baseY;
                const recordedSymbolic = (key === "clientX") ? maybeMouseEvent.symbolicX : maybeMouseEvent.symbolicY;
                if (result === recordedBase) {
                    return tainter.taintAndCapture(recordedBase, recordedSymbolic);
                } else {
                    Log.ALL(Chalk.bgRed("Tried to return a symbolic value for clientX/clientY, but base value didn't match anymore"));
                    Log.ALL("recordedBase", recordedBase);
                    Log.ALL("result", result);
                    return tainter.taintAndCapture(result, new Nothing());
                }
            } else {

                return tainter.taintAndCapture(result, new Nothing());
            }
        } else {
            // Unsupported mouse event property access
            return tainter.taintAndCapture(result, new Nothing());
        }
    }

    handleKeyPressEventGet($$object: dirty, $$key: dirty, _serial: number): dirty {
        function makeSymbolic(symbolic, keyCode) {
            return new SymbolicRelationalExp(symbolic, IntEqual, new SymbolicInt(keyCode));
        }

        function checkWhetherMatches(result, key, base, symbolic, expectedValue, symbolicValue) {
            if (result === expectedValue) {
                Log.ALL(Chalk.green(`Process ${that._alias} getting ${key} from keyboard event, with stored value ${base} and symbolic ${symbolic}`))
                return tainter.taintAndCapture(expectedValue, symbolicValue);
            } else {
                Log.ALL(Chalk.bgRed(`Tried to return a symbolic value for KeyPress-event with field-access ${key}, but base value didn't match anymore`));
                Log.ALL(`Expected value is ${expectedValue}, current value is ${result}`);
                return tainter.taintAndCapture(result, new Nothing());
            }
        }

        const that = this;

        const key = tainter.cleanAndRelease($$key);
        const object = tainter.cleanAndRelease($$object);
        const result = object[key];
        const actualEvent = (object.originalEvent) ? object.originalEvent : object; // If it's a jQuery-event, use the event's originalEvent
        const maybeKeyPressEvent = this.getKeyboardEvent(actualEvent);

        if (maybeKeyPressEvent) {
            const base = maybeKeyPressEvent.base;
            const symbolic = maybeKeyPressEvent.symbolic;

            if (key === "which" || key === "keyCode") {
                return checkWhetherMatches(result, key, base, symbolic, base, symbolic);
            } else if (key === "altKey") {
                return checkWhetherMatches(result, key, base, symbolic, base === KeyCodeEnum.ALT_KEY, makeSymbolic(symbolic, KeyCodeEnum.ALT_KEY));
            } else if (key === "ctrlKey") {
                return checkWhetherMatches(result, key, base, symbolic, base === KeyCodeEnum.CTRL_KEY, makeSymbolic(symbolic, KeyCodeEnum.CTRL_KEY));
            } else if (key === "metaKey") {
                return checkWhetherMatches(result, key, base, symbolic, base === KeyCodeEnum.META_KEY_1, makeSymbolic(symbolic, KeyCodeEnum.META_KEY_1));
            } else if (key === "shiftKey") {
                return checkWhetherMatches(result, key, base, symbolic, base === KeyCodeEnum.SHIFT_KEY, makeSymbolic(symbolic, KeyCodeEnum.SHIFT_KEY));
            } else {
                return tainter.taintAndCapture(result, new Nothing());
            }
        } else {
            return tainter.taintAndCapture(result, new Nothing());
        }
    }

    /**
     * Dispatch the target event to the corresponding fire routine
     * @param target
     * @param event
     * @private
     */
    private _fireEventOnTarget(target: Target, event: EventWithId): void {
        Log.ALL(`Process._fireEventOnTarget, target = ${target.toString()}`);
        ExecutionStateCollector.setExecutionState(this.processId, new TargetTriggeredExecutionState(event.id, event.processId));
        target.fire(this);
    }

    /**
     * Do the necessary to fire the selected event
     * @param event
     */
    fireEvent(event: EventWithId): void {
        const UITarget = this._targetsDiscovered.getTarget(event.targetId);
        Log.ALL("Process.fireEvent, concreteTargetId =", event.targetId);
        if (UITarget !== undefined) {
            this._eventRacer.eventDispatched(event.targetId);
            this._fireEventOnTarget(UITarget, event);
            this._eventRacer.eventEnded(event.targetId);
        } else {
            Log.ALL(Chalk.bgRed(`No event with eventId (${event.targetId}), skipping dispatch`));
            throw new TargetNotFoundError(event.targetId);
        }
    }

    initialize(_global, window, document) {
        this._global = _global;
        this._document = document;
        this._window = window;
    }

    finishedSetup(): void {
        if (!this._hasFinishedSetup) {
            this._hasFinishedSetup = true;
            GTS.globalTesterState!.processFinishedSetup(this._processId);
            Log.ALL(Chalk.green(`Process ${this._alias} has finished setting up`));
        }
    }

    hasFinishedSetup(): boolean {
        return this._hasFinishedSetup;
    }

    targetDiscovered(UITarget: UITarget): void {
        this._targetsDiscovered.pushTarget(UITarget);
        this._eventRacer.newEventDiscovered(UITarget.targetId);
        Log.ALL(`Process ${this._processId}, this._targetsDiscovered.size = ${this._targetsDiscovered.size}`);
    }

    getNrOfSocketIOTargets(): number {
        return this._socketIOCallbacks.length;
    }

    socketIOTargetDiscovered(socketIOTarget, $$callback): void {
        this._socketIOCallbacks.push($$callback);
        this.targetDiscovered(socketIOTarget);
    }

    getTarget(id: number): Target | undefined {
        return this._targetsDiscovered.getTarget(id);
    }

    getTotalNumberOfTargets(): number {
        return this._targetsDiscovered.size;
    }

    private _handleMouseEventRegistration(htmlElement, mouseEventType): ClickableTarget {
        const target = new ClickableTarget(this.processId, this.getTotalNumberOfTargets(), htmlElement, mouseEventType);
        this.targetDiscovered(target);
        Log.ALL(Chalk.blue(`Mouse event registration: ${mouseEventType}`));
        return target;
    }

    private _handleKeyEventRegistration(htmlElement, keyEventType): TextInputTarget {
        Log.ALL(Chalk.blue(`Key event registration: ${keyEventType}`));
        const target = new TextInputTarget(this.processId, this.getTotalNumberOfTargets(), htmlElement, keyEventType);
        this.targetDiscovered(target);
        return target;
    }

    private _handleWindowEventRegistration(htmlElement, windowEventType): WindowTarget {
        Log.ALL(Chalk.blue(`Window event registration: ${windowEventType}`));
        const target = new WindowTarget(this.processId, this.getTotalNumberOfTargets(), htmlElement, windowEventType);
        this.targetDiscovered(target);
        return target;
    }

    private _isMouseEvent(event): boolean {
        return event === "mousedown" || event === "mouseup" || event === "mousemove" ||
            event === "mouseout" || event === "mouseover" || event === "contextmenu" ||
            event === "auxclick" || event === "click" || event === "dblclick" ||
            event === "select" || event === "blur" || event === "change" ||
            event === "focus" || event === "reset";
    }

    /**
     * This function is called when an eventListener or jquery event handler is registered on an API object
     * If it finds a supported event, it will create the corresponding Target instance and notify the corresponding
     * process, SymJSHandler and TestTracer about it.
     * @param typeOfEvent Type of event, see https://developer.mozilla.org/en-US/docs/Web/Events e.g. keydown, keyup, resize, etc.
     * @param eventTarget Web API objects for which the event is registered.
     * @param gts
     * @private
     * @return {undefined}
     */
    _maybeRegisterEvent(typeOfEvent, eventTarget, gts: GlobalTesterState): void {
        let target: Target | undefined
        if (this._isMouseEvent(typeOfEvent)) {
            target = this._handleMouseEventRegistration(eventTarget, typeOfEvent)
        } else if (typeOfEvent === "keydown" || typeOfEvent === "keyup" || typeOfEvent === "keypress") {
            target = this._handleKeyEventRegistration(eventTarget, typeOfEvent);
        } else if (typeOfEvent === "resize" || typeOfEvent === "load") {
            target = this._handleWindowEventRegistration(eventTarget, typeOfEvent);
        }
        if (target) {
            getSymJSHandler().eventHandlerDiscovered(target, gts.getBranchSequence());
            testTracerGetInstance().testNewEventDiscovered(target);
        }
    }

    runEventRacer(symJSHandler: SymJSHandler): void {
        const eR = this._eventRacer;
        eR.sharedVars = symJSHandler._sharedVariables;
        eR.constructEventGraph();
        eR.constructVectorClocks();
        eR.detectRaces();
        eR.printDebugs();
    }

    countEventSeqs(): number {
        const counter = new CountEventSeqs(this._eventRacer.getEventGraph());
        counter.countNrOfPossibleEventSequences();
        return counter.nrOfPossibleEventSequences;
      }

    _resetEventRacer(): void {
        this._eventRacer.reset();
    }

    getDataRaces(): any[] {
        return this._eventRacer.detectedDataRaces;
    }

    readSharedVar(v): void {
        if ((Object.values(this._eventRacer.sharedVars)).includes(v))
          this._eventRacer.readSharedVariable(v);
    }

    writeSharedVar(v): void {
        if ((Object.values(this._eventRacer.sharedVars)).includes(v))
          this._eventRacer.writeSharedVariable(v);
    }

}
