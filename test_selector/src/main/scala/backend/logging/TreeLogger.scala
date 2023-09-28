package backend.logging

import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.ConstraintES

case class TreeLogger(
  treeDotWriter: SymbolicTreeDotWriter[ConstraintES],
  var path: String,
  logLevel: LogLevel
) extends LogAction[SymbolicNode[ConstraintES]] {
  def setPath(newPath: String): Unit = {
    path = newPath
  }
  override def doAction(tree: => SymbolicNode[ConstraintES]): Unit = {
    treeDotWriter.writeTree(tree, path)
  }
}
