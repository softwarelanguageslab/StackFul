import { dirty } from "../../src/instrumentation/tainter"
import SymbolicString from "../../src/symbolic_expression/symbolic_string"
import Nothing from "../../src/symbolic_expression/nothing"
import SymbolicInt from "../../src/symbolic_expression/symbolic_int"
import { AdviceMock, newAdviceMock } from "./mock"
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool"
import SymbolicUnaryExp from "../../src/symbolic_expression/symbolic_unary_exp"

//PART 1: TEST UNARY WITH NUMBERS
test('advice unary numbers', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const unaryRes: dirty = advice.unary("typeof", $$arg, 999)

    expect(unaryRes.base).toBe("number")
    expect(unaryRes.symbolic).toBeInstanceOf(Nothing)
})

//PART 2: TEST UNARY WITH STRINGS
test('advice unary strings', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: "hello world",
        meta: "",
        symbolic: new SymbolicString("hello world"),
        varName: ""
    }

    const unaryRes: dirty = advice.unary("typeof", $$arg, 999)

    expect(unaryRes.base).toBe("string")
    expect(unaryRes.symbolic).toBeInstanceOf(Nothing)
})

//PART 3: TEST UNARY WITH BOOLEANS
test('advice unary booleans: typeof', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: true,
        meta: "",
        symbolic: new SymbolicBool(true),
        varName: ""
    }

    const unaryRes: dirty = advice.unary("typeof", $$arg, 999)

    expect(unaryRes.base).toBe("boolean")
    expect(unaryRes.symbolic).toBeInstanceOf(Nothing)
})

test('advice unary booleans: !', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$arg: dirty = {
        base: true,
        meta: "",
        symbolic: new SymbolicBool(true),
        varName: ""
    }

    const unaryRes: dirty = advice.unary("!", $$arg, 999)

    expect(unaryRes.base).toBe(false)
    expect(unaryRes.symbolic).toBeInstanceOf(SymbolicUnaryExp)
})
