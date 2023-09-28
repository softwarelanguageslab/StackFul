package backend.tree.follow_path.search_strategy

import backend.Path
import backend.execution_state._
import backend.expression._
import backend.tree._
import backend.tree.constraints.{BasicConstraint, basic_constraints}
import backend.tree.constraints.basic_constraints._
import backend.tree.follow_path._
import backend.tree.search_strategy.TreePath
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class TreePathTest extends AnyFunSuite with BeforeAndAfterEach {

  val defaultStore: SymbolicStore = emptyStore

  test("Following then-branch correctly updates path and seen constraints") {
    val bc = BranchConstraint(SymbolicBool(b = true), Nil)
    val bsn = BranchSymbolicNode[BasicConstraint](
      bc, ThenEdge.to(RegularLeafNode()), ElseEdge.to(RegularLeafNode()))
    var (p1, isUnexplored) = TreePath.init[BasicConstraint](bsn)
    assert(!isUnexplored)
    p1 = p1.finishedViaThen
    assert(p1.original.length == 1)
    assert(p1.getObserved.length == 1)
    assert(p1.getPath.length == 1)
    assert(p1.original.head == bsn)
    assert(p1.getObserved.head == bc)
    assert(p1.getPath.head == ThenDirection)
  }

  test("Following else-branch correctly updates path and seen constraints") {
    val bc = basic_constraints.BranchConstraint(SymbolicBool(b = true), Nil)
    val bsn = BranchSymbolicNode[BasicConstraint](
      bc, ThenEdge.to(RegularLeafNode()), ElseEdge.to(RegularLeafNode()))
    var (p1, isUnexplored) = TreePath.init[BasicConstraint](bsn)
    assert(!isUnexplored)
    p1 = p1.finishedViaElse
    assert(p1.original.length == 1)
    assert(p1.getObserved.length == 1)
    assert(p1.getPath.length == 1)
    assert(p1.original.head == bsn)
    assert(p1.getObserved.head == bc.negate)
    assert(p1.getPath.head == ElseDirection)
  }

  private def toBool(b: Boolean): SymbolicBool = {
    SymbolicBool(b)
  }
  private def toInt(i: Int): SymbolicInt = {
    SymbolicInt(i)
  }
  private def toString(s: String): SymbolicString = {
    SymbolicString(s)
  }

  test("Init(BranchSymbolicNode(...)) generates an empty Path") {
    val bsn =
      BranchSymbolicNode(
        basic_constraints.BranchConstraint(toBool(true), Nil): BasicConstraint, ThenEdge.to(RegularLeafNode()),
        ElseEdge.to(RegularLeafNode()))
    var (p0, isUnexplored) = TreePath.init(bsn)
    p0 = p0.finished
    assert(!isUnexplored)
    assert(p0.getPath.isEmpty)
  }

  test("Should generate tttttett") {
    def makeIntCompareNode(
      i1: Int,
      i2: Int,
      thenNode: SymbolicNode[BasicConstraint]
    ): BranchSymbolicNode[BasicConstraint] = {
      BranchSymbolicNode(
        basic_constraints.BranchConstraint(
          RelationalExpression(toInt(i1), IntLessThan, toInt(i2)), Nil),
        ThenEdge.to(thenNode),
        ElseEdge.to(UnsatisfiableNode()))
    }
    def makeStringCompareNode(
      s1: String,
      s2: String,
      thenNode: SymbolicNode[BasicConstraint],
      elseNode: SymbolicNode[BasicConstraint]
    ): BranchSymbolicNode[BasicConstraint] = {
      BranchSymbolicNode(
        basic_constraints.BranchConstraint(
          RelationalExpression(toString(s1), StringEqual, toString(s2)), Nil),
        ThenEdge.to(thenNode),
        ElseEdge.to(elseNode))
    }
    val bsn8 = makeStringCompareNode("object", "object", UnexploredNode(), UnexploredNode())
    val bsn7 = makeStringCompareNode("object", "object", bsn8, UnexploredNode())
    val bsn6 = BranchSymbolicNode(
      basic_constraints.BranchConstraint(RelationalExpression(toInt(5), IntLessThan, toInt(5)), Nil),
      ThenEdge.to(UnsatisfiableNode()),
      ElseEdge.to(bsn7))
    val bsn5 = makeIntCompareNode(4, 5, bsn6)
    val bsn4 = makeIntCompareNode(3, 5, bsn5)
    val bsn3 = makeIntCompareNode(2, 5, bsn4)
    val bsn2 = makeIntCompareNode(1, 5, bsn3)
    val bsn1 = makeIntCompareNode(0, 5, bsn2)
    val (p0, isUnexplored) = TreePath.init(bsn1)
    assert(!isUnexplored)
    assert(p0.finishedViaThen.getPath == ("T": Path))
    val p1 = p0.addThenBranch(bsn2, bsn1.thenBranch)
    val p2 = p1.addThenBranch(bsn3, bsn2.thenBranch)
    val p3 = p2.addThenBranch(bsn4, bsn3.thenBranch)
    val p4 = p3.addThenBranch(bsn5, bsn4.thenBranch)
    val p5 = p4.addThenBranch(bsn6, bsn5.thenBranch)
    val p6 = p5.addElseBranch(bsn7, bsn6.elseBranch)
    val p7 = p6.addThenBranch(bsn8, bsn7.thenBranch)
    assert(p1.finishedViaThen.getPath === ("TT": Path))
    assert(p2.finishedViaThen.getPath === ("TTT": Path))
    assert(p3.finishedViaThen.getPath === ("TTTT": Path))
    assert(p4.finishedViaThen.getPath === ("TTTTT": Path))
    assert(p5.finishedViaThen.getPath === ("TTTTTT": Path))
    assert(p6.finishedViaThen.getPath === ("TTTTTET": Path))
    assert(p7.finishedViaThen.getPath == ("TTTTTETT": Path))
  }

  test("Test a random sequence of calls to 'addNegateNode' and ':+'") {
    // Use a fixed seed to make sure the test outcome is deterministic
    val seed = 0
    val random = new Random(seed)

    val exp = RelationalExpression(SymbolicInt(1), IntEqual, SymbolicInt(2))
    val bc = basic_constraints.BranchConstraint(exp, Nil)
    val node = BranchSymbolicNode[BasicConstraint](
      bc, ThenEdge.to(RegularLeafNode()), ElseEdge.to(RegularLeafNode()))

    var (path, _) = TreePath.init[BasicConstraint](node)
    var directions: List[BinaryDirection] = Nil
    var seenConstraints: List[BasicConstraint] = Nil
    var nodes: List[SymbolicNode[BasicConstraint]] = List(node)

    0.until(30).foreach(_ => {
      nodes :+= node
      if (random.nextBoolean()) {
        path = path.addThenBranch(node, ThenEdge(Nil, node))
        directions :+= ThenDirection
        seenConstraints :+= bc
      } else {
        path = path.addElseBranch(node, ElseEdge(Nil, node))
        directions :+= ElseDirection
        seenConstraints :+= bc.negate
      }
      assert(path.original == nodes)
    })
    path = path.finishedViaThen
    assert(path.original == nodes)
    assert(path.getObserved == (seenConstraints :+ bc))
    assert(path.getPath == (directions :+ ThenDirection))
  }

  test("Test a random sequence of calls to 'addThenBranch' and 'addElseBranch'") {
    // Use a fixed seed to make sure the test outcome is deterministic
    val seed = 0
    val random = new Random(seed)

    val exp = RelationalExpression(SymbolicInt(1), IntEqual, SymbolicInt(2))
    val bc = basic_constraints.BranchConstraint(exp, Nil)
    val node = BranchSymbolicNode[BasicConstraint](
      bc, ThenEdge.to(RegularLeafNode()), ElseEdge.to(RegularLeafNode()))

    var (path, _) = TreePath.init[BasicConstraint](node)
    var directions: List[BinaryDirection] = Nil
    var seenConstraints: List[BasicConstraint] = Nil
    var nodes: List[SymbolicNode[BasicConstraint]] = List(node)

    0.until(30).foreach(_ => {
      nodes :+= node
      if (random.nextBoolean()) {
        path = path.addThenBranch(node, ThenEdge(Nil, node))
        directions :+= ThenDirection
        seenConstraints :+= bc
      } else {
        path = path.addElseBranch(node, ElseEdge(Nil, node))
        directions :+= ElseDirection
        seenConstraints = seenConstraints :+ bc.negate
      }
      assert(path.original == nodes)
    })
    path = path.finishedViaElse // Use finishedViaElse this time
    assert(path.original == nodes)
    assert(path.getObserved == (seenConstraints :+ bc.negate))
    assert(path.getPath == (directions :+ ElseDirection))
  }

}
