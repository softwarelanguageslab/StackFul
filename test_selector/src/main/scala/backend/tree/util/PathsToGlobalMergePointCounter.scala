package backend.tree.util

import backend.tree._
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.merging._

import scala.annotation.tailrec

object PathsToGlobalMergePointCounter {

  def countPaths[T](from: SymbolicNode[ConstraintES]): Option[(BranchSymbolicNode[ConstraintES], Int)] = {
    @tailrec
    def count(
      workList: List[SymbolicNode[ConstraintES]],
      toFind: Option[BranchSymbolicNode[ConstraintES]],
      numberOfPaths: Int
    ): Option[(BranchSymbolicNode[ConstraintES], Int)] = workList.headOption match {
      case None => toFind match {
        case None => None
        case Some(found) => Some((found, numberOfPaths))
      }
      case Some(node) => node match {
        case bsn: BranchSymbolicNode[ConstraintES] =>
          if (toFind.isEmpty && isGlobalMergePoint(bsn)) {
            count(workList.tail, Some(bsn), 1)
          } else if (toFind.contains(bsn)) {
            count(workList.tail, toFind, numberOfPaths + 1)
          } else {
            val newWorkList = bsn.thenBranch.to :: bsn.elseBranch.to :: workList.tail
            count(newWorkList, toFind, numberOfPaths)
          }
        case _ => count(workList.tail, toFind, numberOfPaths)
      }
    }
    count(List(from), None, 0)
  }

}
