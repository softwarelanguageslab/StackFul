package backend.tree.merging

import backend.Path
import backend.execution_state.ExecutionState

object CentralMergedPathRegistry {

  private var registeredPaths: Map[ExecutionState, Set[Path]] = Map()

  def addPath(executionState: ExecutionState, path: Path): Unit = {
    registeredPaths.get(executionState) match {
      case None => registeredPaths += executionState -> Set(path)
      case Some(set) => registeredPaths += executionState -> (set + path)
    }
  }

  def containsPath(executionState: ExecutionState, path: Path): Boolean = {
    registeredPaths.get(executionState) match {
      case None =>
        // Immediately add an empty set to the map, for performance reasons
        registeredPaths += executionState -> Set()
        false
      case Some(set) => set.contains(path)
    }
  }

  def reset(): Unit = {
    registeredPaths = Map()
  }

}
