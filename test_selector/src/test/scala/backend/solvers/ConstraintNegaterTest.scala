package backend.solvers

import backend.modes._
import backend.tree.constraints.{ConstraintAllInOne, EventConstraint, event_constraints}
import backend.tree.constraints.event_constraints.{EventConstraintAllInOne, ProcessesInfo, TargetChosen}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.PrivateMethodTester

class ConstraintNegaterTest extends AnyFunSuite with PrivateMethodTester {

  implicit val mode: Mode = ExploreTreeMode
  implicit val startingProcess: Int = 0

  val allInOne: ConstraintAllInOne[EventConstraint] = EventConstraintAllInOne

  test("Negate a TargetChosen-constraint") {
    val processesInfo = List(10, 20, 15)
    val totalNrOfEvents = processesInfo.sum
    val constraint = TargetChosen(
      0,
      0,
      0,
      Map(0 -> Set(), 1 -> Set(), 2 -> Set()),
      ProcessesInfo(processesInfo, allowCreatingExtraTargets = false),
      startingProcess,
      Nil)
    0.until(totalNrOfEvents - 1)
      .foldLeft(constraint)((constraint, i) => {
        if (i < 10) {
          assert(constraint.processIdChosen == 0)
          assert(constraint.targetIdChosen == i)
          assert(constraint.eventsAlreadyChosen(0).size == i)
        } else if (i < 30) {
          assert(constraint.processIdChosen == 1)
          assert(constraint.targetIdChosen + 10 == i)
          assert(constraint.eventsAlreadyChosen(0).size == 10)
          assert(constraint.eventsAlreadyChosen(1).size == i - 10)
        } else if (i < 45) {
          assert(constraint.processIdChosen == 2)
          assert(constraint.targetIdChosen + 30 == i)
          assert(constraint.eventsAlreadyChosen(0).size == 10)
          assert(constraint.eventsAlreadyChosen(1).size == 20)
          assert(constraint.eventsAlreadyChosen(2).size == i - 30)
        }
        TargetChosen.negate(constraint)
      })
  }

  test("Negate a TargetChosen-constraint, starting from a different process") {
    val processesInfo = List(10, 20, 15)
    val totalNrOfEvents = processesInfo.sum
    val constraint = event_constraints.TargetChosen(
      0,
      2,
      0,
      Map(0 -> Set(), 1 -> Set(), 2 -> Set()),
      ProcessesInfo(processesInfo, allowCreatingExtraTargets = false),
      startingProcess,
      Nil)
    val negatedConstraint = TargetChosen.negate(constraint)
    assert(negatedConstraint.processIdChosen == 0)
    assert(negatedConstraint.targetIdChosen == 0)
    assert(negatedConstraint.eventsAlreadyChosen(2).size == 1)
    1.until(totalNrOfEvents - 1)
      .foldLeft(negatedConstraint)((constraint, i) => {
        if (i < 11) {
          assert(constraint.processIdChosen == 0)
          assert(constraint.targetIdChosen == i - 1)
          assert(constraint.eventsAlreadyChosen(0).size == i - 1)
          assert(constraint.eventsAlreadyChosen(2).size == 1)
        } else if (i < 31) {
          assert(constraint.processIdChosen == 1)
          assert(constraint.targetIdChosen + 11 == i)
          assert(constraint.eventsAlreadyChosen(0).size == 10)
          assert(constraint.eventsAlreadyChosen(1).size == i - 11)
          assert(constraint.eventsAlreadyChosen(2).size == 1)
        } else if (i < 45) {
          assert(constraint.processIdChosen == 2)
          assert(constraint.targetIdChosen + 30 == i)
          assert(constraint.eventsAlreadyChosen(0).size == 10)
          assert(constraint.eventsAlreadyChosen(1).size == 20)
          assert(constraint.eventsAlreadyChosen(2).size == i - 30)
        }
        TargetChosen.negate(constraint)
      })
  }

}
