import ClickableTarget from "@src/process/targets/ClickableTarget";
import WindowTarget from "@src/process/targets/WindowTarget";
import SymJSState from "@src/tester/solve/symjs_state";
import Priority from "@src/tester/solve/priority_implementation/priority";
import SymbolicInputInt from "@src/symbolic_expression/symbolic_input_int";
import Nothing from "@src/symbolic_expression/nothing";
import SymbolicInt from "@src/symbolic_expression/symbolic_int";
import {
    DebuggingTest,
    EventsTest,
    InputsTest,
    MetricsTest, NewEventDiscoveredTest,
    NewIterationType,
    StateDequeuedTest,
    VariableReadTest
} from "@src/tester/TestTracer/AssertionClasses";
import {LocationExecutionState} from "@src/tester/ExecutionState.js";
import Event from "@src/tester/Event.js";

export const MAIN_BRUTE = [
    new NewIterationType(),
    new InputsTest([]),
    new EventsTest([]),
    new MetricsTest({linesCovered: 575, linesCoveredPerProcess: 575, nrOfErrors: 1}),
    new NewIterationType(), new InputsTest([])
];

export const MAIN_SYMJS = [new NewIterationType()];

export const FS_WHITEBOARD_BRUTE = [
    new NewIterationType(),
    new InputsTest([]),
    new EventsTest([{processId: 1, targetId: 0}]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "mousedown")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "mouseup")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 2, undefined, "mouseout")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 3, undefined, "mousemove")),
    new NewEventDiscoveredTest(new WindowTarget(1, 4, undefined, "resize")),
    new MetricsTest({linesCovered: 67, linesCoveredPerProcess: 67, nrOfErrors: 0})
];

export const FS_WHITEBOARD_SYMJS = [new StateDequeuedTest(new SymJSState([], ""), new Priority(+Infinity, -Infinity)),
    new NewIterationType(), new InputsTest([]), new EventsTest([]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "mousedown")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "mouseup")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 2, undefined, "mouseout")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 3, undefined, "mousemove")),
    new NewEventDiscoveredTest(new WindowTarget(1, 4, undefined, "resize")),
    new MetricsTest({linesCovered: 62, linesCoveredPerProcess: 62, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([new Event(1, 0)], ""), new Priority(+Infinity, -Infinity))
];

export const FS_VERIFY_INTRA_BRUTE = [new NewIterationType(), new InputsTest([]), new EventsTest([{
    processId: 1,
    targetId: 0
}]),
    new DebuggingTest("DEBUGGING CLIENT before Button0 registration"),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "click")),
    new DebuggingTest("DEBUGGING CLIENT before Button1 registration"),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "click")),
    new DebuggingTest("DEBUGGING CLIENT, all buttons registered"),
    // new FiringEventsTest(undefined)
    new DebuggingTest("DEBUGGING SERVER connection"),
    new DebuggingTest("DEBUGGING SERVER after socket.on"),
    new DebuggingTest("DEBUGGING CLIENT Button0 click 1"),
    new DebuggingTest("DEBUGGING CLIENT handleComputation val === 0, 1"),
    new DebuggingTest("DEBUGGING CLIENT handleComputation val === 0, 2"),
    new DebuggingTest("DEBUGGING SERVER 0_pressed"),
    new DebuggingTest("DEBUGGING SERVER 0_pressed, val !== 0"),
    new DebuggingTest("Process verify_intra_client_1 encountered DEBUGGING C server_reply"),
    new MetricsTest({iteration: 1, linesCovered: 51, linesCoveredPerProcess: 51, nrOfErrors: 0}),
    new NewIterationType(),
];

// const dummyExecutionState = new LocationExecutionState(new CodePosition(0, 0), [], [], null);
const dummyExecutionState = 0 as unknown as LocationExecutionState;

