package backend.solvers.Z3Solver

import backend.expression._
import backend.solvers.Z3Solver.Z3.{InputMap, Z3Solving}
import backend.solvers._
import backend.tree.constraints.basic_constraints
import backend.tree.constraints.basic_constraints.BranchConstraint
import org.scalatest.funsuite.AnyFunSuite
import com.microsoft.z3.{Solver, _}

import scala.collection.mutable.{Map => MMap}

object Z3SolvingTest extends AnyFunSuite {

  def makeZ3Solving: Z3.Z3Solving = {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    val ctx: Context = new Context(cfg)
    cfg.put("model", "true")
    val solver: Solver = ctx.mkSolver
    val exprMap: InputMap = scala.collection.mutable.HashMap()
    new Z3Solving(solver, ctx, exprMap, Nil)
  }

  test("(ite #t #t #f) produces a BoolExpr") {
    val ite = SymbolicITEExpression(
      SymbolicBool(true),
      SymbolicBool(true),
      SymbolicBool(false))
    makeZ3Solving.expToExpr(ite) match {
      case _: BoolExpr => assert(true)
      case other => assert(false, s"Expected a BoolExpr, but got a $other instead")
    }
  }

  test("(ite #t 1 0) produces an IntExpr") {
    val ite = SymbolicITEExpression(
      SymbolicBool(true),
      SymbolicInt(1),
      SymbolicInt(0))
    makeZ3Solving.expToExpr(ite) match {
      case _: ArithExpr => assert(true)
      case other => assert(false, s"Expected an ArithExpr, but got a $other instead")
    }
  }

  test("(ite #t \"abc\" \"def\") produces a SeqExpr") {
    val ite = SymbolicITEExpression(
      SymbolicBool(true),
      SymbolicString("abc"),
      SymbolicString("def"))
    makeZ3Solving.expToExpr(ite) match {
      case _: SeqExpr => assert(true)
      case other => assert(false, s"Expected a SeqExpr, but got a $other instead")
    }
  }
}

class Z3Test extends AnyFunSuite with SolverTest {

