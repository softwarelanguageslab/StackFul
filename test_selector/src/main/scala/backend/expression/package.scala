package backend

package object expression {

  def isExpressionConstant(exp: SymbolicExpression): Boolean = exp match {
    case e if isExpAtomicConstant(e) => true
    case boolExp: BooleanExpression =>
      SymbolicExpressionNoIdentifierOptimizer.optimizeBoolExp(boolExp) match {
        case e if isExpAtomicConstant(e) => true
        case RelationalExpression(left, _, right, _) =>
          isExpAtomicConstant(left) && isExpAtomicConstant(right)
        case _ => false
      }
    case arithExp: ArithmeticalVariadicOperationExpression =>
      SymbolicExpressionNoIdentifierOptimizer.optimizeArithExp(arithExp) match {
        case e if isExpAtomicConstant(e) => true
        case ArithmeticalVariadicOperationExpression(_, args, _) => args.forall(isExpAtomicConstant)
      }
    case _ => false
  }

  def isExpAtomicConstant(exp: SymbolicExpression): Boolean = exp match {
    case _: SymbolicBool | _: SymbolicInt => true
    case _ => false
  }

  def iteEquals[T <: SymbolicExpression, U <: SymbolicExpression](
    ite1: SymbolicITEExpression[T, U], ite2: SymbolicITEExpression[T, U]
  ): Boolean = {
    def compareBranches(branch1: SymbolicExpression, branch2: SymbolicExpression): Boolean = {
      (branch1, branch2) match {
        case (branch1@SymbolicITEExpression(_, _, _, _), branch2@SymbolicITEExpression(_, _, _, _)) => iteEquals(
          branch1, branch2)
        case _ => branch1 == branch2
      }
    }

    ite1 == ite2 ||
      (ite1.predExp == ite2.predExp && ite1.identifier == ite2.identifier && compareBranches(
        ite1.thenExp, ite2.thenExp) && compareBranches(ite1.elseExp, ite2.elseExp)) ||
      (ite1.predExp == ite2.predExp.negate && ite1.identifier == ite2.identifier && compareBranches(
        ite1.thenExp, ite2.elseExp) && compareBranches(ite1.elseExp, ite2.thenExp)) ||
      (ite1.predExp.negate == ite2.predExp && ite1.identifier == ite2.identifier && compareBranches(
        ite1.thenExp, ite2.elseExp) && compareBranches(ite1.elseExp, ite2.thenExp))
  }

}
