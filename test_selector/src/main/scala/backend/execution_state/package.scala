package backend

import backend.expression._


package object execution_state {
  def emptyStore: SymbolicStore = new SymbolicStore(Map[String, SymbolicExpression]())
  class SymbolicStore(val map: Map[String, SymbolicExpression]) extends AnyVal {
    override def toString: String = {
      map.filter(_._2 != SymbolicNothingExpression).toString
    }
  }

  import scala.language.implicitConversions

  implicit def mapToSymStore(map: Map[String, SymbolicExpression]): SymbolicStore = {
    new SymbolicStore(map)
  }
  implicit def symStoreToMap(symbolicStore: SymbolicStore): Map[String, SymbolicExpression] = symbolicStore.map
}
