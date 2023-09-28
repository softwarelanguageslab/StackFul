package backend.tree.merging.use_cases.automatically_generated

import backend.expression.SymbolicInputInt

trait GeneratedTreeNode
case class To(node: GeneratedTreeNode, storeUpdate: Option[GenerateAssignmentsStoreUpdate[Int]])
case class LeafNode(value: String) extends GeneratedTreeNode
case class BranchNode(input: SymbolicInputInt, value: Int, left: To, right: To) extends GeneratedTreeNode
