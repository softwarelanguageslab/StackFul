package backend.tree.merging.use_cases

import backend.SMTSolveProcesses.SMTSolveProcessId
import backend._
import backend.execution_state.store._
import backend.execution_state.{SymbolicStore, _}
import backend.expression._
import backend.expression.Util._
import backend.solvers.SolverTest
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.constraints.event_constraints._
import backend.tree.follow_path._
import backend.tree.merging.SpecialConstraintESNegater
import backend.tree.path.SymJSState
import org.scalatest

abstract class MergingUseCasesTest extends SetUseDebugToTrueTester with SolverTest {

  import scala.language.implicitConversions

  implicit protected def nrToCodeLocation(serial: Int): CodeLocationExecutionState = {
    CodeLocationExecutionState(SerialCodePosition("default_file", serial), FunctionStack.empty, None, 0)
  }

  implicit def branchConstraintToConstraintES(bc: BranchConstraint): ConstraintES = {
    EventConstraintWithExecutionState(bc, CodeLocationExecutionState.dummyExecutionState)
  }

  val startingProcess: SMTSolveProcessId = 0
  val allInOne: ConstraintAllInOne[ConstraintES] = ConstraintESAllInOne
  implicit val toBooleanExpression: ToBooleanExpression[ConstraintES] = allInOne.toBooleanExpression
  implicit val constraintESNegater: ConstraintNegater[ConstraintES] = allInOne.constraintNegater
  val pathFollower = new ConstraintESNoEventsPathFollowerSkipStopTestingTargets()(SpecialConstraintESNegater)

  protected def regularLeafNode: RegularLeafNode[ConstraintES] = RegularLeafNode()
  protected def unexploredNode: UnexploredNode[ConstraintES] = UnexploredNode()