export const NAMES_1_SYMJS = [ // Iteration 1
    new StateDequeuedTest(new SymJSState([], ""), new Priority(+Infinity, -Infinity)),
    new NewIterationType(), new InputsTest([]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 0, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.x", {base: 1, symbolic: new SymbolicInt(1)}),
    new MetricsTest({iteration: 1, linesCovered: 9, linesCoveredPerProcess: 9, nrOfErrors: 0}),
    // The following 7 states were created by forking some implicit (invisible) condition and are always unsatisfiable
    new StateDequeuedTest(new SymJSState([], "E"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTT"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTEE"), new Priority(-Infinity, +Infinity)),


    // Iteration 2
    new StateDequeuedTest(new SymJSState([], "TTTTTETE"), new Priority(-Infinity, +Infinity)),
    new NewIterationType(), new InputsTest([{type: "int", value: 1000, processId: 0, id: 0}]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 1000, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.y", {base: 2, symbolic: new SymbolicInt(2)}),
    new MetricsTest({iteration: 1, linesCovered: 11, linesCoveredPerProcess: 11, nrOfErrors: 0}),


    // Iteration 3
    // Next 3 states are again forked by some implicit condition that is always unsatisfiable
    new StateDequeuedTest(new SymJSState([], "TTTTTETTE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTE"), new Priority(-Infinity, +Infinity)),

    new StateDequeuedTest(new SymJSState([], "TTTTTETT"), new Priority(-Infinity, +Infinity)),
    new NewIterationType(), new InputsTest([{type: "int", value: 999, processId: 0, id: 0}]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 999, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.x", {base: 1, symbolic: new SymbolicInt(1)}),
    new MetricsTest({iteration: 1, linesCovered: 11, linesCoveredPerProcess: 11, nrOfErrors: 0}),


    // Iteration 4
    // All remaining states are again forked by some implicit condition that is always unsatisfiable
    new StateDequeuedTest(new SymJSState([], "TTTTTETEE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETETE"), new Priority(-Infinity, +Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETETTE"), new Priority(-Infinity, +Infinity))
];

export const NAMES_2_SYMJS = [ // Iteration 1,
    new StateDequeuedTest(new SymJSState([], ""), new Priority(Infinity, -Infinity)),
    new NewIterationType(), new InputsTest([]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 0, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.x", {base: 1, symbolic: new SymbolicInt(1)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o.x", {base: 11, symbolic: new SymbolicInt(11)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o.y", {base: 21, symbolic: new SymbolicInt(21)}),
    new MetricsTest({iteration: 1, linesCovered: 14, linesCoveredPerProcess: 14, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([], "E"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTT"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTEE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETE"), new Priority(-Infinity, Infinity)),

    // Iteration 2,
    new NewIterationType(), new InputsTest([{type: "int", id: 0, processId: 0, value: 1000}]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 1000, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.y", {base: 2, symbolic: new SymbolicInt(2)}),
    new MetricsTest({iteration: 2, linesCovered: 16, linesCoveredPerProcess: 16, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETTTTTTTTTTTTTTTE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETT"), new Priority(-Infinity, Infinity)),

    // Iteration 3,
    new NewIterationType(), new InputsTest([{type: "int", id: 0, processId: 0, value: 999}]), new EventsTest([]),
    new VariableReadTest("__test__i1", {base: 0, symbolic: new SymbolicInputInt(0, dummyExecutionState, 0)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.x", {base: 1, symbolic: new SymbolicInt(1)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o.x", {base: 11, symbolic: new SymbolicInt(11)}),
    new VariableReadTest("__test__o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o", {base: {}, symbolic: new Nothing()}),
    new VariableReadTest("__test__o.o.y", {base: 21, symbolic: new SymbolicInt(21)}),
    new MetricsTest({iteration: 3, linesCovered: 16, linesCoveredPerProcess: 16, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETETE"), new Priority(-Infinity, Infinity)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETETTE"), new Priority(-Infinity, Infinity))
];

export const FS_2BUTTONS_SYMJS = [
    new StateDequeuedTest(new SymJSState([], ""), new Priority(+Infinity, -Infinity)),

    new NewIterationType(),
    new InputsTest([]),
    new EventsTest([]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "click")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "click")),
    new MetricsTest({iteration: 1, linesCovered: 22, linesCoveredPerProcess: 22, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([], "E"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTT"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTEE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETT"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETET"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEEE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETT"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETEE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETETE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETETTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETETTTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETETTTTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([], "TTTTTETEETETTTTTE"), new Priority(Infinity, 0)),
    new StateDequeuedTest(new SymJSState([new Event(1, 0)], "TTTTTETEETETTTTTT"), new Priority(Infinity, 1)),

    new NewIterationType(),
    new InputsTest([]),
    new EventsTest([new ClickableTarget(1, 0, undefined, "click")]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "click")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "click")),
    new DebuggingTest("DEBUGGING ELSE-B1..."),
    new MetricsTest({iteration: 2, linesCovered: 27, linesCoveredPerProcess: 27, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTT"), new Priority(Infinity, 1)),

    new NewIterationType(),
    new InputsTest([]),
    new EventsTest([new ClickableTarget(1, 1, undefined, "click")]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "click")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "click")),
    new DebuggingTest("DEBUGGING ELSE-B2..."),
    new MetricsTest({iteration: 3, linesCovered: 32, linesCoveredPerProcess: 32, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTTE"), new Priority(0, 1)),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTTTT"), new Priority(0, 1)),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTTTET"), new Priority(0, 1)),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTTTEET"), new Priority(0, 1)),

    new NewIterationType(),
    new InputsTest([{"type":"int","value":701,"processId":1,"id":0}]),
    new EventsTest([new ClickableTarget(1, 1, undefined, "click")]),
    new NewEventDiscoveredTest(new ClickableTarget(1, 0, undefined, "click")),
    new NewEventDiscoveredTest(new ClickableTarget(1, 1, undefined, "click")),
    new DebuggingTest("DEBUGGING THEN-B2..."),
    new MetricsTest({iteration: 4, linesCovered: 33, linesCoveredPerProcess: 33, nrOfErrors: 0}),
    new StateDequeuedTest(new SymJSState([new Event(1, 1)], "TTTTTETEETETTTTTTTEEE"), new Priority(0, 1)),

    new NewIterationType(),
]