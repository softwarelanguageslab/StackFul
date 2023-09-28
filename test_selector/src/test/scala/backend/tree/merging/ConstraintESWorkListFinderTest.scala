package backend.tree.merging

import backend.Path
import backend.execution_state._
import backend.expression.SymbolicBool
import backend.tree._
import backend.tree.constraints.{ConstraintAllInOne, ConstraintNegater}
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import org.scalatest.funsuite.AnyFunSuite

class ConstraintESWorkListFinderTest extends AnyFunSuite {

  import scala.language.implicitConversions

  implicit private def toThenEdge(node: SymbolicNode[ConstraintES]): ThenEdge[ConstraintES] = {
    ThenEdge.to(node)
  }
  implicit private def toElseEdge(node: SymbolicNode[ConstraintES]): ElseEdge[ConstraintES] = {
    ElseEdge.to(node)
  }

  val allInOne: ConstraintAllInOne[ConstraintES] = ConstraintESAllInOne
  implicit val constraintESNegater: ConstraintNegater[ConstraintES] = allInOne.constraintNegater
  val defaultStore: SymbolicStore = emptyStore
  def unexplored: SymbolicNode[ConstraintES] = UnexploredNode()
  def bc(state: ExecutionState): ConstraintES = EventConstraintWithExecutionState(bc.ec, state)
  def bc: BasicConstraintES = toConstraintES(BranchConstraint(SymbolicBool(true), Nil))
  def state(serial: Int): ExecutionState = {
    CodeLocationExecutionState(SerialCodePosition("file", serial), FunctionStack.empty, None, 1)
  }

  /*
   * Nodes are represented by the following characters:
   *   - b: BranchSymbolicNode
   *   - s: SingleChildNode
   *   - l: RegularLeafNode
   *   - u: UnsatisfiableNode
   *   - x: UnexploredNode
   * Uppercase characters denote nodes that use the requested execution state.
   * The extent of the subtree is represented by parentheses: (parent-node child-node1 child-node2 ...)
   */

  test("Descend in found node: (B l u) -> { [] }") {
    val es = state(1)
    val root = BranchSymbolicNode[ConstraintES](
      bc(es), RegularLeafNode[ConstraintES](), UnsatisfiableNode[ConstraintES]())
    val finder = new ConstraintESWorkListFinderWithDescend(es)
    val workList = finder.findWorkList(root)
    assert(workList.nonEmpty)
    assert(workList.head.treePath.getPath == Nil)
  }

  test("Do not descend in found node: (B l u) -> { [] }") {
    val es = state(1)
    val root = BranchSymbolicNode[ConstraintES](
      bc(es), RegularLeafNode[ConstraintES](), UnsatisfiableNode[ConstraintES]())
    val finder = new ConstraintESWorkListFinderWithoutDescend(es)
    val workList = finder.findWorkList(root)
    assert(workList.nonEmpty)
    assert(workList.head.treePath.getPath == Nil)
  }

  test("Descend in found node: (B (B x x) (B x x)) -> { [], [T], [E] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val root = BranchSymbolicNode[ConstraintES](bc(es), child, child)
    val finder = new ConstraintESWorkListFinderWithDescend(es)
    val workList = finder.findWorkList(root).map(_.treePath)
    assert(workList.size == 3)
    workList.foreach(treePath => {
      val path = treePath.getPath
      assert(path.isEmpty || path.size == 1)
      if (path.size == 1) {
        assert(path == ("T": Path) || path == ("E": Path))
      } else if (path.isEmpty) {
        assert(true)
      } else {
        assert(false)
      }
    })
  }

  test("Do not descend in found node: (B (B x x) (B x x)) -> { [] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val root = BranchSymbolicNode[ConstraintES](bc(es), child, child)
    val finder = new ConstraintESWorkListFinderWithoutDescend(es)
    val workList = finder.findWorkList(root).map(_.treePath)
    assert(workList.size == 1)
    assert(workList.head.getPath == Nil)
  }

  test("Descend in found node: (b (B x x) x) -> { [T] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val root = BranchSymbolicNode[ConstraintES](bc, child, unexplored)
    val finder = new ConstraintESWorkListFinderWithDescend(es)
    val workList = finder.findWorkList(root)
    assert(workList.size == 1)
    workList.head.treePath.getPath == "T"
  }

  test("Do not descend in found node: (b (B x x) x) -> { [T] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val root = BranchSymbolicNode[ConstraintES](bc, child, unexplored)
    val finder = new ConstraintESWorkListFinderWithoutDescend(es)
    val workList = finder.findWorkList(root)
    assert(workList.size == 1)
    workList.head.treePath.getPath == "T"
  }

  test("Descend in found node: (b (b x (B x x)) (B (B x x) x)) -> { [E], [E, F], [T, E] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val t = BranchSymbolicNode(bc, UnexploredNode[ConstraintES](), child)
    val e = BranchSymbolicNode(bc(es), child, UnexploredNode[ConstraintES]())
    val root = BranchSymbolicNode[ConstraintES](bc, t, e)
    val finder = new ConstraintESWorkListFinderWithDescend(es)
    val workList = finder.findWorkList(root).map(_.treePath)
    assert(workList.size == 3)
    workList.foreach(treePath => {
      val path = treePath.getPath
      if (path.size == 1) {
        assert(path == ("E": Path))
      } else if (path.size == 2) {
        assert(path == ("EF": Path) || path == ("TE": Path))
      } else {
        assert(false)
      }
    })
  }

  test("Do not descend in found node: (b (b x (B x x)) (B (B x x) x)) -> { [E], [T, E] }") {
    val es = state(1)
    def child: SymbolicNode[ConstraintES] = BranchSymbolicNode(bc(es), unexplored, unexplored)
    val t = BranchSymbolicNode(bc, UnexploredNode[ConstraintES](), child)
    val e = BranchSymbolicNode(bc(es), child, UnexploredNode[ConstraintES]())
    val root = BranchSymbolicNode[ConstraintES](bc, t, e)
    val finder = new ConstraintESWorkListFinderWithoutDescend(es)
    val workList = finder.findWorkList(root).map(_.treePath)
    assert(workList.size == 2)
    workList.foreach(treePath => {
      val path = treePath.getPath
      if (path.size == 1) {
        assert(path == ("E": Path))
      } else if (path.size == 2) {
        assert(path == ("EF": Path) || path == ("TE": Path))
      } else {
        assert(false)
      }
    })
  }

}
