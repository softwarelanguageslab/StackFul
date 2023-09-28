import {newAdviceMock, AdviceMock, InterceptorMock, } from "./mock";
import { dirty } from "../../src/instrumentation/tainter";
import SymbolicInt from "../../src/symbolic_expression/symbolic_int";
import SymbolicArithmeticExp from "../../src/symbolic_expression/symbolic_arithmetic_exp";
import SymbolicRelationalExp from "../../src/symbolic_expression/symbolic_relational_exp";
import SymbolicString from "../../src/symbolic_expression/symbolic_string";
import SymbolicStringExpression from "../../src/symbolic_expression/symbolic_string_expression";

//PART 1: TEST BINARY WITH INTEGERS
test('advice binary ints: +', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const $$right: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("+", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(3)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicArithmeticExp)
    expect(primitiveRes.symbolic.operator).toBe("+")

    primitiveRes.symbolic.args.forEach(symInt => {
        expect(symInt).toBeInstanceOf(SymbolicInt)
    });
})

test('advice binary ints: -', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }

    const $$right: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("-", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(1)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicArithmeticExp)
    expect(primitiveRes.symbolic.operator).toBe("-")

    primitiveRes.symbolic.args.forEach(symInt => {
        expect(symInt).toBeInstanceOf(SymbolicInt)
    });
})

test('advice binary ints: > (1)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }

    const $$right: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary(">", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(true)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicRelationalExp)
    expect(primitiveRes.symbolic.operator).toBe(">")

    expect(primitiveRes.symbolic.left.i).toBe(2)
    expect(primitiveRes.symbolic.right.i).toBe(1)
});

test('advice binary ints: > (2)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const $$right: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary(">", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(false)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicRelationalExp)
    expect(primitiveRes.symbolic.operator).toBe(">")

    expect(primitiveRes.symbolic.left.i).toBe(1)
    expect(primitiveRes.symbolic.right.i).toBe(2)
});

test('advice binary ints: < (1)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }

    const $$right: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("<", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(true)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicRelationalExp)
    expect(primitiveRes.symbolic.operator).toBe("<")

    expect(primitiveRes.symbolic.left.i).toBe(1)
    expect(primitiveRes.symbolic.right.i).toBe(2)
})

test('advice binary ints: < (2)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: 2,
        meta: "",
        symbolic: new SymbolicInt(2),
        varName: ""
    }

    const $$right: dirty = {
        base: 1,
        meta: "",
        symbolic: new SymbolicInt(1),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("<", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(false)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicRelationalExp)
    expect(primitiveRes.symbolic.operator).toBe("<")

    expect(primitiveRes.symbolic.left.i).toBe(2)
    expect(primitiveRes.symbolic.right.i).toBe(1)
})

//PART 2: TEST BINARY WITH STRINGS:
test('advice binary strings: +', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: "hello",
        meta: "",
        symbolic: new SymbolicString("hello"),
        varName: ""
    }

    const $$right: dirty = {
        base: " world",
        meta: "",
        symbolic: new SymbolicString(" world"),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("+", $$left, $$right, 999)

    expect(primitiveRes.base).toBe("hello world")
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicStringExpression)
    expect(primitiveRes.symbolic.operator).toBe("string_append")

    primitiveRes.symbolic.args.forEach(symInt => {
        expect(symInt).toBeInstanceOf(SymbolicString)
    });
})

test('advice binary strings: === (1)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: "hello",
        meta: "",
        symbolic: new SymbolicString("hello"),
        varName: ""
    }

    const $$right: dirty = {
        base: "hello",
        meta: "",
        symbolic: new SymbolicString(" hello"),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("===", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(true)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicStringExpression)
    expect(primitiveRes.symbolic.operator).toBe("string_equal")

    primitiveRes.symbolic.args.forEach(symInt => {
        expect(symInt).toBeInstanceOf(SymbolicString)
    });
})

test('advice binary strings: === (2)', () => {
    const advice: AdviceMock = newAdviceMock()
    const $$left: dirty = {
        base: "hello",
        meta: "",
        symbolic: new SymbolicString("hello"),
        varName: ""
    }

    const $$right: dirty = {
        base: " world",
        meta: "",
        symbolic: new SymbolicString(" world"),
        varName: ""
    }
    const primitiveRes: dirty = advice.binary("===", $$left, $$right, 999)

    expect(primitiveRes.base).toBe(false)
    expect(primitiveRes.symbolic).toBeInstanceOf(SymbolicStringExpression)
    expect(primitiveRes.symbolic.operator).toBe("string_equal")

    primitiveRes.symbolic.args.forEach(symInt => {
        expect(symInt).toBeInstanceOf(SymbolicString)
    });
})
