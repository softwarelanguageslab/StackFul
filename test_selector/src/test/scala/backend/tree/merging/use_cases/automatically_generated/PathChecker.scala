package backend.tree.merging.use_cases.automatically_generated

import backend.Path
import backend.expression.{CanDoEqualityCheck, CheckExpressionAsserter}
import backend.logging.Logger
import backend.tree.SymbolicNode
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.follow_path.PathFollower

import scala.util.Random

object PathChecker {
  protected var printIdentifier: Boolean = false
  def setPrintIdentifier(shouldPrint: Boolean): Unit = {
    printIdentifier = shouldPrint
  }
}

class PathChecker[T: CanDoEqualityCheck](val pathFollower: PathFollower[ConstraintES]) {
  protected var newChecks: List[(Path, String, T)] = Nil
  protected var oldChecks: List[(Path, String, T)] = Nil

  def addToChecks(path: Path, identifier: String, expectedValue: T): Unit = {
    newChecks :+= (path, identifier, expectedValue)
  }
  def doAllChecks(root: SymbolicNode[ConstraintES], processesInfo: List[Int]): Unit = {
    checkPaths(root, newChecks, processesInfo)
    checkPaths(root, oldChecks, processesInfo)
    oldChecks ++= newChecks
    newChecks = Nil
  }

  protected def printIdentifier(identifier: String): Unit = {
    if (PathChecker.printIdentifier) {
      Logger.d(s"Testing identifier $identifier")
    }
  }

  protected def checkPaths(root: SymbolicNode[ConstraintES], checks: List[(Path, String, T)], processesInfo: List[Int] = Nil): Unit = {
    checks.foreach(triple => {
      val (path, identifier, expectedValue) = triple
      printIdentifier(identifier)
      CheckExpressionAsserter.assertExpressionEquals(pathFollower, root, path, identifier, expectedValue, processesInfo)
    })
  }
}

class OnlyNewPathsChecked[T: CanDoEqualityCheck](
  pathFollower: PathFollower[ConstraintES]
) extends PathChecker[T](
  pathFollower) {
  override def doAllChecks(root: SymbolicNode[ConstraintES], processesInfo: List[Int] = Nil): Unit = {
    super.doAllChecks(root, processesInfo)
    oldChecks = Nil
  }
}

class CheckRandomOldPaths[T: CanDoEqualityCheck](
  pathFollower: PathFollower[ConstraintES]
) extends PathChecker[T](pathFollower) {
  protected val maxPathsToStore: Int = 30
  protected val pathsToRecheck: Int = 10
  override def doAllChecks(root: SymbolicNode[ConstraintES], processesInfo: List[Int] = Nil): Unit  = {
    checkPaths(root, newChecks, processesInfo)
    // Want to prevent new paths from possibly being checked twice,
    // as this would take up a spot of an old path which could have been rechecked instead.
    oldChecks = Random.shuffle(oldChecks).take(maxPathsToStore)
    val oldPathsToRecheckNow = Random.shuffle(oldChecks).take(pathsToRecheck)
    checkPaths(root, oldPathsToRecheckNow, processesInfo)
    oldChecks ++= newChecks
    newChecks = Nil
  }
}
