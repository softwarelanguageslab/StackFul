import { dirty } from "../../src/instrumentation/tainter"
import SymbolicString from "../../src/symbolic_expression/symbolic_string"
import Nothing from "../../src/symbolic_expression/nothing"
import SymbolicInt from "../../src/symbolic_expression/symbolic_int"
import { AdviceMock, newAdviceMock } from "./mock"
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool"
import SymbolicUnaryExp from "../../src/symbolic_expression/symbolic_unary_exp"
import TestTracer from "@src/tester/TestTracer/TestTracer"
import ConfigurationReader from "@src/config/user_argument_parsing/Config"

test('advice read', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const readRes: dirty = advice.read($$arg, "varName", 1) //the if-test within read will NOT succeed

    expect(readRes).toBe($$arg)
    expect(advice._addIdentifierReadCounter).toBe(0)
    expect(advice.getTestTracerMock().testVariableReadCounter).toBe(0)
})

test('advice read', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const readRes: dirty = advice.read($$arg, "varName", 0) //the if-test within read WILL succeed

    expect(readRes).toBe($$arg)
    expect(advice._addIdentifierReadCounter).toBe(1)
    expect(advice.getTestTracerMock().testVariableReadCounter).toBe(1)
})