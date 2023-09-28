package backend.tree.follow_path

import backend.tree.constraints.EventConstraint
import backend.tree.path.{SymJSState, boolToDirection}

class PathFollowerTest extends PathFollowerBasicTest {
  test(
    "Test some paths created while constructing a tree with three possible targets, where only then-branches are followed")
  {
    makeTree(5)

    assertPathDefined(
      SymJSState(
        List(TargetTriggered(0, 0)), 1.to(1).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))
    assertPathDefined(
      SymJSState(
        List(TargetTriggered(1, 0)), 1.to(2).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))
    assertPathDefined(
      SymJSState(
        List(TargetTriggered(1, 1)), 1.to(3).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))

    assertPathDefined(
      SymJSState(
        List(TargetTriggered(0, 0), TargetTriggered(1, 1)),
        1.to(4).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))
    assertPathDefined(
      SymJSState(
        List(TargetTriggered(1, 0), TargetTriggered(0, 0)),
        1.to(3).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))
    assertPathDefined(
      SymJSState(
        List(TargetTriggered(1, 1), TargetTriggered(1, 1)),
        1.to(6).map(_ => ThenDirection.toEdgeWithoutTo[EventConstraint]).toList))
  }

  test("Test all paths created while randomly constructing a tree with three possible targets") {
    val constraintsPerTTSeq = makeRandomTree(5)
    constraintsPerTTSeq.foreach((constraintsPerTT: Seq[ConstraintsPerTargetTriggered]) => {
      val state = constraintsPerTT.foldLeft(SymJSState.init[EventConstraint])((state, constraintPerTT) => {
        SymJSState(
          state.eventSequence :+ constraintPerTT._1,
          state.branchSequence ++ constraintPerTT._2.map(
            tuple => boolToDirection(tuple._2).toEdgeWithoutTo[EventConstraint]))
      })
      assertPathDefined(state)
    })
  }
}