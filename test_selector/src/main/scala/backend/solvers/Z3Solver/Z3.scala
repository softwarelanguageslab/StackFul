package backend.solvers.Z3Solver

/**
  * *****************************************************************************
  * Copyright (c) 2016, KAIST.
  * All rights reserved.
  *
  * Use is subject to license terms.
  *
  * This distribution may include materials developed by third parties.
  * ****************************************************************************
  */
/**
  * *****************************************************************************
  * Copyright (c) 2021, Maarten Vandercammen, VUB.
  * All rights reserved.
  *
  * Use is subject to license terms.
  *
  * This distribution may include materials developed by third parties.
  * ****************************************************************************
  */

import backend.PathConstraint
import backend.expression._
import backend.logging.Logger
import backend.metrics.MergingMetricsKeeper
import backend.solvers._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.util.timer
import com.microsoft.z3.{Solver, _}

import scala.annotation.tailrec
import scala.collection.mutable.{Map => MMap}

object Z3 extends ConstraintSolver[BasicConstraint] {

  override type SolverResult = Z3Result
  type InputMap = MMap[SymbolicInput, Expr]
  private val nrOfEventsInput = "t_n"
  def solveTuples(pcElements: PathConstraint[BasicConstraint], processesInfo: List[Int]): Z3Result = {
    val constraints = pcElements.map(_.toConstraint)
    solve(constraints, processesInfo)
  }

  def solve(constraints: List[BasicConstraint], processesInfo: List[Int]): Z3Result = {
    Logger.v(s"Z3.solve, constraints = $constraints")
    if (constraints.isEmpty) {
      Satisfiable(Map())
    } else if (constraints.exists({
      case BranchConstraint(SymbolicBool(false, _), _) => true
      case _ => false
    })) {
      Unsatisfiable
    } else {
      try {
        Z3Solving.solve(constraints, processesInfo)
      } catch {
        case ex: Z3Exception =>
          println("TEST CASE FAILED: " + ex.getMessage)
          println("Stack trace: ")
          ex.printStackTrace(System.out)
          SomeZ3Error(ex)
        case TestFailedException =>
          println(s"Error solving constraints $constraints")
          SomeZ3Error(TestFailedException)
        case ex: Exception =>
          ex.printStackTrace()
          println("Unknown Exception: " + ex.getMessage)
          SomeZ3Error(ex)
      }
    }
  }

  def solve(expression: BooleanExpression, processesInfo: List[Int]): Z3Result = {
    Logger.v(s"Z3.solve, expression = $expression")
    try {
      Z3Solving.solve(expression, processesInfo)
    } catch {
      case ex: Z3Exception =>
        println("TEST CASE FAILED: " + ex.getMessage)
        println("Stack trace: ")
        ex.printStackTrace(System.out)
        SomeZ3Error(ex)
      case TestFailedException =>
        println(s"Error solving expression $expression")
        SomeZ3Error(TestFailedException)
      case ex: Exception =>
        ex.printStackTrace()
        println("Unknown Exception: " + ex.getMessage)
        SomeZ3Error(ex)
    }
  }

  private[Z3Solver] def asArithmeticalExp(exp: SymbolicExpression): ArithmeticalExpression = exp match {
    case arithmeticalExp: ArithmeticalExpression => arithmeticalExp
    case _ => throw UnexpectedExpressionType(exp, "ArithmeticalExpression")
  }
  private[Z3Solver] def asBooleanExp(exp: SymbolicExpression): BooleanExpression = exp match {
    case booleanExp: BooleanExpression => booleanExp
    case _ => throw UnexpectedExpressionType(exp, "BooleanExpression")
  }
  private[Z3Solver] def asStringExp(exp: SymbolicExpression): StringExpression = exp match {
    case stringExp: StringExpression => stringExp
    case _ => throw UnexpectedExpressionType(exp, "StringExpression")
  }

  class Z3Solving(val solver: Solver, val ctx: Context, val exprMap: InputMap, processesInfo: List[Int]) {

    private val symbolicIntVariablesDefined: Map[SymbolicIntVariable, IntExpr] = Map()
    private var id: Int = 0

