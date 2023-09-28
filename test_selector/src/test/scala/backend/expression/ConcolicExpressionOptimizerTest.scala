package backend.expression

import org.scalatest.funsuite.AnyFunSuite

class SymbolicExpressionOptimizerTest extends AnyFunSuite {

  private def testSimpleArithExp(
    op: IntegerArithmeticalOperator,
    args: List[Int],
    expectedValue: Int
  ): Unit = {
    val input = ArithmeticalVariadicOperationExpression(op, args.map(SymbolicInt(_, None)))
    val expected = SymbolicInt(expectedValue)
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  /** *****************************************************************************
    * Test simple (i.e., arguments only consist of constants), binary expressions *
    * ******************************************************************************/
  test("Optimize simple, binary Plus expression") {
    testSimpleArithExp(IntPlus, List(1, 2), 3)
  }

  test("Optimize simple, binary Minus expression") {
    testSimpleArithExp(IntMinus, List(10, 2), 8)
  }

  test("Optimize simple, binary Times expression") {
    testSimpleArithExp(IntTimes, List(10, 2), 20)
  }

  test("Optimize simple, binary Div expression") {
    testSimpleArithExp(IntDiv, List(10, 2), 5)
  }

  test("Optimize single, unary Inverse expression") {
    testSimpleArithExp(IntInverse, List(2), -2)
  }

  /** *******************************************************************************
    * Test simple (i.e., arguments only consist of constants), variadic expressions *
    * ********************************************************************************/
  test("Optimize simple, variadic Plus expression") {
    testSimpleArithExp(IntPlus, List(1, 2, 3, 4), 10)
  }

  test("Optimize simple, variadic Minus expression") {
    testSimpleArithExp(IntMinus, List(10, 1, 2), 7)
  }

  test("Optimize simple, variadic Times expression") {
    testSimpleArithExp(IntTimes, List(1, 2, 3, 4, 5), 120)
  }

  /** ******************************************************************************
    * Test simple (i.e., arguments only consist of constants), nullary expressions *
    * *******************************************************************************/
  test("Optimize simple, nullary Plus expression") {
    testSimpleArithExp(IntPlus, Nil, 0)
  }

  test("Optimize simple, nullary Times expression") {
    testSimpleArithExp(IntTimes, Nil, 1)
  }

  /** ************************************************************************
    * Test complex (i.e., arguments may also have to be reduced) expressions *
    * *************************************************************************/
  test("Optimize Minus expression 1") {
    val arg1 = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(10, 1, 2, 3).map(SymbolicInt(_)))
    val input =
      ArithmeticalVariadicOperationExpression(
        IntMinus,
        List(SymbolicInputInt(RegularId(0, 1)), arg1))
    val expected = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(SymbolicInputInt(RegularId(0, 1)), SymbolicInt(4)))
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  test("Optimize Minus expression 2") {
    /* arg1: i0 - 1; becomes: i0 - 1 */
    val arg1 =
      ArithmeticalVariadicOperationExpression(
        IntMinus,
        List(SymbolicInputInt(RegularId(0, 1)), SymbolicInt(1)))
    /* arg2: 10 - 1 - 2 - 3 - i1; becomes: 4 - i1 */
    val arg2 = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(10, 1, 2, 3).map(SymbolicInt(_, None)) :+ SymbolicInputInt(RegularId(1, 0)))
    /* arg3: i1 - i2 - 4 - i3 - 5; becomes: i1 - 9 - i2 - i3 (reduced args are placed as the second of the arguments) */
    val arg3 = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(1, 2)
        .map((i) => SymbolicInputInt(RegularId(i, 0))) :+ SymbolicInt(4) :+ SymbolicInputInt(
        RegularId(3, 0)) :+ SymbolicInt(5))
    /* arg4: 1; becomes: 1 */
    val arg4 = SymbolicInt(1)
    /* input: 10 - arg1 - arg2 - arg3 - arg4 */
    val input =
      ArithmeticalVariadicOperationExpression(IntMinus, List(SymbolicInt(10), arg1, arg2, arg3, arg4))
    val expected = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(
        SymbolicInt(9), // head - arg4
        arg1, // arg1
        ArithmeticalVariadicOperationExpression(
          IntMinus,
          List(SymbolicInt(4), SymbolicInputInt(RegularId(1, 0)))), // arg2
        ArithmeticalVariadicOperationExpression(
          IntMinus,
          List(
            SymbolicInputInt(RegularId(1, 0)),
            SymbolicInt(9),
            SymbolicInputInt(RegularId(2, 0)),
            SymbolicInputInt(RegularId(3, 0))))
      )
    ) // arg3
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  /* (2 + 3) / 1 */
  test("Optimize complex Expression 1") {
    val arg1 = ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(2), SymbolicInt(3)))
    val input = ArithmeticalVariadicOperationExpression(IntDiv, List(arg1, SymbolicInt(1)))
    val expected = SymbolicInt(5)
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  /* ((1 + 2 + 3) + ((4 * 5) + 6 + 7) + 8) - 9 - 10 */
  test("Optimize complex Expression 2") {
    val arg1 =
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(1), SymbolicInt(2), SymbolicInt(3)))
    val arg2 = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(4), SymbolicInt(5)))
    val arg3 = ArithmeticalVariadicOperationExpression(IntPlus, List(arg2, SymbolicInt(6), SymbolicInt(7)))
    val arg4 = ArithmeticalVariadicOperationExpression(IntPlus, List(arg1, arg3, SymbolicInt(8)))
    val input =
      ArithmeticalVariadicOperationExpression(IntMinus, List(arg4, SymbolicInt(9), SymbolicInt(10)))
    val expected = SymbolicInt(((1 + 2 + 3) + ((4 * 5) + 6 + 7) + 8) - 9 - 10)
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  /* i0 + (2 * 3) */
  test("Optimize complex Expression 3") {
    val arg1 = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(2), SymbolicInt(3)))
    val input =
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInputInt(RegularId(0, 0)), arg1))
    val expected =
      ArithmeticalVariadicOperationExpression(
        IntPlus,
        List(SymbolicInputInt(RegularId(0, 0)), SymbolicInt(6)))
    val optimized = SymbolicExpressionOptimizer.optimizeArithExp(input)
    /*
     * The optimization might have changed the ordering of the arguments, though that doesn't matter because
     * addition is commutative anyway.
     */
    assert(optimized.isInstanceOf[ArithmeticalVariadicOperationExpression])
    assert(optimized.asInstanceOf[ArithmeticalVariadicOperationExpression].op == expected.op)
    assert(optimized.asInstanceOf[ArithmeticalVariadicOperationExpression].exps.toSet == expected.exps.toSet)
  }

  /* ((1 + 2 + 3) + ((0 * 5) + 6 + 7) + 8) - 9 - 10 */
  test("Optimize complex Expression 4") {
    val arg1 =
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(1), SymbolicInt(2), SymbolicInt(3)))
    val arg2 =
      ArithmeticalVariadicOperationExpression(
        IntTimes,
        List(SymbolicInputInt(RegularId(0, 0)), SymbolicInt(5)))
    val arg3 = ArithmeticalVariadicOperationExpression(IntPlus, List(arg2, SymbolicInt(6), SymbolicInt(7)))
    val arg4 = ArithmeticalVariadicOperationExpression(IntPlus, List(arg1, arg3, SymbolicInt(8)))
    val input =
      ArithmeticalVariadicOperationExpression(IntMinus, List(arg4, SymbolicInt(9), SymbolicInt(10)))
    val expected = ArithmeticalVariadicOperationExpression(
      IntMinus,
      List(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInt(14),
            ArithmeticalVariadicOperationExpression(
              IntPlus,
              List(
                SymbolicInt(13),
                ArithmeticalVariadicOperationExpression(
                  IntTimes,
                  List(
                    SymbolicInt(5),
                    SymbolicInputInt(RegularId(0, 0))))))
          )
        ),
        SymbolicInt(19)
      )
    )
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

  /* 1 * (2 * (3 * (4 * (i0 * 1)))) */
  test("Optimize complex Expression 5") {
    val arg1 =
      ArithmeticalVariadicOperationExpression(
        IntTimes,
        List(SymbolicInputInt(RegularId(0, 0)), SymbolicInt(1)))
    val arg2 = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(4), arg1))
    val arg3 = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(3), arg2))
    val arg4 = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(2), arg3))
    val input = ArithmeticalVariadicOperationExpression(IntTimes, List(SymbolicInt(1), arg4))
    val expected = ArithmeticalVariadicOperationExpression(
      IntTimes,
      List(SymbolicInt(1 * 2 * 3 * 4), SymbolicInputInt(RegularId(0, 0))))
    assert(SymbolicExpressionOptimizer.optimizeArithExp(input) == expected)
  }

}
