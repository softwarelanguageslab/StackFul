package backend.solvers

import backend.expression.SymbolicBool
import backend.tree._
import backend.tree.constraints.BasicConstraint
import backend.tree.constraints.basic_constraints._
import backend.tree.follow_path.{ElseDirection, ThenDirection}
import backend.tree.search_strategy.TreePath
import org.scalatest.funsuite.AnyFunSuite

class PathNegaterTest extends AnyFunSuite {
  val boolFalseConstraint: BranchConstraint = BranchConstraint(SymbolicBool(false), Nil)
  val boolTrueConstraint: BranchConstraint = BranchConstraint(SymbolicBool(true), Nil)
  def leafNode: RegularLeafNode[BasicConstraint] = RegularLeafNode()
  def unexploredNode: UnexploredNode[BasicConstraint] = UnexploredNode()
  def boolNode(
    thenNode: SymbolicNode[BasicConstraint],
    elseNode: SymbolicNode[BasicConstraint]
  ): BranchSymbolicNode[BasicConstraint] =
    BranchSymbolicNode(boolTrueConstraint, ThenEdge.to(thenNode), ElseEdge.to(elseNode))

  /*
   * X: node fully explored
   * U: unexplored node
   * L: leaf node
   *
   * T: then-branch of node unexplored
   * E: else-branch of node unexplored
   * B: both branches of node unexplored
   */

  test("testNegatePath with path of length 1, T") {
    val node1 = boolNode(unexploredNode, leafNode)
    val eitherPath = TreePath.init(node1)
    assert(eitherPath._2)
    val path = eitherPath._1
    val negatedPath = PathNegater.negatePath(path)
    assert(negatedPath.length == 1)
    assert(negatedPath.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.getObserved.head == boolTrueConstraint)
    assert(negatedPath.getPath.head == ThenDirection)
  }

  test("testNegatePath with path of length 1, E") {
    val node1 = boolNode(leafNode, unexploredNode)
    val eitherPath = TreePath.init(node1)
    assert(eitherPath._2)
    val path = eitherPath._1
    val negatedPath = PathNegater.negatePath(path)
    assert(negatedPath.length == 1)
    assert(negatedPath.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.getObserved.head == boolFalseConstraint)
    assert(negatedPath.getPath.head == ElseDirection)
  }

  test("testNegatePath with path of length 2: X :+ E") {
    val node2 = boolNode(leafNode, unexploredNode)
    val node1 = boolNode(node2, leafNode)
    val eitherPath = TreePath.init(node1)
    assert(!eitherPath._2)
    val path = eitherPath._1.addThenBranch(node2, node1.thenBranch)
    val negatedPath = PathNegater.negatePath(path)

    assert(negatedPath.length == 2)
    assert(negatedPath.init.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.init.getObserved.head == boolTrueConstraint)
    assert(negatedPath.init.getPath.head == ThenDirection)

    val (lastOriginal, lastSeen, lastPath) = negatedPath.last
    assert(lastOriginal == node2)
    assert(lastSeen == boolFalseConstraint)
    assert(lastPath.toDirection == ElseDirection)
  }

  test("testNegatePath with path of length 2: X :+ T") {
    val node2 = boolNode(unexploredNode, leafNode)
    val node1 = boolNode(node2, leafNode)
    val eitherPath = TreePath.init(node1)
    assert(!eitherPath._2)
    val path = eitherPath._1.addThenBranch(node2, node1.thenBranch)
    val negatedPath = PathNegater.negatePath(path)

    assert(negatedPath.length == 2)
    assert(negatedPath.init.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.init.getObserved.head == boolTrueConstraint)
    assert(negatedPath.init.getPath.head == ThenDirection)

    val (lastOriginal, lastSeen, lastPath) = negatedPath.last
    assert(lastOriginal == node2)
    assert(lastSeen == boolTrueConstraint)
    assert(lastPath.toDirection == ThenDirection)
  }

  test("testNegatePath with path of length 2: X :+ B") {
    val node2 = boolNode(unexploredNode, unexploredNode)
    val node1 = boolNode(node2, leafNode)
    val eitherPath = TreePath.init(node1)
    assert(!eitherPath._2)
    val path = eitherPath._1.addThenBranch(node2, node1.thenBranch)
    val negatedPath = PathNegater.negatePath(path)

    assert(negatedPath.length == 2)
    assert(negatedPath.init.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.init.getObserved.head == boolTrueConstraint)
    assert(negatedPath.init.getPath.head == ThenDirection)

    val (lastOriginal, lastSeen, lastPath) = negatedPath.last
    assert(lastOriginal == node2)
    assert(lastSeen == boolTrueConstraint)
    assert(lastPath.toDirection == ThenDirection)
  }

  test("testNegatePath with path of length 2: X !:+ T") {
    val node2 = boolNode(unexploredNode, leafNode)
    val node1 = boolNode(node2, leafNode)
    val eitherPath = TreePath.init(node1)
    assert(!eitherPath._2)
    val path = eitherPath._1.addElseBranch(node2, node1.elseBranch)
    val negatedPath = PathNegater.negatePath(path)

    assert(negatedPath.length == 2)
    assert(negatedPath.init.original.head.constraint == boolTrueConstraint)
    assert(negatedPath.init.getObserved.head == boolFalseConstraint)
    assert(negatedPath.init.getPath.head == ElseDirection)

    val (lastOriginal, lastSeen, lastPath) = negatedPath.last
    assert(lastOriginal == node2)
    assert(lastSeen == boolTrueConstraint)
    assert(lastPath.toDirection == ThenDirection)
  }

}
