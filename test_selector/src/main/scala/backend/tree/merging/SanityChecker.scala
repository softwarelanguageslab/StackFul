//package backend.tree.merging
//
//import backend.expression.{RelationalExpression, StringEqual, SymbolicString}
//import backend.tree.constraints.basic_constraints.BranchConstraint
//import backend.tree.{BranchSymbolicNode, Edge, ElseEdge, RegularLeafNode, SingleChildNode, SymbolicNode, ThenEdge, UnexploredNode, UnsatisfiableNode}
//import backend.tree.constraints.constraint_with_execution_state.{ConstraintES, EventConstraintWithExecutionState}
//import backend.tree.constraints.event_constraints.TargetChosen
//
//object SanityChecker {
//
//  private def checkEdge[T](node: SymbolicNode[T, ConstraintES], edge: Edge[T, ConstraintES]): Unit = {
//    if (edge.storeConstraints.isEmpty) {
//      val child = edge.to
//      child match {
//        case BranchSymbolicNode(EventConstraintWithExecutionState(BranchConstraint(RelationalExpression(SymbolicString("function", _), StringEqual, SymbolicString("object", _),_), _), _), _, _, _) =>
//        case BranchSymbolicNode(EventConstraintWithExecutionState(_: TargetChosen, _), _, _, _) =>
//        case _ => throw new Exception(s"Node $node has empty store constraints with non-empty child node: $edge")
//      }
//    }
//    check(edge.to)
//  }
//
//  def check[T](root: SymbolicNode[T, ConstraintES]): Unit = root match {
//    case bsn@BranchSymbolicNode(_, left, right, _) =>
//      left.to match {
//        case UnsatisfiableNode() | UnexploredNode() | RegularLeafNode() =>
//        case _ => checkEdge(bsn, left)
//      }
//      right.to match {
//        case UnsatisfiableNode() | UnexploredNode() | RegularLeafNode() =>
//        case _ => checkEdge(bsn, right)
//      }
//    case scn@SingleChildNode(_, child, _) => checkEdge(scn, child)
//    case _ =>
//  }
//}
