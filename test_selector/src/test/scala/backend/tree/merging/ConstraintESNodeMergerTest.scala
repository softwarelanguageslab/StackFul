package backend.tree.merging

import backend.SetUseDebugToTrueTester
import backend.execution_state._
import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._
import backend.expression.Util._
import backend.TestConfigs._
import backend.modes.MergingMode
import backend.reporters.ConstraintESReporter
import backend.solvers.ActionSuccessful
import backend.tree._
import backend.tree.constraints._
import backend.tree.constraints.basic_constraints._
import backend.tree.constraints.constraint_with_execution_state._
import backend.tree.search_strategy.TreePath

class ConstraintESNodeMergerTest extends SetUseDebugToTrueTester {

  import scala.language.implicitConversions

  implicit private def toThen(node: SymbolicNode[ConstraintES]): ThenEdge[ConstraintES] = {
    ThenEdge.to(node)
  }
  implicit private def toElse(node: SymbolicNode[ConstraintES]): ElseEdge[ConstraintES] = {
    ElseEdge.to(node)
  }
  val allInOne: ConstraintAllInOne[ConstraintES] = ConstraintESAllInOne
  implicit val toBooleanExpression: ToBooleanExpression[ConstraintES] = allInOne.toBooleanExpression
  implicit val constraintESNegater: ConstraintNegater[ConstraintES] = allInOne.constraintNegater
  val nodeMerger: ConstraintESNodeMerger = new ConstraintESNodeMerger(
    treeLogger, ConstraintESReporter.newReporter(MergingMode))
  val defaultStore: SymbolicStore = emptyStore
  private val leafNode = RegularLeafNode[ConstraintES]()
  private val unexploredNode = UnexploredNode[ConstraintES]()
  private def newVirtualPathStore: VirtualPathStorePerIdentifier = new VirtualPathStorePerIdentifier

  import scala.language.implicitConversions

  implicit def branchConstraintToConstraintES(bc: BranchConstraint): ConstraintES = {
    EventConstraintWithExecutionState(bc, CodeLocationExecutionState.dummyExecutionState)
  }

  test("Empty list of constraints should return SymbolicBool(true)") {
    val actual = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(Nil: List[ConstraintES])
    assert(actual == SymbolicBool(b = true))
  }

  test(
    "Singleton lists consisting of a BranchConstraint should result in the single expression") {
    val exp = SymbolicBool(b = false, Some("someIdentifier"))
    val list: List[ConstraintES] = List(BranchConstraint(exp, Nil))
    val actual = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(list)
    assert(actual == exp)
  }

  test(
    "Non-empty lists should join expressions together as and-expressions. Test list consisting of two constraints") {
    val exp1 = SymbolicBool(b = false, Some("someIdentifier"))
    val exp2 = RelationalExpression(
      SymbolicInt(10, Some("id1")),
      IntNonEqual,
      SymbolicInt(2, Some("id2")),
      Some("id3"))
    val list: List[ConstraintES] = List(
      toConstraintES(basic_constraints.BranchConstraint(exp1, Nil)),
      toConstraintES(basic_constraints.BranchConstraint(exp2, Nil)))
    val actual = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(list)
    assert(
      actual == LogicalBinaryExpression(
        exp1,
        LogicalAnd,
        exp2,
        Some("someIdentifier")))
  }

