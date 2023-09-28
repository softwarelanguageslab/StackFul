package backend.tree.merging

import backend.tree.constraints._
import backend.tree.search_strategy.TreePath

class PathOrdering[C <: Constraint : ConstraintNegater] extends Ordering[TreePath[C]] {

  override def compare(x: TreePath[C], y: TreePath[C]): Int = {
    if (x.length != y.length) {
      implicitly[Ordering[Int]].compare(x.length, y.length)
    } else {
      implicitly[Ordering[Iterable[Int]]].compare(x.getPathWithEdges.map(_.edgeId), y.getPathWithEdges.map(_.edgeId))
    }
  }
}
