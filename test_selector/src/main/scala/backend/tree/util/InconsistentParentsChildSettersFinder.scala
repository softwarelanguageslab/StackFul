package backend.tree.util

import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.{BranchSymbolicNode, MergedNode, RegularLeafNode, SymbolicNode, UnexploredNode, UnsatisfiableNode}

object InconsistentParentsChildSettersFinder {

  def find[T](node: SymbolicNode[ConstraintES]): Unit = node match {
    case RegularLeafNode() | MergedNode() | UnexploredNode() | UnsatisfiableNode() =>
    case BranchSymbolicNode(_, thenBranch, elseBranch) =>
      if (thenBranch.to.getParentsChildSetters.nonEmpty && !thenBranch.to.getParentsChildSetters.contains(node)) {
        throw new Exception()
      } else {
        find(thenBranch.to)
      }
      if (elseBranch.to.getParentsChildSetters.nonEmpty && !elseBranch.to.getParentsChildSetters.contains(node)) {
        throw new Exception()
      } else {
        find(elseBranch.to)
      }
  }

}
