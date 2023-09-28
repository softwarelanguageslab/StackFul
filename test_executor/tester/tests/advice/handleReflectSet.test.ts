import { dirty, wild } from "../../src/instrumentation/tainter"
import SymbolicString from "../../src/symbolic_expression/symbolic_string"
import Nothing from "../../src/symbolic_expression/nothing"
import SymbolicInt from "../../src/symbolic_expression/symbolic_int"
import { AdviceMock, newAdviceMock } from "./mock"
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool"
import SymbolicUnaryExp from "../../src/symbolic_expression/symbolic_unary_exp"
import ConfigurationReader from "@src/config/user_argument_parsing/Config"

//TODO:
//  Use a similar strategy to add a test for _handleReflectGet
//  Although this is easier said than done


test('advice handleReflectSet', () => {
    const advice: AdviceMock = newAdviceMock()

    const $$function: dirty = {
        base: Object.defineProperty,
        meta: "",
        symbolic: new Nothing(),
        varName: ""
    } 

    const $$value2: dirty = {
        base: undefined,
        meta: "",
        symbolic: new Nothing(),
        varName: ""
    } 

    const object: dirty = {
        base: {x: 1} as unknown as wild,
        meta: "",
        symbolic: new Nothing(),
        varName: "",
    }

    const key: dirty = {
        base: "x",
        meta: "",
        symbolic: new SymbolicString("x"),
        varName: "",
    }

    const valueToSet: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: "",
    }

    ConfigurationReader.handleArguments([])
    const res: dirty = advice._handleReflectSet($$function, $$value2, [object, key, valueToSet], 1)
    expect(res.base).toBe(undefined)
    expect(res.symbolic).toBeInstanceOf(Nothing)
})