  private def checkSingleExpressionWithInput(
    input: SymbolicInputInt,
    exp: BooleanExpression,
    expectedValue: Int
  ): Unit = {
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.contains(input))
    assertComputedValueMatches(solution(input), expectedValue)
  }

  private def checkSingleExpressionWithInput(
    input: SymbolicInputInt,
    exp: BooleanExpression,
    valuePred: Int => Boolean
  ): Unit = {
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.contains(input))
    assertComputedValueMatches(solution(input), valuePred)
  }

  private def checkSingleExpressionWithInput(
    input: SymbolicInputString,
    exp: BooleanExpression,
    expectedValue: String
  ): Unit = {
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.contains(input))
    assertComputedValueMatches(solution(input), expectedValue)
  }

  import scala.language.implicitConversions

  implicit def intToSymbolicInt(int: Int): SymbolicInt = SymbolicInt(int)
  implicit def stringToSymbolicString(string: String): SymbolicString =
    SymbolicString(string)

  test("(= i0 10) is satisfiable") {
    val i0 = SymbolicInputInt(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(i0, IntEqual, 10)
    checkSingleExpressionWithInput(i0, b, 10)
  }

  test("(= i0 (% 10 6))") {
    val i0 = SymbolicInputInt(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(
      i0,
      IntEqual,
      ArithmeticalVariadicOperationExpression(
        IntModulo,
        List(
          SymbolicInt(10),
          SymbolicInt(6))))
    checkSingleExpressionWithInput(i0, b, 4)
  }

  test("""(= s0 "abc") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression =
      RelationalExpression(s0, StringEqual, "abc")
    checkSingleExpressionWithInput(s0, b, "abc")
  }

  test("""(= s0 "\"abc") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression =
      RelationalExpression(s0, StringEqual, "\"abc")
    checkSingleExpressionWithInput(s0, b, "\"abc")
  }

  test("""(= (string_append s0 "bc") "\"abc") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(
      StringOperationProducesStringExpression(StringAppend, List(s0, "bc")),
      StringEqual,
      "\"abc")
    checkSingleExpressionWithInput(s0, b, "\"a")
  }

  test("""(= (string_replace "abcd" "bc" s0) "afood") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(
      StringOperationProducesStringExpression(StringReplace, List("abcd", "bc", s0)),
      StringEqual,
      "afood")
    checkSingleExpressionWithInput(s0, b, "foo")
  }

  test("""(= (string_replace s0 "ab" "a") "aa") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(
      StringOperationProducesStringExpression(StringReplace, List(s0, "ab", "a")),
      StringEqual,
      "aa")
    checkSingleExpressionWithInput(s0, b, "aba")
  }

  test("""(= (string_replace s0 "ab" "a") "abba") is satisfiable""") {
    val s0 = SymbolicInputString(RegularId(0, 1))
    val b: BooleanExpression = RelationalExpression(
      StringOperationProducesStringExpression(StringReplace, List(s0, "ab", "a")),
      StringEqual,
      "abba")
    checkSingleExpressionWithInput(s0, b, "abbba")
  }

  test("(Experimenting with floats) (= r0 3.1) is satisfiable") {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    cfg.put("model", "true")
    val ctx: Context = new Context(cfg)
    val solver: Solver = ctx.mkSolver
    val realExpr = ctx.mkRealConst("r0")
    val expr = ctx.mkEq(realExpr, ctx.mkReal("3.141592654"))
    val realExpr2 = ctx.mkRealConst("r1")
    val expr2 = ctx.mkEq(ctx.mkDiv(realExpr2, ctx.mkReal(10)), ctx.mkReal("5.12"))
    var exprMap: Map[String, Expr] = Map()
    exprMap += "r0" -> realExpr
    exprMap += "r1" -> realExpr2
    solver.add(expr)
    solver.add(expr2)

    if (Status.SATISFIABLE eq solver.check) {
      val model: Model = solver.getModel
      exprMap.foreach({
        case (s, expr) =>
          println(s"Solution for $s is ${model.getConstInterp(expr).toString}")
      })
    } else {
      assert(false, "Floating-point constraint should be satisfiable")
      Unsatisfiable
    }
  }

  test("(Experimenting with strings) (= (substring s0 \"abcd\") is satisfiable") {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    cfg.put("model", "true")
    val ctx: Context = new Context(cfg)
    var exprMap: MMap[String, Expr] = scala.collection.mutable.HashMap[String, Expr]()
    val solver: Solver = ctx.mkSolver
    val stringConst = ctx.mkConst("s0", ctx.getStringSort)
    exprMap += "s0" -> stringConst
    val expr = ctx.mkContains(stringConst.asInstanceOf[SeqExpr], ctx.mkString("abc"))
    solver.add(expr)

    if (Status.SATISFIABLE eq solver.check) {
      val model: Model = solver.getModel
      exprMap.foreach({
        case (s, expr) =>
          val result = model.getConstInterp(expr)
          println(s"Value computed for $s is $result")
      })
    } else {
      assert(false, "String constraint should be satisfiable")
      Unsatisfiable
    }
  }

  test("(Experimenting with strings) (= s0 (at \"abcd\" 1)) is satisfiable") {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    cfg.put("model", "true")
    val ctx: Context = new Context(cfg)
    var exprMap: MMap[String, Expr] = scala.collection.mutable.HashMap[String, Expr]()
    val solver: Solver = ctx.mkSolver
    val stringConst = ctx.mkConst("s0", ctx.getStringSort)
    exprMap += "s0" -> stringConst
    val expr = ctx.mkEq(ctx.mkAt(ctx.mkString("abcd"), ctx.mkInt(1)), stringConst)
    solver.add(expr)

    if (Status.SATISFIABLE eq solver.check) {
      val model: Model = solver.getModel
      exprMap.foreach({
        case (s, expr) =>
          val result = model.getConstInterp(expr)
          println("(Experimenting with strings) (= s0 (at \\\"abcd\\\" 1)) is satisfiable\"")
          println(s"Value computed for $s is $result")
      })
    } else {
      assert(false, "String constraint should be satisfiable")
      Unsatisfiable
    }
  }

  test("(Experimenting with strings) (= i0 (index_of \"abcd\" \"b\")) is satisfiable") {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    cfg.put("model", "true")
    val ctx: Context = new Context(cfg)
    var exprMap: MMap[String, Expr] = scala.collection.mutable.HashMap[String, Expr]()
    val solver: Solver = ctx.mkSolver
    val intConst = ctx.mkIntConst("i0")
    exprMap += "s0" -> intConst
    val expr =
      ctx.mkEq(ctx.mkIndexOf(ctx.mkString("abcd"), ctx.mkString("b"), ctx.mkInt(-5)), intConst)
    solver.add(expr)

    if (Status.SATISFIABLE eq solver.check) {
      val model: Model = solver.getModel
      exprMap.foreach({
        case (s, expr) =>
          val result = model.getConstInterp(expr)
          println("(Experimenting with strings) (= i0 (index_of \"abcd\" \"b\")) is satisfiable")
          println(s"Value computed for $s is $result")
      })
    } else {
      assert(false, "String constraint should be satisfiable")
      Unsatisfiable
    }
  }

  test("(Experimenting with strings) (= (index_of s0 \"A\") 5) is satisfiable") {
    val exp = RelationalExpression(
      StringOperationProducesIntExpression(
        StringIndexOf,
        List(
          SymbolicInputString(RegularId(0, 0)),
          SymbolicString("A"))),
      IntEqual,
      SymbolicInt(5)
    )
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    println(solution)
  }

  test(
    "(Experimenting with strings) (= (string_get_substring \"abcdefghij\" 2 3) \"cde\") is satisfiable") {
    val exp = RelationalExpression(
      StringOperationProducesStringExpression(
        StringGetSubstring,
        List(
          SymbolicString("abcdefghij"),
          SymbolicInt(2),
          SymbolicInt(3))),
      StringEqual,
      SymbolicInputString(RegularId(0, 0))
    )
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.size == 1)
    assert(solution.head._2 == ComputedString("cde"))
  }

  test("(Experimenting with strings) (prefix_of \"a\" \"abcd\")) is satisfiable") {
    val exp = RelationalExpression(SymbolicString("a"), StringPrefixOf, SymbolicString("abcd"))
    Z3SatisfiabilityAsserter.assertSatisfiable(exp)
  }

  private def newStringConst(ctx: Context, exprMap: MMap[String, Expr], name: String): SeqExpr = {
    val stringConst = ctx.mkConst(name, ctx.getStringSort).asInstanceOf[SeqExpr]
    exprMap += name -> stringConst
    stringConst
  }

  private def addReExpr(
    ctx: Context,
    solver: Solver,
    stringConst: SeqExpr,
    reExpr: ReExpr
  ): Expr = {
    val e = ctx.mkInRe(stringConst, reExpr)
    solver.add(e)
    e
  }

  test("(Experimenting with regexes)") {
    val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
    cfg.put("model", "true")
    val ctx: Context = new Context(cfg)
    val exprMap: MMap[String, Expr] = scala.collection.mutable.HashMap[String, Expr]()
    val solver: Solver = ctx.mkSolver
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s1"),
      ctx.mkRange(ctx.mkString("a"), ctx.mkString("e")))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s2"),
      ctx.mkConcat(ctx.mkToRe(ctx.mkString("abc")), ctx.mkToRe(ctx.mkString("def"))))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s3"),
      ctx.mkStar(ctx.mkToRe(ctx.mkString("abc"))))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s4"),
      ctx.mkPlus(ctx.mkToRe(ctx.mkString("abc"))))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s5"),
      ctx.mkOption(ctx.mkToRe(ctx.mkString("abc"))))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s6"),
      ctx.mkLoop(ctx.mkToRe(ctx.mkString("abc")), 2, 3))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s7"),
      ctx.mkUnion(ctx.mkToRe(ctx.mkString("abc")), ctx.mkToRe(ctx.mkString("def"))))
    addReExpr(
      ctx,
      solver,
      newStringConst(ctx, exprMap, "s8"),
      ctx.mkIntersect(ctx.mkToRe(ctx.mkString("abc")), ctx.mkToRe(ctx.mkString("abc")))) // Empty string

    //    val expr = ctx.mkEq(ctx.mkAt(ctx.mkString("abcd"), ctx.mkInt(1)), stringConst)
    //    ctx.mkToRe()        Done
    //    ctx.mkInRe()        Done
    //    allchar?            Not in this version
    //    nostr?              Not in this version
    //    ctx.mkRange()       Done
    //    ctx.mkConcat()      Done
    //    ctx.mkStar()        Done
    //    ctx.mkPlus()        Done
    //    ctx.mkOption()      Done
    //    ctx.mkLoop()        Done
    //    ctx.mkUnion()       Done
    //    ctx.mkIntersect()   Done
    if (Status.SATISFIABLE eq solver.check) {
      val model: Model = solver.getModel
      exprMap.foreach({
        case (s, expr) =>
          val result = model.getConstInterp(expr)
          println(s"Regex: value computed for $s is $result")
      })
    } else {
      assert(false, "String constraints should be satisfiable")
      Unsatisfiable
    }
  }

  test("test [s0 != \"abc\" && (string_prefix s0 \"Saturday\"") {
    val s0 = SymbolicInputString(RegularId(0, 0))
    val bc1 = basic_constraints.BranchConstraint(
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          s0,
          StringEqual,
          SymbolicString("abc"))), Nil)
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicString("Saturday"),
        StringPrefixOf,
        s0), Nil)
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(List(bc1, bc2))
    assert(solution.size == 1)
    println(solution.head._2)
    assert(solution.head._2 match {
      case ComputedString(str) => str.startsWith("Saturday")
      case _ => false
    })
  }

  test("test [s0 != \"abc\" && (string_suffix s0 \"Saturday\"") {
    val s0 = SymbolicInputString(RegularId(0, 0))
    val bc1 = basic_constraints.BranchConstraint(
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          s0,
          StringEqual,
          SymbolicString("abc"))), Nil)
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicString("Saturday"),
        StringSuffixOf,
        s0), Nil)
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(List(bc1, bc2))
    assert(solution.size == 1)
    println(solution.head._2)
    assert(solution.head._2 match {
      case ComputedString(str) => str.endsWith("Saturday")
      case _ => false
    })
  }

  test("test [s0 != (string_suffix s0 \"Saturday\"") {
    val s0 = SymbolicInputString(RegularId(0, 0))
    val bc1 = basic_constraints.BranchConstraint(
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          s0,
          StringEqual,
          SymbolicString("abc"))), Nil)
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicString("Saturday"),
        StringSuffixOf,
        s0), Nil)
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(List(bc1, bc2))
    assert(solution.size == 1)
    println(solution.head._2)
    assert(solution.head._2 match {
      case ComputedString(str) => str.endsWith("Saturday")
      case _ => false
    })
  }

  test("test [(string_get_substring \"Apple, Banana, Kiwi\" 7 6) == s0") {
    val s0 = SymbolicInputString(RegularId(0, 0))
    val exp = RelationalExpression(
      StringOperationProducesStringExpression(
        StringGetSubstring,
        List(
          SymbolicString("Apple, Banana, Kiwi"),
          SymbolicInt(7),
          SymbolicInt(13 - 7))),
      StringEqual,
      s0
    )
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.size == 1)
    println(solution.head._2)
  }

  test("test [(string_at \"Apple\" 1) == s && (s == \"p\")]") {
    val s0 = SymbolicInputString(RegularId(0, 0))
    val bc1 = basic_constraints.BranchConstraint(
      RelationalExpression(
        StringOperationProducesStringExpression(
          StringAt,
          List(
            SymbolicString("Apple"),
            SymbolicInt(1))),
        StringEqual,
        s0), Nil)
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        s0,
        StringEqual,
        SymbolicString("p")), Nil)
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(List(bc1, bc2))
    assert(solution.size == 1)
    assert(solution.head._2 match {
      case ComputedString(s) => s == "p"
      case _ => false
    })
  }

  test("ITE-expression with a true-predicate") {
    val i0 = SymbolicInputInt(RegularId(0, 0))
    val ite = SymbolicITEExpression[ArithmeticalExpression, ArithmeticalExpression](
      RelationalExpression(i0, IntEqual, SymbolicInt(1)),
      SymbolicInt(2),
      SymbolicInt(3)
    )
    val exp = RelationalExpression(ite, IntEqual, SymbolicInt(2))
    checkSingleExpressionWithInput(i0, exp, 1)
  }

  test("ITE-expression with a false-predicate") {
    val i0 = SymbolicInputInt(RegularId(0, 0))
    val ite = SymbolicITEExpression(
      RelationalExpression(i0, IntEqual, SymbolicInt(1)),
      SymbolicInt(2),
      SymbolicInt(3)
    )
    val exp = RelationalExpression(ite, IntEqual, SymbolicInt(3))
    checkSingleExpressionWithInput(i0, exp, _ != 1)
  }

  test("(= (ite (= i0 1) 2 i1) 2)") {
    val i0 = SymbolicInputInt(RegularId(0, 0))
    val i1 = SymbolicInputInt(RegularId(1, 0))
    val ite = SymbolicITEExpression(
      RelationalExpression(i0, IntEqual, SymbolicInt(1)),
      SymbolicInt(2),
      i1)
    val exp = RelationalExpression(ite, IntEqual, SymbolicInt(2))
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    if (solution.contains(i0)) {
      // ite's predicate was true, so it evaluated to 2
      assertComputedValueMatches(solution(i0), 1)
    } else if (solution.contains(i1)) {
      assertComputedValueMatches(solution(i1), 2)
    } else {
      assert(false, "ite expression did not evaluate to 2")
    }
  }

  test("(= (+ 2 (ite (and (>= 0 0) (= i0 10) (= i1 20)) 100 10)) 102)") {
    val i0 = SymbolicInputInt(RegularId(0, 0))
    val i1 = SymbolicInputInt(RegularId(1, 0))
    val _0gte0 = RelationalExpression(SymbolicInt(0), IntGreaterThanEqual, SymbolicInt(0))
    val _i0eq10 = RelationalExpression(i0, IntEqual, SymbolicInt(10))
    val _i1eq20 = RelationalExpression(i1, IntEqual, SymbolicInt(20))
    val ands = LogicalBinaryExpression(_0gte0, LogicalAnd, LogicalBinaryExpression(_i0eq10, LogicalAnd, _i1eq20))
    val ite = SymbolicITEExpression(ands, SymbolicInt(100), SymbolicInt(10))
    val exp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(2), ite)), IntEqual, SymbolicInt(102))
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.contains(i0))
    assert(solution.contains(i1))
    assert(solution(i0) == ComputedInt(10))
    assert(solution(i1) == ComputedInt(20))
  }

  test("(= (+ (ite (= i0 10) 1 2) (ite (!= i1 20) 100 10) (ite (!= i2 30) 8 5) 17)") {
    val i0 = SymbolicInputInt(RegularId(0, 0))
    val i1 = SymbolicInputInt(RegularId(1, 0))
    val i2 = SymbolicInputInt(RegularId(2, 0))
    val _i0eq10 = RelationalExpression(i0, IntEqual, SymbolicInt(10))
    val _i1neq20 = RelationalExpression(i1, IntNonEqual, SymbolicInt(20))
    val _i2neq30 = RelationalExpression(i2, IntNonEqual, SymbolicInt(30))
    val ite1 = SymbolicITEExpression(_i0eq10, SymbolicInt(1), SymbolicInt(2))
    val ite2 = SymbolicITEExpression(_i1neq20, SymbolicInt(100), SymbolicInt(10))
    val ite3 = SymbolicITEExpression(_i2neq30, SymbolicInt(8), SymbolicInt(5))
    val exp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(ite1, ite2, ite3)), IntEqual, SymbolicInt(17))
    val bc: BranchConstraint = basic_constraints.BranchConstraint(exp, Nil)
    val solution = Z3SatisfiabilityAsserter.assertSatisfiable(exp)
    assert(solution.contains(i0))
    assert(solution.contains(i1))
    assert(solution.contains(i2))
    assert(solution(i0) != ComputedInt(10))
    assert(solution(i1) == ComputedInt(20))
    assert(solution(i2) == ComputedInt(30))
  }

  test("test valid function summary") {
    // List(FixedConstraint((input(Input(fid:5, pid:0, id:0, tc:2)) = input(pid:0, i:0)) & (0 < 1) & ! (1 < 1) & ! (input(Input(fid:5, pid:0, id:0, tc:1)) < 7) & ((var:input(Return(fid:5, pid:0, tc:2)), value:3) = 3) | (0 < 1) & ! (1 < 1) & (input(Input(fid:5, pid:0, id:0, tc:1)) < 7) & (input(Input(fid:21, pid:0, id:0, tc:1)) = input(Input(fid:21, pid:0, id:0, tc:2))) & (0 < 1) & ! (1 < 1) & (input(Input(fid:21, pid:0, id:0, tc:1)) > 5) & ((var:input(Return(fid:21, pid:0, tc:1)), value:(input(Input(fid:21, pid:0, id:0, tc:1))) + (5)) = (input(Input(fid:21, pid:0, id:0, tc:1))) + (5)) | (0 < 1) & ! (1 < 1) & ! (input(Input(fid:21, pid:0, id:0, tc:1)) > 5) & ((var:input(Return(fid:21, pid:0, tc:1)), value:2) = 2) & ((var:input(Return(fid:5, pid:0, tc:2)), value:(var:input(Return(fid:21, pid:0, tc:2)), value:(input(Input(fid:21, pid:0, id:0, tc:2))) + (5))) = (var:input(Return(fid:21, pid:0, tc:2)), value:(input(Input(fid:21, pid:0, id:0, tc:2))) + (5)))), (object string_equals object), ((0) + ((var:input(Return(fid:5, pid:0, tc:2)), value:3)) >= 10))
    val lt_0_1 = RelationalExpression(SymbolicInt(0), IntLessThan, SymbolicInt(1))
    val ge_1_1 =
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          SymbolicInt(1),
          IntLessThan,
          SymbolicInt(1)))

    val bc_rllll = lt_0_1
    val bc_rlllr = ge_1_1
    val bc_rlll = LogicalBinaryExpression(bc_rllll, LogicalAnd, bc_rlllr)
    val bc_rll = LogicalBinaryExpression(
      bc_rlll,
      LogicalAnd,
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          SymbolicInputInt(FunctionInputId(5, 0, 0, 1)),
          IntLessThan,
          SymbolicInt(7))
      )
    )
    val bc_rl = LogicalBinaryExpression(
      bc_rll,
      LogicalAnd,
      RelationalExpression(
        SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(5, 2, 0))),
        IntEqual,
        SymbolicInt(3)),
    )

    val bc_rrlll = LogicalBinaryExpression(lt_0_1, LogicalAnd, ge_1_1)
    val bc_rrll = LogicalBinaryExpression(
      bc_rrlll,
      LogicalAnd,
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(5, 0, 0, 1)),
        IntLessThan,
        SymbolicInt(7))
    )

    val bc_rrlrrll = LogicalBinaryExpression(
      LogicalBinaryExpression(lt_0_1, LogicalAnd, ge_1_1),
      LogicalAnd,
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(21, 0, 0, 1)),
        IntGreaterThan,
        SymbolicInt(5)),
    )
    val bc_rrlrrl = LogicalBinaryExpression(
      bc_rrlrrll,
      LogicalAnd,
      RelationalExpression(
        SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(21, 1, 0))),
        IntEqual,
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInputInt(FunctionInputId(21, 0, 0, 1)),
            SymbolicInt(5)))
      )
    )
    val bc_rrlrrrl = LogicalBinaryExpression(
      LogicalBinaryExpression(lt_0_1, LogicalAnd, ge_1_1),
      LogicalAnd,
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          SymbolicInputInt(FunctionInputId(21, 0, 0, 1)),
          IntGreaterThan,
          SymbolicInt(5))
      )
    )
    val bc_rrlrrr = LogicalBinaryExpression(
      bc_rrlrrrl,
      LogicalAnd,
      RelationalExpression(
        SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(21, 1, 0))),
        IntEqual,
        SymbolicInt(2))
    )
    val bc_rrlrr = LogicalBinaryExpression(bc_rrlrrl, LogicalOr, bc_rrlrrr)
    val bc_rrlr = LogicalBinaryExpression(
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(21, 0, 0, 1)),
        IntEqual,
        SymbolicInputInt(FunctionInputId(21, 0, 0, 2))),
      LogicalAnd,
      bc_rrlrr
    )
    val bc_rrl = LogicalBinaryExpression(bc_rrll, LogicalAnd, bc_rrlr)

    val bc_rrr = RelationalExpression(
      SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(5, 2, 0))),
      IntEqual,
      SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(21, 2, 0)))
    )
    val bc_rr = LogicalBinaryExpression(bc_rrl, LogicalAnd, bc_rrr)

    val bc_l =
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(5, 0, 0, 2)),
        IntEqual,
        SymbolicInputInt(RegularId(0, 0)))
    val bc_r = LogicalBinaryExpression(bc_rl, LogicalOr, bc_rr)

    val bc1 = BranchConstraint(LogicalBinaryExpression(bc_l, LogicalAnd, bc_r), Nil)
    // second (branch) constraint is a string comparison between "object" and "object". We'll leave this constraint out since it shouldn't affect the correctness of the test
    val bc3 = basic_constraints.BranchConstraint(
      RelationalExpression(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInt(0),
            SymbolicIntVariable(SymbolicInputInt(FunctionReturnId(5, 2, 0))))),
        IntGreaterThanEqual,
        SymbolicInt(10)
      ), Nil)

    val extra_bc1 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(5, 0, 0, 2)),
        IntEqual,
        SymbolicInputInt(FunctionInputId(5, 0, 0, 1))), Nil)
    val extra_bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(FunctionInputId(21, 0, 0, 1)),
        IntEqual,
        SymbolicInputInt(FunctionInputId(5, 0, 0, 1))), Nil)
    val extra_bc3 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(FunctionReturnId(21, 1, 0)),
        IntEqual,
        SymbolicInputInt(FunctionReturnId(21, 2, 0))), Nil)
    val extra_bc4 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(FunctionReturnId(5, 1, 0)),
        IntEqual,
        SymbolicInputInt(FunctionReturnId(5, 2, 0))), Nil)
    val extra_bc5 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(FunctionReturnId(21, 1, 0)),
        IntEqual,
        SymbolicInputInt(FunctionReturnId(5, 1, 0))), Nil)
    val extra_bcs = List(
      extra_bc1,
      extra_bc3) // Minimal constraints needed to get this summary working seem to be extra_bc1 and extra_bc3: link FunctionInputId(5, 0, 0, 2) to FunctionInputId(5, 0, 0, 1) and link SymbolicInputInt(FunctionReturnId(21, 1, 0) to SymbolicInputInt(FunctionReturnId(21, 2, 0)

    val allConstraints = List(bc1, bc3) ++ extra_bcs
    Z3SatisfiabilityAsserter.assertSatisfiable(allConstraints)
  }

}
