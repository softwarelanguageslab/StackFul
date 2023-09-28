import { dirty } from "../../src/instrumentation/tainter"
import { AdviceMock, newAdviceMock } from "./mock"
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool"

//a unit-test for the advice function test

test('advice write', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: false,
        meta: "",
        symbolic: new SymbolicBool(false),
        varName: ""
    } //should NOT call receiveSymbolicTestCondition

    const unaryRes: any = advice.write($$arg, "varName", 0)

    expect(advice._getAssignmentIdentifierCounter).toBe(1)
    expect(advice.getTestRunnerMock()._processes[0].readSharedVarCounter).toBe(1)
    expect(advice._addIdentifierWrittenCounter).toBe(1)
})