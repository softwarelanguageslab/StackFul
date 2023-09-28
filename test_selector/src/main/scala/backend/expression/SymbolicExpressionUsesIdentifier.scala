package backend.expression

import backend.tree.UsesIdentifier

object SymbolicExpressionUsesIdentifier extends UsesIdentifier[SymbolicExpression] {
  override def usesIdentifier(exp: SymbolicExpression): Boolean = exp match {
    case ArithmeticalVariadicOperationExpression(_, args, id) => id.nonEmpty || args.exists(usesIdentifier)
    case ArithmeticalUnaryOperationExpression(_, arg, id) => id.nonEmpty || usesIdentifier(arg)
    case RelationalExpression(left, _, right, id) => id.nonEmpty || usesIdentifier(left) || usesIdentifier(right)
    case LogicalBinaryExpression(left, _, right, id) => id.nonEmpty || usesIdentifier(left) || usesIdentifier(right)
    case LogicalUnaryExpression(_, arg, id) => id.nonEmpty || usesIdentifier(arg)
    case StringOperationProducesIntExpression(_, args, id) => id.nonEmpty || args.exists(usesIdentifier)
    case StringOperationProducesStringExpression(_, args, id) => id.nonEmpty || args.exists(usesIdentifier)
    case _ => exp.identifier.nonEmpty
  }
}
