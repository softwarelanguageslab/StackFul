import { dirty } from "../../src/instrumentation/tainter"
import SymbolicString from "../../src/symbolic_expression/symbolic_string"
import Nothing from "../../src/symbolic_expression/nothing"
import SymbolicInt from "../../src/symbolic_expression/symbolic_int"
import { AdviceMock, newAdviceMock } from "./mock"
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool"
import SymbolicUnaryExp from "../../src/symbolic_expression/symbolic_unary_exp"

//a unit-test for the advice function test

test('advice test', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: false,
        meta: "",
        symbolic: new SymbolicBool(false),
        varName: ""
    } //should NOT call receiveSymbolicTestCondition

    const unaryRes: any = advice.test($$arg, 999)

    expect(advice.getTestRunnerMock().receiveSymbolicTestConditionCounter).toBe(1)
    expect(unaryRes).toBe(false)
})

test('advice test', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: true,
        meta: "",
        symbolic: new SymbolicBool(true),
        varName: ""
    }   //should call receiveSymbolicTestCondition

    const unaryRes: any = advice.test($$arg, 999)

    expect(advice.getTestRunnerMock().receiveSymbolicTestConditionCounter).toBe(0)
    expect(unaryRes).toBe(true)
})