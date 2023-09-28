package backend.tree.merging

import backend.execution_state.ExecutionState
import backend.tree.OffersVirtualPath

object CentralVirtualPathStore {

  private var map: Map[ExecutionState, OffersVirtualPath] = Map()

  def getStore(executionState: ExecutionState): OffersVirtualPath = map.get(executionState) match {
    case None =>
      val newStore = new VirtualPathStorePerIdentifier
      map += executionState -> newStore
      newStore
    case Some(store) => store
  }

  def reset(): Unit = {
    map = Map()
  }

}
