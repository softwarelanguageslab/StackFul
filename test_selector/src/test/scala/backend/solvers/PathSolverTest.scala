package backend.solvers

import backend.expression.SymbolicInput
import backend.tree.constraints.event_constraints._
import backend.tree.constraints.EventConstraint
import backend.tree.follow_path.{EventConstraintPathFollower, PathFollowerBasicTest}
import backend.tree.path._

class PathSolverTest extends PathFollowerBasicTest with SolverTest {

  test(
    "Test whether all SymJS-states that can be produced from randomly produced trees are solved correctly") {
    val constraintsPerTTSeq = makeRandomTree(5)
    constraintsPerTTSeq.foreach((constraintsPerTT: Seq[ConstraintsPerTargetTriggered]) => {
      val state = constraintsPerTT.foldLeft(SymJSState.init[EventConstraint])((state, constraintPerTT) => {
        SymJSState[EventConstraint](
          state.eventSequence :+ constraintPerTT._1,
          state.branchSequence ++ constraintPerTT._2.map(
            (tuple) => boolToDirection(tuple._2).toEdgeWithoutTo[EventConstraint]))
      })
      assertPathDefined(state)
      val pathSolver = new PathSolver[EventConstraint](
        reporter.getRoot.get, state, new EventConstraintPathFollower)
      pathSolver.solve() match {
        case NewInput(inputs, _) =>
          constraintsPerTT.foreach((constraintsPerTT: ConstraintsPerTargetTriggered) => {
            constraintsPerTT._2.foreach((tuple: (SymbolicInput, Boolean)) => {
              val (input, isTrue) = tuple
              inputs.get(input) match {
                case Some(n) =>
                  if (isTrue) {
                    assertComputedValueMatches(n, 0)
                  } else {
                    assert(expectInt(n) != 0)
                  }
                case None => assert(false, s"Input $input should have been included in the results")
              }
            })
          })
        case other => assert(false, s"Expected NewInputs but got $other instead")
      }
    })
  }

}
