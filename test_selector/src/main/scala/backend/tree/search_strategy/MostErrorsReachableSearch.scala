//package backend.tree.search_strategy
//
//import backend.path_filtering.PartialRegexMatcher
//import backend.tree._
//import dk.brics.automaton.State
//
//import scala.annotation.tailrec
//
//class MostErrorsReachableSearch extends SearchStrategy[PartialRegexMatcher] {
//
//  def countReachableErrors(partialRegexMatcher: PartialRegexMatcher): Int = {
//    @tailrec
//    def loop(todo: Set[State], visited: Set[State], currentCount: Int): Int =
//      todo.headOption match {
//        case None                                 => currentCount
//        case Some(head) if visited.contains(head) => loop(todo.tail, visited, currentCount)
//        case Some(head) =>
//          val destStates =
//            scala.collection.JavaConverters.asScalaSet(head.getTransitions).map(_.getDest)
//          val updatedCount = if (head.isAccept) currentCount + 1 else currentCount
//          loop(todo.tail ++ destStates, visited + head, updatedCount)
//      }
//    loop(Set(partialRegexMatcher.lastState), Set(), 0)
//  }
//
//  def findAllUnexploredNodes(
//      b: BranchSymbolicNode[PartialRegexMatcher],
//      treePath: TreePath[PartialRegexMatcher]): Set[(TreePath[PartialRegexMatcher], Int)] = {
//    if (!(b.thenBranchTaken && b.elseBranchTaken)) {
//      Set((treePath, countReachableErrors(b.extraInfo)))
//    } else {
//      (b.thenBranch, b.elseBranch) match {
//        case (thenBranch: BranchSymbolicNode[PartialRegexMatcher],
//              elseBranch: BranchSymbolicNode[PartialRegexMatcher]) =>
//          findAllUnexploredNodes(thenBranch, treePath.addThenBranch(thenBranch)) ++ findAllUnexploredNodes(
//            elseBranch,
//            treePath.addElseBranch(elseBranch))
//        case (thenBranch: BranchSymbolicNode[PartialRegexMatcher], _) =>
//          findAllUnexploredNodes(thenBranch, treePath.addThenBranch(thenBranch))
//        case (_, elseBranch: BranchSymbolicNode[PartialRegexMatcher]) =>
//          findAllUnexploredNodes(elseBranch, treePath.addElseBranch(elseBranch))
//        case (_, _) => Set()
//      }
//    }
//  }
//
//  def findFirstUnexploredNode(
//      symbolicNode: SymbolicNode[PartialRegexMatcher]): Option[TreePath[PartialRegexMatcher]] =
//    symbolicNode match {
//      case b: BranchSymbolicNode[PartialRegexMatcher] =>
//        val initTree: TreePath[PartialRegexMatcher] = TreePath.init(b) match {
//          case Left(left)   => left
//          case Right(right) => right
//        }
//        val allResults = findAllUnexploredNodes(b, initTree)
//        allResults
//          .filter(_._1.length > 0)
//          .toList
//          .sortWith((t1, t2) => t1._2 > t2._2)
//          .headOption
//          .map(_._1)
//      case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() |
//          UnsatisfiableNode() =>
//        None
//    }
//}
