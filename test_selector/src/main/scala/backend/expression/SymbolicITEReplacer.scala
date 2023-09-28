package backend.expression

import backend.Path
import backend.tree.follow_path.{ElseDirection, ThenDirection}

object SymbolicITEReplacer {

  def replaceIn(
    ite: SymbolicExpression,
    replaceWith: SymbolicExpression,
    itePath: Path
  ): SymbolicExpression = itePath.headOption match {
    case None => replaceWith
    case Some(ThenDirection) => ite match {
      case ite: SymbolicITEExpression[_, _] => ite.copy(thenExp = replaceIn(ite.thenExp, replaceWith, itePath.tail))
      case _ =>
        throw new Exception(s"Expected a SymbolicITEExpression, but got $ite")
    }
    case Some(ElseDirection) => ite match {
      case ite: SymbolicITEExpression[_, _] => ite.copy(elseExp = replaceIn(ite.elseExp, replaceWith, itePath.tail))
      case _ =>
        throw new Exception(s"Expected a SymbolicITEExpression, but got $ite")
    }
  }

  def replacePredicateIn(
    ite: SymbolicExpression,
    newPredicateExp: BooleanExpression,
    itePath: Path
  ): SymbolicExpression = itePath.headOption match {
    case None => ite match {
      case ite: SymbolicITEExpression[_, _] => ite.copy(predExp = newPredicateExp)
      case _ =>
        throw new Exception(s"Expected a SymbolicITEExpression, but got $ite")
    }
    case Some(ThenDirection) => ite match {
      case ite: SymbolicITEExpression[_, _] => ite.copy(thenExp = replacePredicateIn(ite.thenExp, newPredicateExp, itePath.tail))
      case _ => throw new Exception(s"Expected a SymbolicITEExpression, but got $ite")
    }
    case Some(ElseDirection) => ite match {
      case ite: SymbolicITEExpression[_, _] => ite.copy(elseExp = replacePredicateIn(ite.elseExp, newPredicateExp, itePath.tail))
      case _ => throw new Exception(s"Expected a SymbolicITEExpression, but got $ite")
    }
  }

}
