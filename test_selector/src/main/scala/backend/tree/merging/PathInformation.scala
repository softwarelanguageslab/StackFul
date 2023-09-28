package backend.tree.merging

import backend.execution_state.{ExecutionState, SymbolicStore}
import backend.tree.{OffersVirtualPath, SymbolicNode}
import backend.tree.constraints.Constraint
import backend.tree.search_strategy.TreePath

case class PathInformation[C <: Constraint, Node <: SymbolicNode[C]](
  executionState: ExecutionState,
  node: Node,
  pathToNode: TreePath[C],
  store: SymbolicStore
) {
  // Type bound: https://stackoverflow.com/a/18465363
  def toOffersVirtualPath(offersVirtualPath: OffersVirtualPath): PathInformationOffersVirtualPath[C] = {
    PathInformationOffersVirtualPath(executionState, offersVirtualPath, pathToNode, store)
  }
//    def toOffersVirtualPath(offersVirtualPath: OffersVirtualPath): PathInformationOffersVirtualPath[C] = {
//      PathInformationOffersVirtualPath(offersVirtualPath, pathToNode, store, ESStack)
//    }
}
case class PathInformationOffersVirtualPath[C <: Constraint](
  executionState: ExecutionState,
  offersVirtualPath: OffersVirtualPath,
  pathToNode: TreePath[C],
  store: SymbolicStore
)
