import Advice from "../../src/instrumentation/advice"
import {SymbolicExpression} from "../../src/symbolic_expression/symbolic_expressions"
import SymBool from "../../src/symbolic_expression/symbolic_bool"
import Interceptor from "../../src/instrumentation/interceptor/Interceptor"
import Process from "../../src/process/processes/Process"
import tainter, {dirty, tame, value, wild} from "../../src/instrumentation/tainter";
import {TestRunner} from "../../src/tester/test-runners/TestRunner"
import {jsPrint} from "../../src/util/io_operations"
import TestTracer from "@src/tester/TestTracer/TestTracer"

export class InterceptorMock {
    public pageLoadedCounter = 0
    pageLoaded() {
        this.pageLoadedCounter++
    }
}

export class TestRunnerMock {
    public serverFinishedSetupCounter = 0 
    public receiveSymbolicTestConditionCounter = 0
    public _processes: ProcessMock[] = [
        new ProcessMock
    ]

    serverFinishedSetup() {
        this.serverFinishedSetupCounter++
    }

    receiveSymbolicTestCondition(serial, alias, constraint) {
        this.receiveSymbolicTestConditionCounter++
    }

}

export class ProcessMock {
    public finishedSetupCounter = 0
    public readSharedVarCounter = 0
    finishedSetup() {
        this.finishedSetupCounter++
    }

    readSharedVar(varName) {
        this.readSharedVarCounter++
    }

    isANodeProcess(): boolean {
        return false
    }
}

export class AranMock {

    public reflect = new ReflectMock
    public nodes: object[] = [
        {
            type: "Identifier"
        },
        {
            type: "typeMock"
        }
    ]

}

export class ReflectMock {
    public binaryCounter = 0

    unary(operator: string, arg) {
        switch(operator) {
            case "typeof":
                return typeof(arg)
            case "!":
                return !arg
        }
    }

    binary(operator: string, left, right) {
        switch(operator) {
            case "+":
                return left + right
            case "-":
                return left - right
            case ">":
                return left > right
            case "<":
                return left < right
            case "===":
                return left === right
        }
    
    }
}

export class TestTracerMock {
    public testVariableReadCounter = 0

    testVariableRead(varName, $$produced) {
        this.testVariableReadCounter++
    }
}

/**
 * ADVICE MOCK:
 *   only overrides the methods of Advice that we want mocked
 *   methods we want to test are not overwritten
 */
export class AdviceMock extends Advice {
    public _nodeCoveredCounter = 0
    public _errorDiscoveredCounter = 0
    public _addIdentifierReadCounter = 0
    public _getAssignmentIdentifierCounter = 0
    public _addIdentifierWrittenCounter = 0

    //at run-time, the constructor will be given mocks (e.g. TestRunnerMock instead of TestRunner)
    //We make the compiler happy by using type casts
    constructor(public _aran: any, public _pointcut: Function, public _testRunner: TestRunner, public _interceptor: Interceptor, public _argm: any, public _process: Process, public _testTracer: TestTracer) {
        super(_aran, _pointcut, null, null, _argm, _process, null, _testTracer);
    }

    getInterceptorMock(): InterceptorMock {
        return this._interceptor as unknown as InterceptorMock
    }

    getTestRunnerMock(): TestRunnerMock {
        return this._testRunner as unknown as TestRunnerMock
    }

    getProcessMock(): ProcessMock {
        return this._process as unknown as ProcessMock
    }

    getTestTracerMock(): TestTracerMock {
        return this._testTracer as unknown as TestTracerMock
    }

    protected _nodeCovered(serial: number): void {
        this._nodeCoveredCounter++
    }

    protected _errorDiscovered(errorMessage: any, serial: number): void {
        this._errorDiscoveredCounter++
    }

    protected _addIdentifierRead(identifier: string, $$value: dirty): dirty {
        this._addIdentifierReadCounter++
        return tainter.taintAndCapture(false, new SymBool(false));
    }

    protected _addIdentifierWritten(identifier, conValue, symValue: SymbolicExpression): SymbolicExpression {
        this._addIdentifierWrittenCounter++
        return new SymBool(false);
    }

    protected _getAssignmentIdentifier(aranNode) {
        this._getAssignmentIdentifierCounter++
    }
}

export function newAdviceMock(): AdviceMock {
    return new AdviceMock(
        new AranMock,
        () => {
          jsPrint("pointcut mock")
        },
        new TestRunnerMock as unknown as TestRunner,
        new InterceptorMock as unknown as Interceptor,
        {
            alias: "aliasMock"
        },
        new ProcessMock as unknown as Process,
        new TestTracerMock as unknown as TestTracer
       )
}