  test("Replacing constraint 1") {
    val x = Some("x")
    val assignment = Map(x.get -> SymbolicInt(20, x))
    val store: SymbolicStore = new SymbolicStore(assignment)
    val leafNode = RegularLeafNode[ConstraintES]()
    // bc2: i0 + 1 = 10
    val bc2 = BranchConstraint(
      RelationalExpression(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInputInt(RegularId(1, 0)),
            SymbolicInt(1, x))),
        IntEqual,
        SymbolicInt(10)
      ), Nil)
    // b2: (bc2, L, L)
    val b2 = BranchSymbolicNode[ConstraintES](
      bc2,
      ThenEdge.to(leafNode),
      ElseEdge.to(leafNode)
    )
    // rootBc: true
    val mergedConstraint = nodeMerger.storeMerger.replaceConstraintAndAddStoreUpdate(store, b2, Set(), List("x"))
    mergedConstraint match {
      case EventConstraintWithExecutionState(BranchConstraint(RelationalExpression(left, op, right, id), _), _) =>
        assert(id.isEmpty)
        assert(right == SymbolicInt(10))
        assert(op == IntEqual)
        left match {
          case ArithmeticalVariadicOperationExpression(op, args, id) =>
            assert(op == IntPlus)
            assert(id.isEmpty)
            assert(args.size == 2)
            assert(args.head == SymbolicInputInt(RegularId(1, 0)))
            assert(args(1) == SymbolicInt(20, x))
          case _ =>
            assert(
              false,
              s"Right should have been an ArithmeticalSymbolicExpression, but is $right")
        }
      case _ =>
        assert(false, s"Merged constraint should have contained a RelationalSymbolicExp, but is $mergedConstraint")
    }
  }

  test("Experimenting with merging 1") {
    val x = Some("x")
    val assignment = Map(x.get -> SymbolicInt(20, x))
    val store: SymbolicStore = new SymbolicStore(assignment)
    val leafNode = RegularLeafNode[ConstraintES]()
    // bc2: i0 + 1 = 10
    val bc2 = BranchConstraint(
      RelationalExpression(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInputInt(RegularId(1, 0)),
            SymbolicInt(1, x))),
        IntEqual,
        SymbolicInt(10)
      ), Nil)
    // b2: (bc2, L, L)
    val b2 = BranchSymbolicNode[ConstraintES](
      bc2,
      ThenEdge.to(leafNode),
      ElseEdge.to(leafNode)
    )
    // rootBc: true
    val rootBc = BranchConstraint(SymbolicBool(b = true), List(AssignmentsStoreUpdate(assignment.toList)))
    val completeSuffixPath: List[ConstraintES] = List(rootBc)
    val mergedConstraint = nodeMerger.storeMerger.produceMergedConstraint(
      CodeLocationExecutionState.dummyExecutionState,
      CodeLocationExecutionState.dummyExecutionState, store, completeSuffixPath, "T", "E", b2, b2, Set())
    mergedConstraint match {
      case EventConstraintWithExecutionState(BranchConstraint(RelationalExpression(left, op, right, id), _), _) =>
        assert(id.isEmpty)
        assert(right == SymbolicInt(10))
        assert(op == IntEqual)
        left match {
          case ArithmeticalVariadicOperationExpression(op, args, id) =>
            assert(op == IntPlus)
            assert(id.isEmpty)
            assert(args.size == 2)
            assert(args.head == SymbolicInputInt(RegularId(1, 0)))
            val predExp = SymbolicBool(b = true)
            assert(args(1) == SymbolicITEExpression(predExp, SymbolicInt(1, x), SymbolicInt(20, x), x))
          case _ =>
            assert(
              false,
              s"Right should have been an ArithmeticalSymbolicExpression, but is $right")
        }
      case _ =>
        assert(false, s"Merged constraint should have contained a RelationalSymbolicExp, but is $mergedConstraint")
    }
  }

  test("Replacing constraint 2") {
    val x = Some("x")
    val store: SymbolicStore = Map(x.get -> SymbolicInt(20, x))
    val leafNode = RegularLeafNode[ConstraintES]()
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInputInt(RegularId(1, 0)),
            SymbolicInt(1, x))),
        IntEqual,
        SymbolicInt(10)
      ), Nil)
    val b2 = BranchSymbolicNode[ConstraintES](
      bc2,
      ThenEdge.to(leafNode),
      ElseEdge.to(leafNode)
    )
    val bc1 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(RegularId(0, 0)),
        IntEqual,
        SymbolicInt(0)), Nil)
    val rootBc = basic_constraints.BranchConstraint(SymbolicBool(b = true), Nil)
    val completeSuffixPath: List[ConstraintES] = List(rootBc, bc1.negate)
    val mergedConstraint = nodeMerger.storeMerger.produceMergedConstraint(
      CodeLocationExecutionState.dummyExecutionState,
      CodeLocationExecutionState.dummyExecutionState, store, completeSuffixPath, "T", "E", b2, b2, Set())
    mergedConstraint match {
      case EventConstraintWithExecutionState(BranchConstraint(RelationalExpression(left, op, right, id), _), _) =>
        assert(id.isEmpty)
        assert(right == SymbolicInt(10))
        assert(op == IntEqual)
        left match {
          case ArithmeticalVariadicOperationExpression(op, args, id) =>
            assert(op == IntPlus)
            assert(id.isEmpty)
            assert(args.size == 2)
            assert(args.head == SymbolicInputInt(RegularId(1, 0)))
            val predExp = LogicalBinaryExpression(SymbolicBool(b = true), LogicalAnd, bc1.negate.exp)
            assert(args(1) == SymbolicITEExpression(predExp, SymbolicInt(1, x), SymbolicInt(20, x), x))
          case _ =>
            assert(
              false,
              s"Right should have been an ArithmeticalSymbolicExpression, but is $right")
        }
      case _ =>
        assert(false, s"Merged constraint should have contained a RelationalSymbolicExp, but is $mergedConstraint")
    }
  }

  test("Experimenting with merging 2") {
    val x = Some("x")
    val store: SymbolicStore = Map(x.get -> SymbolicInt(20, x))
    val leafNode = RegularLeafNode[ConstraintES]()
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        ArithmeticalVariadicOperationExpression(
          IntPlus,
          List(
            SymbolicInputInt(RegularId(1, 0)),
            SymbolicInt(1, x))),
        IntEqual,
        SymbolicInt(10)
      ), Nil)
    val b2 = BranchSymbolicNode[ConstraintES](
      bc2,
      ThenEdge.to(leafNode),
      ElseEdge.to(leafNode)
    )
    val bc1 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInputInt(RegularId(0, 0)),
        IntEqual,
        SymbolicInt(0)), Nil)
    val rootBc = basic_constraints.BranchConstraint(SymbolicBool(b = true), Nil)
    val completeSuffixPath: List[ConstraintES] = List(rootBc, bc1.negate)
    val mergedConstraint = nodeMerger.storeMerger.produceMergedConstraint(
      CodeLocationExecutionState.dummyExecutionState,
      CodeLocationExecutionState.dummyExecutionState, store, completeSuffixPath, "T", "E", b2, b2, Set())
    mergedConstraint match {
      case EventConstraintWithExecutionState(BranchConstraint(RelationalExpression(left, op, right, id), _), _) =>
        assert(id.isEmpty)
        assert(right == SymbolicInt(10))
        assert(op == IntEqual)
        left match {
          case ArithmeticalVariadicOperationExpression(op, args, id) =>
            assert(op == IntPlus)
            assert(id.isEmpty)
            assert(args.size == 2)
            assert(args.head == SymbolicInputInt(RegularId(1, 0)))
            val predExp = LogicalBinaryExpression(SymbolicBool(b = true), LogicalAnd, bc1.negate.exp)
            assert(args(1) == SymbolicITEExpression(predExp, SymbolicInt(1, x), SymbolicInt(20, x), x))
          case _ =>
            assert(
              false,
              s"Right should have been an ArithmeticalSymbolicExpression, but is $right")
        }
      case _ =>
        assert(false, s"Merged constraint should have contained a RelationalSymbolicExp, but is $mergedConstraint")
    }
  }
  private def createSomePath: List[ConstraintES] = {
    val bc1 = basic_constraints.BranchConstraint(SymbolicBool(b = true), Nil)
    val bc2 = basic_constraints.BranchConstraint(
      RelationalExpression(
        SymbolicInt(1),
        IntEqual,
        SymbolicInt(2)), Nil)
    val fc3 = basic_constraints.BranchConstraint(
      LogicalUnaryExpression(
        LogicalNot,
        RelationalExpression(
          SymbolicInt(5),
          IntGreaterThan,
          SymbolicInt(2))), Nil)
    List(toConstraintES(bc1), toConstraintES(bc2), toConstraintES(fc3))
  }

  test("An empty symbolic store should always ensure that the merged exp remains unchanged") {
    val store: SymbolicStore = emptyStore
    val exp = RelationalExpression(
      SymbolicInt(10),
      IntEqual,
      SymbolicInt(10))
    val mergedExp = nodeMerger.storeMerger.produceMergedExp(
      CodeLocationExecutionState.dummyExecutionState, store, createSomePath, exp, Nil, Nil, newVirtualPathStore)
    assert(mergedExp == exp)
  }

  test("Expressions using identifiers not present in the store should not be changed") {
    val x = Some("x")
    val store: SymbolicStore = Map("y" -> SymbolicInt(123, Some("y")), "z" -> SymbolicString("abc", Some("z")))
    val exp = RelationalExpression(
      SymbolicInt(10, x),
      IntEqual,
      SymbolicInt(10))
    val mergedExp = nodeMerger.storeMerger.produceMergedExp(
      CodeLocationExecutionState.dummyExecutionState, store, createSomePath, exp, Nil, Nil, newVirtualPathStore)
    assert(mergedExp == exp)
  }

  test("( [true] ? *( [i1+1=10] ? x : x )* : *( [i1+20=10] ? x : x )* )") {
    val x = Some("x")
    val state2 = CodeLocationExecutionState(SerialCodePosition("f", 1), FunctionStack.empty, None, 0)

    def plusExp(xValue: Int): ArithmeticalVariadicOperationExpression = {
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInputInt(RegularId(1, 0)), SymbolicInt(xValue, x)))
    }
    def bc2Exp(xValue: Int) = RelationalExpression(plusExp(xValue), IntEqual, SymbolicInt(10))
    def makeNode(xValue: Int): BranchSymbolicNode[ConstraintES] = {
      val bc2 = EventConstraintWithExecutionState(BranchConstraint(bc2Exp(xValue), Nil), state2)
      BranchSymbolicNode[ConstraintES](bc2, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
    }

    val rootBc = EventConstraintWithExecutionState(
      BranchConstraint(SymbolicBool(true), Nil),
      CodeLocationExecutionState.dummyExecutionState)
    val nodeT = makeNode(1)
    val thenEdge = ThenEdge(List(AssignmentsStoreUpdate(List(("x", SymbolicInt(1, x))))), nodeT)
    val nodeF = makeNode(20)
    val elseEdge = ElseEdge(List(AssignmentsStoreUpdate(List(("x", SymbolicInt(20, x))))), nodeF)
    val root = BranchSymbolicNode(rootBc, thenEdge, elseEdge)
    val pathConstraintToMerge = TreePath.init(root)._1.addElseBranch(nodeF, elseEdge).finishedViaElse.init
    // Doesn't matter for this test whether pathConstraintToMerge leads to then or else node
    val mergeResult = nodeMerger.mergeWorkListStates(root, pathConstraintToMerge, state2)
    assert(mergeResult == ActionSuccessful)
    assert(root.thenBranch.to == root.elseBranch.to)
    root.thenBranch.to match {
      case BranchSymbolicNode(bc, thenBranch, elseBranch) =>
        assert(thenBranch.to == unexploredNode)
        assert(elseBranch.to == unexploredNode)
        bc match {
          case EventConstraintWithExecutionState(bc, _) => bc match {
            case BranchConstraint(exp, _) => exp match {
              case RelationalExpression(left, IntEqual, SymbolicInt(10, _), _) =>
                left match {
                  case ArithmeticalVariadicOperationExpression(IntPlus, List(arg1, arg2), _) =>
                    assert(arg1 == SymbolicInputInt(RegularId(1, 0)))
                    val expectedIte = SymbolicITEExpression(
                      SymbolicBool(true), SymbolicInt(1, x), SymbolicInt(20, x), x)
                    assert(arg2.isInstanceOf[SymbolicITEExpression[SymbolicInt, SymbolicInt]])
                    val ite = arg2.asInstanceOf[SymbolicITEExpression[SymbolicInt, SymbolicInt]]
                    assert(iteEquals(ite, expectedIte))
                  case other => assert(false, s"Expected a ArithmeticalVariadicOperationExpression() but got $other")
                }
              case other => assert(false, s"Expected a RelationalExpression() but got $other")
            }
            case _ => assert(false, "Constraint should be a BranchConstraint")
          }
        }
      case _ => assert(false, "then- and else-branch should be a BranchSymbolicNode")
    }
  }

  test("( [i0=10] ? *( [1_x<2] ? x : x )* : *( [2_x<2] ? x : x )* )") {
    /*
     if (i0 == 10) {
       x = 1;
     else
       x = 2;
     if (i1 <= 2)
       y = 3;
     else
       y = 4
     */

    val x = Some("x")
    val state = CodeLocationExecutionState(SerialCodePosition("f", 1), FunctionStack.empty, None, 0)

    val nodeTExp = RelationalExpression(SymbolicInt(1, x), IntLessThanEqual, SymbolicInt(2))
    val nodeTBc = EventConstraintWithExecutionState(BranchConstraint(nodeTExp, Nil), state)
    val nodeT = BranchSymbolicNode(nodeTBc, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
    val thenEdge = ThenEdge(List(AssignmentsStoreUpdate(List((x.get, SymbolicInt(1, x))))), nodeT)

    val nodeFExp = RelationalExpression(SymbolicInt(2, x), IntLessThanEqual, SymbolicInt(2))
    val nodeFBc = EventConstraintWithExecutionState(BranchConstraint(nodeFExp, Nil), state)
    val nodeF = BranchSymbolicNode(nodeFBc, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
    val elseEdge = ElseEdge(List(AssignmentsStoreUpdate(List((x.get, SymbolicInt(2, x))))), nodeF)

    val rootExp = RelationalExpression(SymbolicInputInt(RegularId(0, 0)), IntEqual, SymbolicInt(10))
    val rootBc: ConstraintES = BranchConstraint(rootExp, Nil)
    val root = BranchSymbolicNode(rootBc, thenEdge, elseEdge)

    // Doesn't matter for this test whether pathConstraintToMerge leads to then or else node
    val pathConstraintToMerge = TreePath.init(root)._1.addElseBranch(nodeF, elseEdge).finishedViaElse.init
    val mergeResult = nodeMerger.mergeWorkListStates(root, pathConstraintToMerge, state)
    assert(mergeResult == ActionSuccessful)

    assert(root.thenBranch.to == root.elseBranch.to)
    root.thenBranch.to match {
      case thenNode: BranchSymbolicNode[ConstraintES] => thenNode.constraint match {
        case EventConstraintWithExecutionState(BranchConstraint(exp, _), _) => exp match {
          case RelationalExpression(left, op, right, _) =>
            assert(op == IntLessThanEqual)
            assert(right == SymbolicInt(2))
            val castedLeft = left.asInstanceOf[SymbolicITEExpression[SymbolicInt, SymbolicInt]]
            val negater = implicitly[ConstraintNegater[ConstraintES]]
            val predExp1 = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(rootBc))
            val predExp2 = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(negater.negate(rootBc)))
            assert(iteEquals(castedLeft, SymbolicITEExpression(predExp1, SymbolicInt(1, x), SymbolicInt(2, x), x)) ||
              iteEquals(castedLeft, SymbolicITEExpression(predExp2, SymbolicInt(2, x), SymbolicInt(1, x), x)))
          //            assert(left == SymbolicITEExpression(predExp1, SymbolicInt(1, x), SymbolicInt(2, x), x) ||
          //                   left == SymbolicITEExpression(predExp2, SymbolicInt(2, x), SymbolicInt(1, x), x))
          case other => assert(false, s"Expected a RelationalExpression but got $other")
        }
        case other => assert(false, s"Expected a BasicConstraintWithExecutionState(BranchConstraint()) but got $other")
      }
        assert(thenNode.thenBranch.to == unexploredNode)
        assert(thenNode.elseBranch.to == unexploredNode)
      case other => assert(false, s"Expected a BranchSymbolicNode[ConstraintES] but got $other")
    }
  }

  test("( [i0=10] ? ( [i1=20] ? x : *( [1x+4y=30] ? x : x )* ) : ( [i1=20] ? x *( [2x+4y=30] ? x : x )* ) )") {
    /*
     if (i0 == 10) {
       x = 1;
     else
       x = 2;
     if (i1 == 20)
       y = 3;
     else
       y = 4
     if (x + y == 30)
       z = 5;
     else
       z = 6;
     */

    val x = Some("x")
    val y = Some("y")
    val state = CodeLocationExecutionState(SerialCodePosition("f", 1), FunctionStack.empty, None, 0)

    val nodeFFExp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(2, x), SymbolicInt(4, y))), IntEqual,
      SymbolicInt(30))
    val nodeFFBc = EventConstraintWithExecutionState(BranchConstraint(nodeFFExp, Nil), state)
    val nodeFF = BranchSymbolicNode(nodeFFBc, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
    val ffEdge = ElseEdge(List(AssignmentsStoreUpdate(List(y.get -> SymbolicInt(4, y)))), nodeFF)

    val i1 = SymbolicInputInt(RegularId(1, 0))
    val nodeFExp = RelationalExpression(i1, IntEqual, SymbolicInt(20))
    val nodeFBc: ConstraintES = BranchConstraint(nodeFExp, Nil)
    val nodeF = BranchSymbolicNode(nodeFBc, ThenEdge.to(unexploredNode), ffEdge)
    val fEdge = ElseEdge(List(AssignmentsStoreUpdate(List(x.get -> SymbolicInt(2, x)))), nodeF)

    val nodeTFExp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(1, x), SymbolicInt(4, y))), IntEqual,
      SymbolicInt(30))
    val nodeTFBc = EventConstraintWithExecutionState(BranchConstraint(nodeTFExp, Nil), state)
    val nodeTF = BranchSymbolicNode(nodeTFBc, ThenEdge.to(unexploredNode), ElseEdge.to(unexploredNode))
    val tfEdge = ElseEdge(List(AssignmentsStoreUpdate(List(y.get -> SymbolicInt(4, y)))), nodeTF)

    val nodeTBc = nodeFBc
    val nodeT = BranchSymbolicNode(nodeTBc, ThenEdge.to(unexploredNode), tfEdge)
    val tEdge = ThenEdge(List(AssignmentsStoreUpdate(List(x.get -> SymbolicInt(1, x)))), nodeT)

    val i0 = SymbolicInputInt(RegularId(0, 0))
    val rootExp = RelationalExpression(i0, IntEqual, SymbolicInt(10))
    val rootBc: ConstraintES = BranchConstraint(rootExp, Nil)
    val root = BranchSymbolicNode(rootBc, tEdge, fEdge)

    // Doesn't matter for this test to which node pathConstraintToMerge leads
    val pathConstraintToMerge = TreePath.init(root)._1.addElseBranch(nodeF, fEdge).
      addElseBranch(nodeFF, ffEdge).finishedViaElse.init
    val mergeResult = nodeMerger.mergeWorkListStates(root, pathConstraintToMerge, state)
    assert(mergeResult == ActionSuccessful)

    assert(root.thenBranch.to == nodeT)
    assert(root.elseBranch.to == nodeF)
    assert(nodeT.thenBranch.to == unexploredNode)

    assert(nodeT.elseBranch == nodeF.elseBranch) // These nodes should have been merged
    nodeT.elseBranch.to match {
      case nodeFT: BranchSymbolicNode[ConstraintES] =>
        assert(nodeFT.thenBranch.to == unexploredNode)
        assert(nodeFT.elseBranch.to == unexploredNode)
        nodeFT.constraint match {
          case EventConstraintWithExecutionState(BranchConstraint(exp, _), state2) =>
            assert(state2 == state)
            exp match {
              case RelationalExpression(left, op, right, _) =>
                assert(op == IntEqual)
                assert(right == SymbolicInt(30))
                left match {
                  case ArithmeticalVariadicOperationExpression(op, args, _) =>
                    assert(op == IntPlus)

                    val predExp1 = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(rootBc))
                    val expected = SymbolicITEExpression(predExp1, SymbolicInt(1, x), SymbolicInt(2, x), x)
                    //                    assert(args == List(expected, SymbolicInt(4, y)) ||
                    //                           args == List(SymbolicITEExpression(predExp2, SymbolicInt(2, x), SymbolicInt(1, x), x), SymbolicInt(4, y)))
                    assert(args.length == 2)
                    CheckITEExpressionAsserter.checkITEExpression(List((i0, 10), (i1, 21)), args(1), 4)
                    assert(args.head.isInstanceOf[SymbolicITEExpression[SymbolicInt, SymbolicInt]])
                    val castedHead = args.head.asInstanceOf[SymbolicITEExpression[SymbolicInt, SymbolicInt]]
                    assert(iteEquals(castedHead, expected), s"Expected a $expected, but got $castedHead")
                }
              case other => assert(false, s"Expected a RelationalExpression but got $other")
            }
          case other => assert(
            false, s"Expected a BasicConstraintWithExecutionState(BranchConstraint()) but got $other")
        }
      case other => assert(false, s"Expected a BranchSymbolicNode[ConstraintES] but got $other")
    }
  }

  test("( [i0=10] ? " + // root
    "( [i1=20] ? " + // nodeT
    "x : " + // nodeTT
    "*( [1x+4y=30] ? " + // nodeTF
    "( [5z=40] ? L : L ) : " + // nodeTFT
    "x )* ) : " + // nodeTFF
    "( [i1 = 20] ? " + // nodeF
    "x " + // nodeFT
    "*( [2x+4y=30] ? x : x )* ) )") { // nodeFF
    /*
     if (i0 == 10) {
       x = 1;
     else
       x = 2;
     if (i1 == 20)
       y = 3;
     else
       y = 4
     if (x + y == 30)
       z = 5;
     else
       z = 6;
     if (z == 40)
       // nothing, though unsatisfiable
     else
       // nothing
     */

    val x = Some("x")
    val y = Some("y")
    val z = Some("z")
    val state = CodeLocationExecutionState(SerialCodePosition("f", 1), FunctionStack.empty, None, 0)

    val nodeFFExp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(2, x), SymbolicInt(4, y))), IntEqual,
      SymbolicInt(30))
    val nodeFFBc = EventConstraintWithExecutionState(BranchConstraint(nodeFFExp, Nil), state)
    val nodeFF = BranchSymbolicNode(nodeFFBc, unexploredNode, unexploredNode)
    val ffEdge = ElseEdge(List(AssignmentsStoreUpdate(List((y.get, SymbolicInt(4, y))))), nodeFF)

    val i1 = SymbolicInputInt(RegularId(1, 0))
    val nodeFExp = RelationalExpression(i1, IntEqual, SymbolicInt(20))
    val nodeFBc: ConstraintES = BranchConstraint(nodeFExp, Nil)
    val nodeF = BranchSymbolicNode(nodeFBc, unexploredNode, ffEdge)
    val fEdge = ElseEdge(List(AssignmentsStoreUpdate(List((x.get, SymbolicInt(2, x))))), nodeF)

    val nodeTFTExp = RelationalExpression(SymbolicInt(5, z), IntEqual, SymbolicInt(40))
    val nodeTFTBc = EventConstraintWithExecutionState(
      BranchConstraint(nodeTFTExp, Nil), CodeLocationExecutionState.dummyExecutionState)
    val nodeTFT = BranchSymbolicNode(nodeTFTBc, leafNode, leafNode)
    val tftEdge = ThenEdge(List(AssignmentsStoreUpdate(List((z.get, SymbolicInt(5, z))))), nodeTFT)

    val nodeTFExp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(IntPlus, List(SymbolicInt(1, x), SymbolicInt(4, y))), IntEqual,
      SymbolicInt(30))
    val nodeTFBc = EventConstraintWithExecutionState(BranchConstraint(nodeTFExp, Nil), state)
    val nodeTF = BranchSymbolicNode(nodeTFBc, tftEdge, unexploredNode)
    val tfEdge = ElseEdge(List(AssignmentsStoreUpdate(List((y.get, SymbolicInt(4, y))))), nodeTF)

    val nodeTBc = nodeFBc
    val nodeT = BranchSymbolicNode(nodeTBc, unexploredNode, tfEdge)
    val tEdge = ThenEdge(List(AssignmentsStoreUpdate(List((x.get, SymbolicInt(1, x))))), nodeT)

    val i0 = SymbolicInputInt(RegularId(0, 0))
    val rootExp = RelationalExpression(i0, IntEqual, SymbolicInt(10))
    val rootBc: ConstraintES = BranchConstraint(rootExp, Nil)
    val root = BranchSymbolicNode(rootBc, tEdge, fEdge)

    // Doesn't matter for this test to which node pathConstraintToMerge leads
    val pathConstraintToMerge = TreePath.init(root)._1.addElseBranch(nodeF, fEdge).
      addElseBranch(nodeFF, ffEdge).finishedViaElse.init
    val mergeResult = nodeMerger.mergeWorkListStates(root, pathConstraintToMerge, state)
    assert(mergeResult == ActionSuccessful)

    assert(root.thenBranch.to.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    assert(root.elseBranch.to.isInstanceOf[BranchSymbolicNode[ConstraintES]])
    val castedNodeT = root.thenBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    val castedNodeF = root.elseBranch.to.asInstanceOf[BranchSymbolicNode[ConstraintES]]
    assert(castedNodeT.elseBranch == castedNodeF.elseBranch)
    castedNodeT.elseBranch.to match {
      case nodeTF: BranchSymbolicNode[ConstraintES] =>
        val negater = implicitly[ConstraintNegater[ConstraintES]]
        val predExp1 = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(rootBc))
        val predExp2 = implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(negater.negate(rootBc)))
        val expectedX1 = SymbolicITEExpression(predExp1, SymbolicInt(1, x), SymbolicInt(2, x), x)
        val expectedX2 = SymbolicITEExpression(predExp2, SymbolicInt(2, x), SymbolicInt(1, x), x)

        assert(nodeTF.elseBranch.to == unexploredNode)
        nodeTF.thenBranch.to match {
          case nodeTFT: BranchSymbolicNode[ConstraintES] =>
            assert(nodeTFT.thenBranch.to == leafNode)
            assert(nodeTFT.elseBranch.to == leafNode)
            nodeTFT.constraint match {
              case EventConstraintWithExecutionState(BranchConstraint(exp, _), _) =>
                assert(exp == nodeTFTExp)
              case other => assert(false, s"Expected a EventConstraintWithExecutionState but got $other")
            }
          case _ =>
        }
        nodeTF.constraint match {
          case EventConstraintWithExecutionState(BranchConstraint(exp, _), state1) =>
            assert(state1 == state)
            exp match {
              case RelationalExpression(left, op, right, _) =>
                assert(op == IntEqual)
                assert(right == SymbolicInt(30))
                left match {
                  case ArithmeticalVariadicOperationExpression(op, args, _) =>
                    assert(op == IntPlus)
                    assert(args.length == 2)
                    assert(args.head == expectedX1 || args.head == expectedX2)
                    CheckITEExpressionAsserter.checkITEExpression(List((i0, 10), (i1, 21)), args(1), 4)
                  case other => assert(false, s"Expected a ArithmeticalVariadicOperationExpression but got $other")
                }
              case other => assert(false, s"Expected a RelationalExpression but got $other")
            }
          case other => assert(
            false, s"Expected a BasicConstraintWithExecutionState(BranchConstraint()) but got $other")
        }
      case other => assert(false, s"Expected a BranchSymbolicNode[ConstraintES] but got $other")
    }
  }


  test("Merging (= (+ 1 (1,x)) 10) should produce (= (+ 1 (ite <pred> 1 20)) 10)") {
    val x = Some("x")
    val store: SymbolicStore = Map(x.get -> SymbolicInt(20, x))
    val originalExp = RelationalExpression(
      ArithmeticalVariadicOperationExpression(
        IntPlus,
        List(
          SymbolicInputInt(RegularId(1, 0)),
          SymbolicInt(1, x))),
      IntEqual,
      SymbolicInt(10)
    )
    val predExp = RelationalExpression(SymbolicString("abc"), StringEqual, SymbolicString("abc"))
    val predC: ConstraintES = BranchConstraint(predExp, Nil)
    val mergedExp = nodeMerger.storeMerger.produceMergedExp(
      CodeLocationExecutionState.dummyExecutionState, store, List(predC), originalExp, "T", "E", newVirtualPathStore)
    val expected = RelationalExpression(
      ArithmeticalVariadicOperationExpression(
        IntPlus,
        List(
          SymbolicInputInt(RegularId(1, 0)),
          SymbolicITEExpression(implicitly[ToBooleanExpression[ConstraintES]].toBoolExp(List(predC)), SymbolicInt(1, x), SymbolicInt(20, x), x))),
      IntEqual,
      SymbolicInt(10)
    )
    assertResult(expected)(mergedExp)
  }

}
