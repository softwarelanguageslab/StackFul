package backend.solvers.solve_events

import backend.tree.constraints.Constraint
import backend.tree.constraints.constraint_with_execution_state.EventConstraintWithExecutionState
import backend.tree.constraints.event_constraints.TargetChosen

import scala.annotation.tailrec

object NaiveEventSolver {

  def getEventSequence(constraints: List[Constraint]): List[(Int, Int)] = {
    /*
     * Loop over all TargetChosen-constraints in the list. Older TargetChosen-constraints (i.e., constraints with a
     * lower id) should appear earlier in the list. The last TargetChosen-constraint of a particular id is the one
     * whose processId and targetId will be selected for that particular id.
     * The loop therefore keeps track of the last TargetChosen. If it finds that the current TC has a higher id than the
     * previous one, add the processId and targetId of the last TC to the acc-list.
     */
    @tailrec
    def loop(
      constraints: List[Constraint],
      lastTargetChosen: Option[TargetChosen],
      acc: List[(Int, Int)]
    ): List[(Int, Int)] = {
      constraints.headOption match {
        case None =>
          val updatedAcc = lastTargetChosen match {
            case None => acc
            case Some(tc) => (tc.processIdChosen, tc.targetIdChosen) :: acc
          }
          updatedAcc.reverse
        case Some(tc: TargetChosen) => lastTargetChosen match {
          case None =>
            assert(acc.isEmpty)
            loop(constraints.tail, Some(tc), acc)
          case Some(prevTc) if prevTc.id == tc.id => loop(constraints.tail, Some(tc), acc)
          case Some(prevTc) if prevTc.id < tc.id =>
            loop(
              constraints.tail,
              Some(tc),
              (prevTc.processIdChosen, prevTc.targetIdChosen) :: acc)
          case Some(prevTc) if prevTc.id > tc.id =>
            throw new Exception(
              s"Should not happen: newer target before an older target in list of constraints: ${prevTc.id} and ${tc.id}")
        }
        case Some(EventConstraintWithExecutionState(tc: TargetChosen, _)) => lastTargetChosen match {
          case None =>
            assert(acc.isEmpty)
            loop(constraints.tail, Some(tc), acc)
          case Some(prevTc) if prevTc.id == tc.id => loop(constraints.tail, Some(tc), acc)
          case Some(prevTc) if prevTc.id < tc.id =>
            loop(
              constraints.tail,
              Some(tc),
              (prevTc.processIdChosen, prevTc.targetIdChosen) :: acc)
          case Some(prevTc) if prevTc.id > tc.id =>
            throw new Exception(
              s"Should not happen: newer target before an older target in list of constraints: ${prevTc.id} and ${tc.id}")
        }
        case Some(_) => loop(constraints.tail, lastTargetChosen, acc)
      }
    }
    loop(constraints, None, Nil)
  }

}
