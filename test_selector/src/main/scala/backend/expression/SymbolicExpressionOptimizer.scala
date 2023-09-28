package backend.expression

trait SymbolicExpressionOptimizer {

  protected type EitherReduced = Either[SymbolicExpression, Int]
  def optimizeArithExp(exp: ArithmeticalVariadicOperationExpression): SymbolicExpression = {
    eitherToSymbolicExp(tryOptimizeArithExp(exp))
  }
  protected def tryOptimizeArithExp(exp: ArithmeticalVariadicOperationExpression): EitherReduced =
    exp.op match {
      case IntPlus => reduceCommutativeArithExp(exp, _.sum)
      case IntMinus => optimizeMinus(exp.exps)
      case IntTimes => reduceCommutativeArithExp(exp, _.product)
      case IntDiv if exp.exps.size == 2 =>
        /* Integer division expressions are only optimized if the divider is equal to 1. */
        val reduced1 = reduceToInt(exp.exps.head)
        val reduced2 = reduceToInt(exp.exps(1))
        (reduced1, reduced2) match {
          /* If the first argument is dividable by the second, the division can be safely optimised. */
          case (Right(int1), Right(int2)) if (int2 >= 1) && (int1 % int2 == 0) => Right(int1 / int2)
          case (Right(int1), Right(int2)) =>
            Left(
              exp.copy(
                exps = List(SymbolicInt(int1), SymbolicInt(int2))))
          case (Right(int1), Left(arg2)) =>
            Left(exp.copy(exps = List(SymbolicInt(int1), arg2)))
          case (Left(arg1), Right(1)) => Left(arg1)
          case (Left(arg1), Right(int2)) =>
            Left(exp.copy(exps = List(arg1, SymbolicInt(int2))))
          case (Left(arg1), Left(arg2)) => Left(exp.copy(exps = List(arg1, arg2)))
        }
      case IntInverse =>
        assert(exp.exps.length == 1)
        reduceToInt(exp.exps.head) match {
          case Right(i) => Right(-i)
          case Left(arg) => Left(exp)
        }
      case _ =>
        /* TODO Optimize other arithmetical operations as well */
        Left(exp)
    }
  /**
    * Attempts to reduce an ArithmeticSymbolicExpression that uses a commutative operator.
    *
    * @param exp
    * @param reduce
    * @return
    */
  protected def reduceCommutativeArithExp(
    exp: ArithmeticalVariadicOperationExpression,
    reduce: List[Int] => Int
  ): EitherReduced = {
    val (ints, args) = splitReduced(exp.exps)
    val reducedInts = reduce(ints)
    val result: EitherReduced = if (args.isEmpty) {
      /* All arguments could be reduced to an integer */
      Right(reducedInts)
    } else if (ints.nonEmpty) {
      /* At least one argument that could not be reduced, but exp can still be optimized by adding together arguments */
      val newArgs = SymbolicInt(reducedInts) :: args
      Left(exp.copy(exps = newArgs))
    } else {
      Left(exp)
    }
    result
  }
  protected def splitReduced(exps: List[SymbolicExpression]): (List[Int], List[SymbolicExpression]) = {
    exps.foldLeft[(List[Int], List[SymbolicExpression])]((Nil, Nil))((acc, exp) =>
      reduceToInt(exp) match {
        case Left(exp) => (acc._1, acc._2 :+ exp)
        case Right(i) => (acc._1 :+ i, acc._2)
      })
  }
  protected def optimizeMinus(args: List[SymbolicExpression]): EitherReduced = args match {
    case Nil => Right(0) /* A minus-expression without args equals the neutral element 0. */
    case List(exp) => reduceToInt(exp)
    case head :: tail =>
      val reducedHead = reduceToInt(head)
      val reducedTail = tail.map(reduceToInt)
      val (nonReducedArgs, constant) =
        reducedTail.foldLeft[(List[SymbolicExpression], Int)]((Nil, 0))((acc, eitherReduced) =>
          eitherReduced match {
            case Left(exp) => (acc._1 :+ exp, acc._2)
            case Right(int) => (acc._1, acc._2 + int)
          })
      reducedHead match {
        case Right(int) =>
          if (nonReducedArgs.isEmpty) {
            /* Every argument of this minus-expression was successfully reduced. */
            Right(int - constant)
          } else {
            val newHead = SymbolicInt(int - constant)
            Left(ArithmeticalVariadicOperationExpression(IntMinus, newHead :: nonReducedArgs))
          }
        case Left(exp) =>
          if (constant == 0) {
            Left(ArithmeticalVariadicOperationExpression(IntMinus, exp :: nonReducedArgs))
          } else {
            Left(
              ArithmeticalVariadicOperationExpression(
                IntMinus,
                exp :: SymbolicInt(constant) :: nonReducedArgs))
          }
      }
  }
  protected def eitherToSymbolicExp(either: EitherReduced): SymbolicExpression = either match {
    case Left(exp) => exp
    case Right(i) => SymbolicInt(i)
  }
  protected def reduceToInt(exp: SymbolicExpression): EitherReduced

}