  protected val ite = SymbolicITEExpression
  def checkITEValue[T <: SymbolicExpression, U <: SymbolicExpression](
    expectedITE: SymbolicITEExpression[T, U],
    actualITE: SymbolicExpression
  ): Boolean = {
    actualITE.isInstanceOf[SymbolicITEExpression[T, U]] && iteEquals(
      actualITE.asInstanceOf[SymbolicITEExpression[T, U]], expectedITE)
  }
  protected def assertUnexploredNode(edge: Edge[ConstraintES]): scalatest.Assertion = {
    assert(edge.to == unexploredNode, s"Expected an UnexploredNode, found a ${edge.to}")
  }
  protected def assertLeafNode(edge: Edge[ConstraintES]): scalatest.Assertion = {
    assert(edge.to == regularLeafNode, s"Expected an RegularLeafNode, found a ${edge.to}")
  }
  protected def assertUnsatisfiableNode(edge: Edge[ConstraintES]): scalatest.Assertion = {
    assert(edge.to == UnsatisfiableNode(), s"Expected an UnsatisfiableNode, found a ${edge.to}")
  }
  protected def assertStoreContainsViaMultiplePaths(
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    paths: Iterable[Path],
    variablesValuesPairs: List[(String, SymbolicExpression => Boolean)]
  ): Unit = {
    paths.foreach(assertStoreContains(pathFollower, root, _, variablesValuesPairs))
  }
  protected def assertStoreContains(
    pathFollower: PathFollower[ConstraintES],
    root: SymbolicNode[ConstraintES],
    path: Path,
    variablesValuesPairs: List[(String, SymbolicExpression => Boolean)]
  ): Unit = {
    val pathFollowed = pathFollower.followPathAndArriveAt(root, emptyStore, SymJSState(Nil, path))
    assert(pathFollowed.isDefined)
    val storeFound = pathFollowed.get.store
    assert(storeFound.map.size == variablesValuesPairs.length)
    variablesValuesPairs.foreach(pair => {
      val (variable, valuePred) = pair
      assert(storeFound.map.get(variable).exists(valuePred))
    })
  }
  protected def makeEventPath1(
    x: Option[String],
    processesInfo: ProcessesInfo
  ): (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = {
    val constraint1: ConstraintES = EventConstraintWithExecutionState(
      TargetChosen(0, 0, 0, Map(0 -> Set()), processesInfo, startingProcess, Nil), 0)
    val el1_t: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(constraint1, true)
    val x_t = SymbolicInt(1, x)
    val ac_t = AssignmentsStoreUpdate(List(x.get -> x_t))
    val el2_t: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac_t)
    val store_t: SymbolicStore = Map(x.get -> x_t)
    val sc: ConstraintES = EventConstraintWithExecutionState(
      StopTestingTargets(0, processesInfo.processesInfo, Nil, startingProcess), TargetEndExecutionState(0))
    val el3_t: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(sc, true)
    val bc_f: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(x_t, IntEqual, 10), Nil), 1)
    val el4_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(bc_f, false)
    (store_t, List(el1_t, el2_t, el3_t), List(el4_f))
  }

  protected def makeEventPath2(
    x: Option[String],
    processesInfo: ProcessesInfo
  ): (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = {
    val constraint2: ConstraintES = EventConstraintWithExecutionState(
      TargetChosen(0, 0, 1, Map(0 -> Set(0)), processesInfo, startingProcess, Nil), 0)
    val el1_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(constraint2, true)
    val x_f = SymbolicInt(10, x)
    val ac_f = AssignmentsStoreUpdate(List(x.get -> x_f))
    val el2_f: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac_f)
    val store_f: SymbolicStore = Map(x.get -> x_f)
    val sc_f: ConstraintES = EventConstraintWithExecutionState(
      StopTestingTargets(0, processesInfo.processesInfo, Nil, startingProcess), TargetEndExecutionState(0))
    val el3_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(sc_f, true)
    val bc_t: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(x_f, IntEqual, 10), Nil), 1)
    val el4_t: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(bc_t, true)
    (store_f, List(el1_f, el2_f, el3_f), List(el4_t))
  }

  protected def makeEventPath3(
    x: Option[String],
    processesInfo: ProcessesInfo
  ): (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = {
    val constraint3: ConstraintES = EventConstraintWithExecutionState(
      TargetChosen(0, 0, 2, Map(0 -> Set(0, 1)), processesInfo, startingProcess, Nil), 0)
    val el1_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(constraint3, true)
    val x_f = SymbolicInt(100, x)
    val ac_f = AssignmentsStoreUpdate(List(x.get -> x_f))
    val el2_f: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac_f)
    val store_f: SymbolicStore = Map(x.get -> x_f)
    val sc_f: ConstraintES = EventConstraintWithExecutionState(
      StopTestingTargets(0, processesInfo.processesInfo, Nil, startingProcess), TargetEndExecutionState(0))
    val el3_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(sc_f, true)
    val bc_t: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(x_f, IntEqual, 10), Nil), 1)
    val el4_f: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(bc_t, false)
    (store_f, List(el1_f, el2_f, el3_f), List(el4_f))
  }

  protected def makeGenericEventPath(
    processesInfo: ProcessesInfo,
    tid: Int,
    values: (Int, Int, Int)
  ): (SymbolicStore, PathConstraintWithStoreUpdates[ConstraintES], PathConstraintWithStoreUpdates[ConstraintES]) = {
    val constraint1: ConstraintES = EventConstraintWithExecutionState(
      TargetChosen(0, 0, tid, Map(0 -> 0.until(tid).toSet), processesInfo, startingProcess, Nil), 0)
    val el1_t: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(constraint1, true)
    val (xValue, yValue, zValue) = values
    val x_1 = SymbolicInt(xValue, Some("x"))
    val y_1 = SymbolicInt(yValue, Some("y"))
    val z_1 = SymbolicInt(zValue, Some("z"))
    val ac1_1 = AssignmentsStoreUpdate(List("x" -> x_1))
    val ac2_1 = AssignmentsStoreUpdate(List("x" -> x_1, "y" -> y_1))
    val ac3_1 = AssignmentsStoreUpdate(List("x" -> x_1, "y" -> y_1, "z" -> z_1))
    val el2_1: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac1_1)
    val el3_1: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac2_1)
    val el4_1: PCElementWithStoreUpdate[ConstraintES] = StoreUpdatePCElement(ac3_1)

    val store_1: SymbolicStore = Map("x" -> x_1, "y" -> y_1, "z" -> z_1)
    val sc: ConstraintES = EventConstraintWithExecutionState(
      StopTestingTargets(0, processesInfo.processesInfo, Nil, startingProcess), TargetEndExecutionState(0))
    val el5_1: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(sc, true)
    val plusExp = ArithmeticalVariadicOperationExpression(IntPlus, List(x_1, y_1))
    val bc_1: ConstraintES = EventConstraintWithExecutionState(
      BranchConstraint(RelationalExpression(plusExp, IntEqual, z_1), Nil), 1)
    val el6_1: PCElementWithStoreUpdate[ConstraintES] = ConstraintWithStoreUpdate(
      bc_1, false) // Assume this constraint to always be false
    (store_1, List(el1_t, el2_1, el3_1, el4_1, el5_1), List(el6_1))
  }
}
