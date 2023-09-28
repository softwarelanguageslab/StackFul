package backend.tree

import backend.expression._
import backend.tree.constraints._
import backend.tree.follow_path._
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec
import scala.collection.immutable.Queue

class PathConstraintForUnexploredNodeFinder[C <: Constraint : ConstraintNegater : ToBooleanExpression] {
  private type PathNodeDirection = (TreePath[C], SymbolicNode[C])

  def createPathConstraintFromTo(
    root: SymbolicNode[C],
    to: SymbolicNode[C],
    dir: BinaryDirection,
    getUsableConstraints: TreePath[C] => List[C]
  ): BooleanExpression = {
    @tailrec
    def loop(
      workList: Queue[PathNodeDirection],
      disjunction: BooleanExpression
    ): BooleanExpression = workList.headOption match {
      case None => disjunction
      case Some((path, node: BranchSymbolicNode[C])) if to == node =>
        val finishedPath = dir match {
          case ElseDirection => path.finishedViaElse
          case ThenDirection => path.finishedViaThen
        }
        val pathExp = implicitly[ToBooleanExpression[C]].toBoolExp(getUsableConstraints(finishedPath))
        val newDisjunction = LogicalBinaryExpression(pathExp, LogicalOr, disjunction)
        loop(workList.tail, newDisjunction)
      case Some((path, BranchSymbolicNode(_, thenEdge, elseEdge))) =>
        val newWL1 = thenEdge.to match {
          case thenNode: BranchSymbolicNode[C] =>
            workList.tail :+ (path.addThenBranch(thenNode, thenEdge), thenNode)
          case _ =>
            workList.tail
        }
        val newWL2 = elseEdge.to match {
          case elseNode: BranchSymbolicNode[C] =>
            newWL1 :+ (path.addElseBranch(elseNode, elseEdge), elseNode)
          case _ =>
            newWL1
        }
        loop(newWL2, disjunction)
      case _ => loop(workList.tail, disjunction)
    }
    loop(Queue((TreePath.init(root)._1, root)), SymbolicBool(false))
  }
}
