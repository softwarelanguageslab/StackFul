package backend.tree.constraints

import backend.expression._

import scala.annotation.tailrec

trait ToBooleanExpression[C] {
  def toBoolExp(c: C): Option[BooleanExpression]
  def toBoolExp(cs: Iterable[C]): BooleanExpression = {
    @tailrec
    def loop(cs: Iterable[C], exp: BooleanExpression): BooleanExpression = cs.headOption match {
      case None => exp
      case Some(c) =>
        val newExp: BooleanExpression = toBoolExp(c).fold(exp)(LogicalBinaryExpression(_, LogicalAnd, exp))
        loop(cs.tail, newExp)
    }
    loop(cs, SymbolicBool(true))
  }
}
