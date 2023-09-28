package backend.tree.merging

import backend.SetUseDebugToTrueTester
import backend.expression.Util._
import backend.expression._

trait TestsSymbolicITECreator {

  protected var virtualPathStore: VirtualPathStore = newVirtualPathStore
  protected var iteCreator: SymbolicITECreator = new SymbolicITECreator(virtualPathStore)
  def newIteCreator: SymbolicITECreator = new SymbolicITECreator(virtualPathStore)
  def reset(): Unit = {
    virtualPathStore = newVirtualPathStore
    iteCreator = new SymbolicITECreator(virtualPathStore)
  }
  def newVirtualPathStore: VirtualPathStore = new VirtualPathStore

}

class SymbolicITECreatorTest extends SetUseDebugToTrueTester with TestsSymbolicITECreator {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  test("Merge two non-nested expressions: [(i0==0) ~ 1 ~ 2] => ITE(i0==0, 1, 2)") {
    val i0 = input(0)
    val pred = RelationalExpression(i0, IntEqual, i(0))
    val ite = iteCreator.createITE(pred, "T", "E", i(1, "x"), i(2, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0)), ite, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite, 2)
  }

  test("Merge nested with non-nested expressions: [(i0==0)~[(i1==10) ~ 1 ~ 2]~3] => ITE(i0==0, ITE(i1==10, 1, 2), 3)") {
    /**
      * var x;
      * if (i0 == 0) {
      * if (i1 == 10) {
      * x = 1;
      * else {
      * x = 2;
      * }
      * } else {
      * x = 3;
      * }
      */
    val i1 = input(1)
    val pred1 = RelationalExpression(i1, IntEqual, i(10))
    val ite1 = iteCreator.createITE(pred1, "TT", "TE", i(1, "x"), i(2, "x"), "x")

    // Check ite1
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 10)), ite1, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 11)), ite1, 2)

    // Check ite2
    iteCreator = newIteCreator
    val i0 = input(0)
    val pred2 = RelationalExpression(i0, IntEqual, i(0))
    val ite2 = iteCreator.createITE(pred2, "TT", "E", ite1, i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite2, 3)
    // Following two shouldn't matter, but check anyway
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 0)), ite2, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 1)), ite2, 3)
  }

  test("Simulate adding TTT, adding TTF, merging, adding TF, merging, adding F, merging") {
    /**
      * var x;
      * if (i0 == 0) {
      * if (i1 == 10) {
      * if (i2 == 100) {
      * x = 1;
      * else {
      * x = 2;
      * }
      * } else {
      * x = 3;
      * } else {
      * x = 4;
      * }
      */

    val i2 = input(2)

    val pred1 = RelationalExpression(i2, IntEqual, i(100))
    val ite1 = iteCreator.createITE(pred1, "TTT", "TTE", i(1, "x"), i(2, "x"), "x")

    // Check ite1
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 100)), ite1, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 101)), ite1, 2)

    // Check ite2
    iteCreator = newIteCreator
    val i1 = input(1)
    val pred2 = RelationalExpression(i1, IntEqual, i(10))
    val ite2 = iteCreator.createITE(pred2, "TTT", "TE", ite1, i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 10), (i2, 100)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 10), (i2, 101)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 11)), ite2, 3)

    // Check ite3
    iteCreator = newIteCreator
    val i0 = input(0)
    val pred3 = RelationalExpression(i0, IntEqual, i(0))
    val ite3 = iteCreator.createITE(pred3, "TTT", "E", ite2, i(4, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 100)), ite3, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 101)), ite3, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite3, 4)
    // Following two shouldn't matter, but check anyway
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 0)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 1)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 0)), ite3, 4)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 1)), ite3, 4)
  }

  test("Simulate adding TTT, adding TTF, merging, adding TF, merging with TTT, adding F, merging with TF") {
    /**
      * var x;
      * if (i0 == 0) {
      * if (i1 == 10) {
      * if (i2 == 100) {
      * x = 1;
      * else {
      * x = 2;
      * }
      * } else {
      * x = 3;
      * } else {
      * x = 4;
      * }
      */

    val i2 = input(2)

    val pred1 = RelationalExpression(i2, IntEqual, i(100))
    val ite1 = iteCreator.createITE(pred1, "TTT", "TTE", i(1, "x"), i(2, "x"), "x")

    // Check ite1
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 100)), ite1, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 101)), ite1, 2)

    // Check ite2
    iteCreator = newIteCreator
    val i1 = input(1)
    val pred2 = RelationalExpression(i1, IntEqual, i(10))
    val ite2 = iteCreator.createITE(pred2, "TTT", "TE", ite1, i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 10), (i2, 100)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 10), (i2, 101)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i1, 11)), ite2, 3)

    // Check ite3
    iteCreator = newIteCreator
    val i0 = input(0)
    val pred3 = RelationalExpression(i0, IntEqual, i(0))
    val ite3 = iteCreator.createITE(pred3, "TE", "E", ite2, i(4, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 100)), ite3, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 101)), ite3, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite3, 4)
    // Following two shouldn't matter, but check anyway
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 0)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 1)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 0)), ite3, 4)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 1)), ite3, 4)
  }

  test("Simulate adding TTT, adding TTF, merging, adding F, merging with TTT, adding TF, merging with TTT") {
    /**
      * var x;
      * if (i0 == 0) {
      * if (i1 == 10) {
      * if (i2 == 100) {
      * x = 1;
      * else {
      * x = 2;
      * }
      * } else {
      * x = 3;
      * } else {
      * x = 4;
      * }
      */

    val i2 = input(2)

    val pred1 = RelationalExpression(i2, IntEqual, i(100))
    val ite1 = iteCreator.createITE(pred1, "TTT", "TTE", i(1, "x"), i(2, "x"), "x")

    // Check ite1
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 100)), ite1, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i2, 101)), ite1, 2)

    // Check ite2
    iteCreator = newIteCreator
    val i0 = input(0)
    val pred2 = RelationalExpression(i0, IntEqual, i(0))
    val ite2 = iteCreator.createITE(pred2, "TTT", "E", ite1, i(4, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i2, 100)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i2, 101)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite2, 4)

    // Check ite3
    iteCreator = newIteCreator
    val i1 = input(1)
    val pred3 = RelationalExpression(i1, IntEqual, i(10))
    val ite3 = iteCreator.createITE(pred3, "TTT", "TE", ite2, i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 100)), ite3, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 10), (i2, 101)), ite3, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 11)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite3, 4)
    // Following two shouldn't matter, but check anyway
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 0)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 0), (i1, 1)), ite3, 3)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 0)), ite3, 4)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1), (i1, 1)), ite3, 4)
  }

  test("Simulate adding T, adding ET, adding EET, merging ET and EET, merging T and EET") {
    /**
      * var x;
      * if (i0 == 1} {
      * x = 1;
      * } else if (i0 == 2) {
      * x = 2;
      * } else if (i0 == 3) {
      * x = 3;
      * }
      * if (x == 0} {...} else {...}
      */

    val i0 = input(0)
    val pred1 = RelationalExpression(i0, IntEqual, 1)
    val pred2 = RelationalExpression(i0, IntEqual, 2)
    val pred3 = RelationalExpression(i0, IntEqual, 3)

    // Check ite1
    val ite1 = iteCreator.createITE(pred2, "ET", "EET", i(2, "x"), i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 2)), ite1, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 10)), ite1, 3)

    // Check ite2
    // already-merged path is EET, which is the second argument, so use the existing virtualPathStore as the second argument as well
    iteCreator = new SymbolicITECreator(newVirtualPathStore)
    val ite2 = iteCreator.createITE(pred1, "T", "EET", i(1, "x"), ite1, "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 2)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 3)), ite2, 3)
  }

  test("Simulate adding T, adding ET, merging, adding EET, merging with ET") {
    val i0 = input(0)
    val pred1 = RelationalExpression(i0, IntEqual, 1)
    val pred2 = RelationalExpression(i0, IntEqual, 2)
    val pred3 = RelationalExpression(i0, IntEqual, 3)

    // Check ite1
    val ite1 = iteCreator.createITE(pred1, "T", "ET", i(1, "x"), i(2, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite1, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 2)), ite1, 2)

    // Check ite2
    iteCreator = newIteCreator
    val ite2 = newIteCreator.createITE(pred2, "ET", "EET", ite1, i(3, "x"), "x")
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 1)), ite2, 1)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 2)), ite2, 2)
    CheckITEExpressionAsserter.checkITEExpression(List((i0, 3)), ite2, 3)
  }
}


