package backend

import backend.logging._
import backend.tree.SymbolicTreeDotWriter
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.merging.use_cases.automatically_generated.PathChecker

object TestConfigs {

  private val logLevel: LogLevel = Error

  Logger.setLogLevel(logLevel)
  PathChecker.setPrintIdentifier(logLevel.level < Normal.level)

  val symbolicTreePath: String = "output/execution_trees/test_symbolic_tree.dot"
  val treeLogger: TreeLogger = TreeLogger(
    new SymbolicTreeDotWriter[ConstraintES], symbolicTreePath, logLevel)

}
