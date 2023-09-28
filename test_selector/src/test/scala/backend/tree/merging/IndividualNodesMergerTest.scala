package backend.tree.merging

import backend.{SetUseDebugToTrueTester, TestConfigs}
import backend.tree._
import backend.execution_state._
import backend.expression.SymbolicBool
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.tree.constraints.ConstraintAllInOne
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.search_strategy.TreePath

class IndividualNodesMergerTest extends SetUseDebugToTrueTester {

  val storeMerger = new SymbolicStoreMerger

  private val allInOne: ConstraintAllInOne[ConstraintES] = ConstraintESAllInOne
  import allInOne._

  val nodesMerger = new IndividualNodesMerger(
    storeMerger, TestConfigs.treeLogger, ConstraintESReporter.newReporter(MergingMode))
  private def branchNode: BranchSymbolicNode[ConstraintES] = {
    val executionState = CodeLocationExecutionState.dummyExecutionState
    val c: ConstraintES = EventConstraintWithExecutionState(BranchConstraint(SymbolicBool(true), Nil), executionState)
    BranchSymbolicNode(c, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
  }
  private def unexploredNode: UnexploredNode[ConstraintES] = UnexploredNode[ConstraintES]()
  private def leafNode: RegularLeafNode[ConstraintES] = RegularLeafNode[ConstraintES]()
  private def unsatisfiableNode: UnsatisfiableNode[ConstraintES] = UnsatisfiableNode[ConstraintES]()

  private def mergeTwoNodes(
    node1: SymbolicNodeWithConstraint[ConstraintES],
    node2: SymbolicNodeWithConstraint[ConstraintES]
  ): (SymbolicNode[ConstraintES], SymbolicStore) = {
    val result = nodesMerger.mergeTwoNodes(
      emptyStore, CodeLocationExecutionState.dummyExecutionState, Nil,
      PathInformation(node1.constraint.executionState, node1, TreePath.init(node1)._1, emptyStore),
      PathInformation(node2.constraint.executionState, node2, TreePath.init(node2)._1, emptyStore), Set(),
      Set())
    (result._1, result._2)
  }

  private def mergeTwoNodes(
    node1: SymbolicNode[ConstraintES],
    node2: SymbolicNode[ConstraintES]
  ): (SymbolicNode[ConstraintES], SymbolicStore) = {
    val result = nodesMerger.mergeTwoNodes(
      emptyStore, CodeLocationExecutionState.dummyExecutionState,Nil, PathInformation(
        CodeLocationExecutionState.dummyExecutionState, node1, TreePath.empty[ConstraintES], emptyStore),
      PathInformation(
        CodeLocationExecutionState.dummyExecutionState, node2, TreePath.empty[ConstraintES], emptyStore),
      Set(), Set())
    (result._1, result._2)
  }

  /** **********************
    * Branch nodes     *
    * ***********************/
  test("Branch ~ Branch -> Branch") {
    val node1 = branchNode
    val node2 = branchNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Branch ~ Leaf -> Branch") {
    val node1 = branchNode
    val node2 = leafNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Branch ~ Unexplored -> Branch") {
    val node1 = branchNode
    val node2 = unexploredNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Branch ~ Unsatisfiable -> Branch") {
    val node1 = branchNode
    val node2 = unsatisfiableNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  /** ********************
    * Leaf nodes     *
    * *********************/
  test("Leaf ~ Branch -> Branch") {
    val node1 = leafNode
    val node2 = branchNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Leaf ~ Leaf -> Leaf") {
    val node1 = leafNode
    val node2 = leafNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[RegularLeafNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Leaf ~ Unexplored -> Leaf") {
    val node1 = leafNode
    val node2 = unexploredNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[RegularLeafNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Leaf ~ Unsatisfiable -> Leaf") {
    val node1 = leafNode
    val node2 = unsatisfiableNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnsatisfiableNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  /** **************************
    * Unexplored nodes     *
    * ***************************/
  test("Unexplored ~ Branch -> Branch") {
    val node1 = unexploredNode
    val node2 = branchNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Unexplored ~ Leaf -> Leaf") {
    val node1 = unexploredNode
    val node2 = leafNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[RegularLeafNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  test("Unexplored ~ Unexplored -> Unexplored") {
    val node1 = unexploredNode
    val node2 = unexploredNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnexploredNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  test("Unexplored ~ Unsatisfiable -> Unexplored") {
    val node1 = unexploredNode
    val node2 = unsatisfiableNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnsatisfiableNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  /** *****************************
    * Unsatisfiable nodes     *
    * ******************************/
  test("Unsatisfiable ~ Branch -> Branch") {
    val node1 = unsatisfiableNode
    val node2 = branchNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
  test("Unsatisfiable ~ Leaf -> Leaf") {
    val node1 = unsatisfiableNode
    val node2 = leafNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnsatisfiableNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  test("Unsatisfiable ~ Unexplored -> Unexplored") {
    val node1 = unsatisfiableNode
    val node2 = unexploredNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnsatisfiableNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }

  test("Unsatisfiable ~ Unsatisfiable -> Unsatisfiable") {
    val node1 = unsatisfiableNode
    val node2 = unsatisfiableNode
    val (mergedNode, mergedStore) = mergeTwoNodes(node1, node2)
    assert(mergedNode.isInstanceOf[UnsatisfiableNode[ConstraintES]])
    assert(mergedStore.isEmpty)
  }
}