    private def findModel(): Z3Result = {
      val (solverResult, solvingTime) = timer(solver.check)
      MergingMetricsKeeper.addSolvingTime(solvingTime)
      if (solverResult == Status.SATISFIABLE) {
        val model: Model = solver.getModel
        var result = Map[SymbolicInput, ComputedValue]()
        exprMap.foreach({
          case (someInput, expr) =>
            val sol = model.getConstInterp(expr)
            someInput match {
              case _: SymbolicInputBool =>
                sol.toString match {
                  case "true" => result += someInput -> ComputedBool(true)
                  case "false" => result += someInput -> ComputedBool(false)
                }
              case _: SymbolicInputFloat =>
                if (sol != null) {
                  result += someInput -> ComputedFloat(sol.toString.toFloat)
                }
              case _: SymbolicInputInt =>
                if (sol != null) {
                  result += someInput -> ComputedInt(sol.toString.toInt)
                }
              case _: SymbolicInputString =>
                if (sol != null) {
                  val solvedString = sol.toString
                  val sanitizedString = Z3StringSanitizer.sanitizeString(solvedString)
                  result += someInput -> ComputedString(sanitizedString)
                }
              case e: SymbolicInputEvent =>
                if (sol != null) {
                  result += e -> ComputedEvent(e.eventId, 1, sol.toString.toInt) // TODO: do not hardcode processId
                }
            }
        })
        Satisfiable(result)
      } else {
        Unsatisfiable
      }
    }

    def constraintSolver(conslist: List[Constraint]): Z3Result = {
      for (constraint <- conslist) {
        addConstraint(constraint)
      }
      findModel()
    }

    def constraintSolver(expression: BooleanExpression): Z3Result = {
      addSymbolicExp(expression)
      findModel()
    }

    private[Z3Solver] def symbolicIntVariableEncountered(variable: SymbolicIntVariable): IntExpr = {
      symbolicIntVariablesDefined.get(variable) match {
        case Some(existing) => existing
        case None =>
          val newIntExpr = addSymbolicInputInt(variable.variable)
          //          val definition = RelationalSymbolicExpression(variable.variable, IntEqual, variable.value)
          //          val boolExpr = expToBoolExpr(definition)
          //          symbolicIntVariablesDefined += (variable -> (newIntExpr, boolExpr))
          //          solver.add(boolExpr)
          //          (newIntExpr, boolExpr)
          newIntExpr
      }
    }

    private[Z3Solver] def addSymbolicInputBool(b: SymbolicInputBool): BoolExpr = {
      exprMap.getOrElseUpdate(b.dropIdentifier, ctx.mkBoolConst(b.toString)).asInstanceOf[BoolExpr]
    }

    private[Z3Solver] def addSymbolicInputInt(i: SymbolicInputInt): IntExpr = {
      exprMap.getOrElseUpdate(i.dropIdentifier, ctx.mkIntConst(i.toString)).asInstanceOf[IntExpr]
    }

    private[Z3Solver] def addSymbolicInputString(s: SymbolicInputString): SeqExpr = {
      exprMap.getOrElseUpdate(s.dropIdentifier, ctx.mkConst(s.toString, ctx.getStringSort)).asInstanceOf[SeqExpr]
    }

    private[Z3Solver] def addSymbolicInputEvent(e: SymbolicInputEvent): IntExpr = {
      val maxTid = e.id match {
        case RegularId(_, pid) => processesInfo(pid)
      }
      val tid = ctx.mkIntConst(e.toString)
      solver.add(ctx.mkLt(tid, ctx.mkInt(maxTid)))
      solver.add(ctx.mkGe(tid, ctx.mkInt(0)))
      exprMap.getOrElseUpdate(e.dropIdentifier, tid).asInstanceOf[IntExpr]
    }

    private[Z3Solver] def makeTempId: String = {
      id += 1
      "__temp__" + id
    }

    private def toIntExpr(arithExpr: ArithExpr): IntExpr = {
      arithExpr match {
        case expr: IntExpr => expr
        case _ =>
          val variable = ctx.mkIntConst(makeTempId)
          val equality = ctx.mkEq(arithExpr, variable)
          solver.add(equality)
          variable
      }
    }

    // Represent conversion as a stack machine
    private type ExprStack = List[Expr]
    var exprStack: ExprStack = Nil
    private def pushExpr(expr: Expr): Unit = {
      exprStack ::= expr
    }
    private def popExpr(): Expr = exprStack.headOption match {
      case None =>
        throw new Exception("Should not happen")
      case Some(expr) =>
        exprStack = exprStack.tail
        expr
    }

    private def pushOperation(expr: Expr): List[ConversionOperation] = {
      pushExpr(expr)
      Nil
    }

