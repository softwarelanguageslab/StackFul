import SymbolicInt from "../../src/symbolic_expression/symbolic_int";
import Nothing from "../../src/symbolic_expression/nothing";
import { SymbolicNullObject } from "../../src/symbolic_expression/symbolic_null_object";
import { dirty } from "../../src/instrumentation/tainter";
import {newAdviceMock, AdviceMock, InterceptorMock, } from "./mock";
import SymbolicBool from "../../src/symbolic_expression/symbolic_bool";
import SymbolicString from "../../src/symbolic_expression/symbolic_string";

test('advice primitive: an integer', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive(1, 999)
    expect(advice._nodeCoveredCounter).toBe(1)

    expect(primitiveRes.base).toBe(1);
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicInt)
    expect(primitiveRes.symbolic.i).toBe(1);
})

test('advice primitive: undefined', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive(undefined, 999)
    expect(advice._nodeCoveredCounter).toBe(1)

    expect(primitiveRes.base).toBe(undefined);
    expect(primitiveRes.symbolic).toBeInstanceOf(Nothing)
})

test('advice primitive: boolean', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive(true, 999)
    expect(advice._nodeCoveredCounter).toBe(1)

    expect(primitiveRes.base).toBe(true);
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicBool)
    expect(primitiveRes.symbolic.b).toBe(true);
})

test('advice primitive: null', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive(null, 999)
    expect(advice._nodeCoveredCounter).toBe(1)

    expect(primitiveRes.base).toBe(null);
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicNullObject);
    expect(primitiveRes.symbolic.n).toBe(null);
})

test('advice primitive: string', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive("someString", 999)
    expect(advice._nodeCoveredCounter).toBe(1)

    expect(primitiveRes.base).toBe("someString");
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicString);
    expect(primitiveRes.symbolic.s).toBe("someString");
})

test('advice primitive: error discovered', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive("ERROR STRING", 999)
    expect(advice._nodeCoveredCounter).toBe(1)
    expect(advice._errorDiscoveredCounter).toBe(1)
})

test('advice primitive: client entered', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive("CLIENT ENTERED", 999)
    expect(advice._nodeCoveredCounter).toBe(1)
    expect(advice.getInterceptorMock().pageLoadedCounter).toBe(1)
})

test('advice primitive: finished setup', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive("FINISHED_SETUP", 999)
    expect(advice._nodeCoveredCounter).toBe(1)
    expect(advice.getTestRunnerMock().serverFinishedSetupCounter).toBe(1)
})

test('advice primitive: "END ANNOTATION"', () => {
    const advice: AdviceMock = newAdviceMock()
    const primitiveRes: dirty = advice.primitive("END ANNOTATION", 999)
    expect(advice._nodeCoveredCounter).toBe(1)
    expect(advice.getProcessMock().finishedSetupCounter).toBe(1)
})