object SymbolicExpressionOptimizer extends SymbolicExpressionOptimizer {

  def optimizeBoolExp(exp: BooleanExpression): BooleanExpression = exp match {
    case RelationalExpression(left, op, right, _) =>
      val optimizedLeft = left match {
        case left: ArithmeticalVariadicOperationExpression =>
          val optimized = tryOptimizeArithExp(left)
          eitherToSymbolicExp(optimized)
        case _ => left
      }
      val optimizedRight = right match {
        case right: ArithmeticalVariadicOperationExpression =>
          val optimized = tryOptimizeArithExp(right)
          eitherToSymbolicExp(optimized)
        case _ => right
      }
      RelationalExpression(optimizedLeft, op, optimizedRight)
    case exp: BooleanExpression => tryOptimizeBoolExp(exp)
    case _ => exp
  }

  protected def reduceToInt(exp: SymbolicExpression): EitherReduced = exp match {
    case SymbolicInt(i, _) => Right(i)
    case exp: ArithmeticalVariadicOperationExpression => tryOptimizeArithExp(exp)
    case _ => Left(exp)
  }

  private def tryOptimizeBoolExp(exp: BooleanExpression): BooleanExpression =
    exp match {
      case LogicalUnaryExpression(LogicalNot, SymbolicBool(b, _), _) =>
        SymbolicBool(!b)
      /*
       * Can't try to optimize the negation of the expression, because the negation might prepend the expression with a LogicalNot,
       * which would cause an infinite loop.
       */
      case LogicalUnaryExpression(LogicalNot, arg, _) =>
        LogicalUnaryExpression(LogicalNot, tryOptimizeBoolExp(arg))
      case _ => exp
    }

}

object SymbolicExpressionNoIdentifierOptimizer extends SymbolicExpressionOptimizer {

  def optimizeBoolExp(exp: BooleanExpression): BooleanExpression = {
    if (SymbolicExpressionUsesIdentifier.usesIdentifier(exp)) {
      exp
    } else {
      tryOptimizeBoolExp(exp)
    }
  }

  protected def reduceToInt(exp: SymbolicExpression): EitherReduced = {
    exp match {
      case SymbolicInt(i, None) => Right(i)
      case exp: ArithmeticalVariadicOperationExpression if exp.identifier.isEmpty => tryOptimizeArithExp(exp)
      case _ => Left(exp)
    }
  }

  private def tryOptimizeBoolExp(exp: BooleanExpression): BooleanExpression =
    exp match {
      case LogicalUnaryExpression(LogicalNot, SymbolicBool(b, _), None) =>
        SymbolicBool(!b)
      /*
       * Can't try to optimize the negation of the expression, because the negation might prepend the expression with a LogicalNot,
       * which would cause an infinite loop.
       */
      case LogicalUnaryExpression(LogicalNot, arg, None) =>
        LogicalUnaryExpression(LogicalNot, tryOptimizeBoolExp(arg))
      case _ => exp
    }

}