    sealed trait ConversionOperation {
      def operate(): List[ConversionOperation]
    }
    case class LogicalBinaryConversionOperation(op: LogicalBinaryOperator) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        // Reverse order: right -> left
        val rightExpr = popExpr().asInstanceOf[BoolExpr]
        val leftExpr = popExpr().asInstanceOf[BoolExpr]
        val expr = op match {
          case LogicalAnd => ctx.mkAnd(leftExpr, rightExpr)
          case LogicalOr => ctx.mkOr(leftExpr, rightExpr)
        }
        pushOperation(expr)
      }
    }
    case class LogicalUnaryConversionOperation(op: LogicalUnaryOperator) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        val arg = popExpr().asInstanceOf[BoolExpr]
        val expr = op match {
          case LogicalNot => ctx.mkNot(arg)
        }
        pushOperation(expr)
      }
    }

    case class ArithmeticalVariadicOperationInstruction(op: IntegerArithmeticalOperator, arity: Int) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        val args = 1.to(arity).map(_ => popExpr().asInstanceOf[ArithExpr]).reverse
        val expr: Expr = op match {
          case IntInverse =>
            assert(arity == 1)
            ctx.mkUnaryMinus(args.head)
          case IntPlus => ctx.mkAdd(args: _*)
          case IntMinus => ctx.mkSub(args: _*)
          case IntTimes => ctx.mkMul(args: _*)
          case IntDiv =>
            assert(arity >= 2) // TODO Refactor ArithmeticalSymbolicExpression so there are separate classes for variadic expressions and fixed-size expressions
            ctx.mkNot(ctx.mkEq(args(1), ctx.mkInt(0))) /* Make sure the divisor does not equal 0 */
            ctx.mkDiv(args.head, args(1))
          case IntModulo =>
            assert(arity >= 2) // TODO Refactor ArithmeticalSymbolicExpression so there are separate classes for variadic expressions and fixed-size expressions
            ctx.mkNot(ctx.mkEq(args(1), ctx.mkInt(0)))
            /* Make sure the divisor does not equal 0 */
            val leftVarExpr = ctx.mkIntConst(makeTempId)
            val leftEq = ctx.mkEq(leftVarExpr, args.head)
            val rightVarExpr = ctx.mkIntConst(makeTempId)
            val rightEq = ctx.mkEq(rightVarExpr, args(1))
            solver.add(leftEq)
            solver.add(rightEq)
            ctx.mkMod(leftVarExpr, rightVarExpr)
        }
        pushOperation(expr)
      }
    }

    case class StringOperationProducesIntInstruction(op: StringOperator, arity: Int) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        val args = 1.to(arity).map(_ => popExpr()).reverse
        val expr = op match {
          case StringLength =>
            val stringExpr = args.head.asInstanceOf[SeqExpr]
            ctx.mkLength(stringExpr)
          case StringIndexOf =>
            val stringExpr = args.head.asInstanceOf[SeqExpr]
            val subExpr = args(1).asInstanceOf[SeqExpr]
            // StringIndexOf-expressions can have either two or three arguments: default value of the third argument is 0
            val offsetExpr = if (arity >= 3) args(2).asInstanceOf[ArithExpr] else ctx.mkInt(0)
            ctx.mkIndexOf(stringExpr, subExpr, offsetExpr)
        }
        pushOperation(expr)
      }
    }

    case class StringOperationProducesStringConversion(op: StringOperator, arity: Int) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        val args = 1.to(arity).map(_ => popExpr()).reverse
        val expr = op match {
          case StringAppend => ctx.mkConcat(args.map(arg => arg.asInstanceOf[SeqExpr]): _*)
          case StringAt =>
            assert(arity == 2)
            val stringExpr = args.head.asInstanceOf[SeqExpr]
            val idxExpr = toIntExpr(args(1).asInstanceOf[ArithExpr])
            ctx.mkAt(stringExpr, idxExpr)
          case StringReplace =>
            val stringExpr = args.head.asInstanceOf[SeqExpr]
            val sourceExpr = args(1).asInstanceOf[SeqExpr]
            val destinationExpr = args(2).asInstanceOf[SeqExpr]
            ctx.mkReplace(stringExpr, sourceExpr, destinationExpr)
          case StringGetSubstring =>
            assert(arity == 3)
            val stringExpr = args.head.asInstanceOf[SeqExpr]
            val startExpr = toIntExpr(args(1).asInstanceOf[ArithExpr])
            val lengthExpr = toIntExpr(args(2).asInstanceOf[ArithExpr])
            ctx.mkExtract(stringExpr, startExpr, lengthExpr)
        }
        pushOperation(expr)
      }
    }

    case object SymbolicITEConversionOperation extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        // Pop in reverse order: else -> then -> pred
        val elseExpr = popExpr()
        val thenExpr = popExpr()
        val predExpr = popExpr().asInstanceOf[BoolExpr]

        // type check
        (thenExpr, elseExpr) match {
          case (_: ArithExpr, _: ArithExpr) =>
          case (_: SeqExpr, _: SeqExpr) =>
          case (_: BoolExpr, _: BoolExpr) =>
          case _ => throw new Exception(s"Types do not match: ITE then exp ${thenExpr} does not match with else exp ${elseExpr}")
        }

        val iteExpr = ctx.mkITE(predExpr, thenExpr, elseExpr)
        pushOperation(iteExpr)
      }
    }

    case class RelationalConversion(op: BinaryRelationalOperator) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = {
        // Reverse order: right -> left
        val rightExpr = popExpr()
        val leftExpr = popExpr()
        op match {
          case IntEqual | IntNonEqual =>
            val expr = if (op == IntEqual) {
              ctx.mkEq(leftExpr, rightExpr)
            } else {
              ctx.mkNot(ctx.mkEq(leftExpr, rightExpr))
            }
            pushOperation(expr)
          case _: IntegerRelationalOperator =>
            val castedLeft = leftExpr.asInstanceOf[ArithExpr]
            val castedRight = rightExpr.asInstanceOf[ArithExpr]
            val expr = op match {
              case IntLessThan => ctx.mkLt(castedLeft, castedRight)
              case IntLessThanEqual => ctx.mkLe(castedLeft, castedRight)
              case IntGreaterThan => ctx.mkGt(castedLeft, castedRight)
              case IntGreaterThanEqual => ctx.mkGe(castedLeft, castedRight)
            }
            pushOperation(expr)
          case booleanOp: BooleanRelationalOperator =>
            val castedLeft = leftExpr.asInstanceOf[BoolExpr]
            val castedRight = rightExpr.asInstanceOf[BoolExpr]
            val expr = booleanOp match {
              case BooleanEqual => ctx.mkEq(castedLeft, castedRight)
            }
            pushOperation(expr)
          case stringOp: StringRelationalOperator =>
            val castedLeft = leftExpr.asInstanceOf[SeqExpr]
            val castedRight = rightExpr.asInstanceOf[SeqExpr]
            val expr = stringOp match {
              case StringEqual => ctx.mkEq(leftExpr, rightExpr) // Does not need to be casted to SeqExpr
              case StringPrefixOf => ctx.mkPrefixOf(castedLeft, castedRight)
              case StringIsSubstring => ctx.mkContains(castedLeft, castedRight)
              case StringSuffixOf => ctx.mkSuffixOf(castedLeft, castedRight)
            }
            pushOperation(expr)
        }
      }
    }

    case class TopLevelConversionOperation(exp: SymbolicExpression) extends ConversionOperation {
      override def operate(): List[ConversionOperation] = exp match {
        case SymbolicBool(b, _) => pushOperation(ctx.mkBool(b))
        case SymbolicInt(i, _) => pushOperation(ctx.mkInt(i))
        case SymbolicString(s, _) => pushOperation(ctx.mkString(s))

        case b: SymbolicInputBool => pushOperation(addSymbolicInputBool(b))
        case i: SymbolicInputInt => pushOperation(addSymbolicInputInt(i))
        case s: SymbolicInputString => pushOperation(addSymbolicInputString(s))

        case v: SymbolicIntVariable => pushOperation(symbolicIntVariableEncountered(v))

        case ArithmeticalVariadicOperationExpression(op, exps, _) =>
          val arity = exps.length
          exps.map(TopLevelConversionOperation) :+ ArithmeticalVariadicOperationInstruction(op, arity)
        case exp: StringOperationProducesIntExpression =>
          val arity = exp.exps.length
          exp.exps.map(TopLevelConversionOperation) :+ StringOperationProducesIntInstruction(exp.op, arity)
        case exp: StringOperationProducesStringExpression =>
          val arity = exp.exps.length
          exp.exps.map(TopLevelConversionOperation) :+ StringOperationProducesStringConversion(exp.op, arity)

        case LogicalBinaryExpression(left, op, right, _) =>
          val leftConversion = TopLevelConversionOperation(left)
          val rightConversion = TopLevelConversionOperation(right)
          val finalConversion = LogicalBinaryConversionOperation(op)
          List(leftConversion, rightConversion, finalConversion)
        case LogicalUnaryExpression(op, exp, _) =>
          val argConversion = TopLevelConversionOperation(exp)
          val finalConversion = LogicalUnaryConversionOperation(op)
          List(argConversion, finalConversion)

        case RelationalExpression(left, op, right, _) =>
          val leftConversion = TopLevelConversionOperation(left)
          val rightConversion = TopLevelConversionOperation(right)
          val finalConversion = RelationalConversion(op)
          List(leftConversion, rightConversion, finalConversion)

        case SymbolicEventChosenExpression(input, op, tid, _) =>
          val inputExpr = addSymbolicInputEvent(input)
          val tidExpr = ctx.mkInt(tid)
          val expr = op match {
            case EventChosenOperator => ctx.mkEq(inputExpr, tidExpr)
            case EventNotChosenOperator => ctx.mkNot(ctx.mkEq(inputExpr, tidExpr))
          }
          pushOperation(expr)

        case ite: SymbolicITEExpression[_, _] =>
          val predConversion = TopLevelConversionOperation(ite.predExp)
          val thenExpConversion = TopLevelConversionOperation(ite.thenExp)
          val elseExpConversion = TopLevelConversionOperation(ite.elseExp)
          val finalConversion = SymbolicITEConversionOperation
          List(predConversion, thenExpConversion, elseExpConversion, finalConversion)
      }
    }

    @tailrec
    private def iterativeExpToExpr(instructions: List[ConversionOperation]): Expr = instructions.headOption match {
      case Some(operation) =>
        val newInstructions = operation.operate()
        iterativeExpToExpr(newInstructions ++ instructions.tail)
      case None =>
        assert(exprStack.length == 1)
        popExpr()
    }

    private[Z3Solver] def expToExpr(exp: SymbolicExpression): Expr = {
      val initialConversion = TopLevelConversionOperation(exp)
      iterativeExpToExpr(List(initialConversion))
    }

    private[Z3Solver] def addSymbolicExp(symbolicExp: BooleanExpression): Unit = {
      symbolicExp match {
        case exp: RelationalExpression =>
          val expr = expToExpr(exp)
          solver.add(expr.asInstanceOf[BoolExpr])
        case exp: LogicalBinaryExpression =>
          val expr = expToExpr(exp)
          solver.add(expr.asInstanceOf[BoolExpr])
        case exp: LogicalUnaryExpression =>
          val expr = expToExpr(exp)
          solver.add(expr.asInstanceOf[BoolExpr])
        case exp: SymbolicEventChosenExpression =>
          val expr = expToExpr(exp)
          solver.add(expr.asInstanceOf[BoolExpr])
        case _: EventChosen => // Don't need to add this constraint
        case SymbolicBool(true, _) => // Don't need to add this constraint
        case SymbolicBool(false, _) => solver.add(ctx.mkBool(false))
        case SymbolicITEExpression(predExp, thenExp, elseExp, _) =>
          assert(thenExp.isInstanceOf[ConvertibleToBooleanExpression])
          assert(elseExp.isInstanceOf[ConvertibleToBooleanExpression])
          val predExpr = expToExpr(predExp)
          val thenExpr = expToExpr(thenExp)
          val elseExpr = expToExpr(elseExp)
          val iteExpr = ctx.mkITE(predExpr.asInstanceOf[BoolExpr], thenExpr, elseExpr).asInstanceOf[BoolExpr]
          solver.add(iteExpr)
      }
    }

    private[Z3Solver] def addConstraint(constraint: Constraint): Unit = constraint match {
      case bc: BranchConstraint => addSymbolicExp(bc.exp)
    }

  }

  case object TestFailedException extends Exception("Check FAILED")

  object Z3Solving {
    def solve(constraints: List[BasicConstraint], processesInfo: List[Int]): Z3Result = {
      val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
      cfg.put("model", "true")
      val ctx: Context = new Context(cfg)
      val solver: Solver = ctx.mkSolver
      val exprMap: InputMap = scala.collection.mutable.HashMap()
      val solving = new Z3Solving(solver, ctx, exprMap, processesInfo)
      val result = solving.constraintSolver(constraints)
      ctx.close()
      result
    }

    def solve(expression: BooleanExpression, processesInfo: List[Int]): Z3Result = {
      val cfg: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
      cfg.put("model", "true")
      val ctx: Context = new Context(cfg)
      val solver: Solver = ctx.mkSolver
      val exprMap: InputMap = scala.collection.mutable.HashMap()
      val solving = new Z3Solving(solver, ctx, exprMap, processesInfo)
      val result = solving.constraintSolver(expression)
      ctx.close()
      result
    }
  }
}
