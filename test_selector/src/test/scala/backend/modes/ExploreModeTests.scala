package backend.modes

import backend._
import backend.expression.{RegularId, SymbolicInputInt}
import backend.fixtures.SolvePathTestFixtures.Test18Fixtures
import backend.reporters.EventConstraintPCReporter
import backend.solvers._
import backend.tree.constraints.AllProcessesExplored
import backend.tree.constraints.event_constraints.EventConstraintAllInOne
import org.scalatest.Ignore

@Ignore // Tests are too outdated to be useful at the moment
class ExploreModeTests extends SolvePathTest {

  implicit val mode: Mode = ExploreTreeMode
  private def makeSingleExploreTree: SMTSolveProcesses = {
    val allInOne = EventConstraintAllInOne
    val reporter = new EventConstraintPCReporter(mode)
    import allInOne._
    new SMTSolveProcesses(
      1, _ => ExploreTree(true, TestConfigs.symbolicTreePath, allInOne, reporter, ExploreTreeFactory), None,
      ExploreTreeMode)
  }

  test("Bug 3 (2019-02-22)") {
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":0,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":0}}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":15}]}"""
    val result1 = exploreTree.solve(content1)
  }

  test("Bug 4 (2019-03-01)") {
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":0}}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":0}]}"""
    try {
      exploreTree.solve(content1)
      assert(false, "Should have thrown an AllProcessesExplored exception")
    } catch {
      case AllProcessesExplored =>
      case t: Throwable => assert(false, "Should have thrown an AllProcessesExplored exception")
    }
  }

  test("Bug 5 (2019-03-12)") {
    /* Reported by Bert Van Mieghem (2019-03-12) */
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0},"operator":"==","right":{"type":"SymbolicInt","i":1}},"_wasTrue":false},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":0}}], "processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":0}]}"""
    exploreTree.solve(content1)
  }

  test("Bug 8 (2019-04-03)") {
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content2 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false}},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    exploreTree.solve(content1)
    exploreTree.solve(content2) match {
      case InputAndEventSequence(_, events, _) => assert(events.length > 1)
    }
  }

  test("Bug 14 (2019-04-24)") {
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":true,"varName":"drawing"}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":true,"varName":"emit"}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false,"varName":"emit"}},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":1,"varName":"y0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":1,"varName":"y0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":1,"varName":"y0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content2 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false,"varName":"drawing"}},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content3 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content4 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content5 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false,"varName":"drawing"}},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content6 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":true,"varName":"drawing"}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":true,"varName":"emit"}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false,"varName":"emit"}},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"otherStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":2,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content7 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":false,"varName":"drawing"}},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    val content8 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","finished_run":true,"PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":0,"type":"clickable","htmlElement":{},"specificEventType":"mousedown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicBool","b":true,"varName":"ownStartPos"},"_wasTrue":true},{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":1,"processIdChosen":1,"eventIdChosen":1,"totalNrOfProcesses":2,"totalNrOfEvents":2,"target":{"processId":1,"targetId":1,"type":"clickable","htmlElement":{},"specificEventType":"mouseup"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicBool","b":true,"varName":"drawing"}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0,"varName":"x0"},"operator":"==","right":{"type":"SymbolicInt","i":500}},"_wasTrue":false}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":2}]}"""
    newExploreInputExpected(exploreTree, content1)
    newExploreInputExpected(exploreTree, content2)
    newExploreInputExpected(exploreTree, content3)
    newExploreInputExpected(exploreTree, content4)
    newExploreInputExpected(exploreTree, content5)
    newExploreInputExpected(exploreTree, content6)
    newExploreInputExpected(exploreTree, content7)
    newInputExpected(
      exploreTree,
      content8, {
        case InputAndEventSequence(newInput, _, _) =>
          if (newInput.input.nonEmpty) {
            newInput.input(SymbolicInputInt(RegularId(0, 1))) match {
              case ComputedInt(i) => assert(i != 501)
              case ComputedString(_) => assert(false, "Definitely shouldn't be a string")
            }
          } else {
            assert(true)
          }
        case other => assert(false, other.toString)
      }
    )
  }

  test("Bug 18 (2019-05-16") {
    // Bug was due to the fact that JSONParsing would convert expressions such as "if (textInput.value)" to RelationalSymbolicExpression(SymbolicInputString, IntEquals, SymbolicInt(0))
    val exploreTree = makeSingleExploreTree
    newExploreInputExpected(exploreTree, Test18Fixtures.content1)
    newExploreInputExpected(exploreTree, Test18Fixtures.content2)
    newExploreInputExpected(exploreTree, Test18Fixtures.content3)
    newExploreInputExpected(exploreTree, Test18Fixtures.content4)
    newExploreInputExpected(exploreTree, Test18Fixtures.content5)
    newExploreInputExpected(exploreTree, Test18Fixtures.content6)
    newExploreInputExpected(exploreTree, Test18Fixtures.content7)
    newExploreInputExpected(exploreTree, Test18Fixtures.content8)
    newExploreInputExpected(exploreTree, Test18Fixtures.content9)
    newExploreInputExpected(exploreTree, Test18Fixtures.content10)
    newExploreInputExpected(exploreTree, Test18Fixtures.content11)
    newExploreInputExpected(exploreTree, Test18Fixtures.content12)
    newExploreInputExpected(exploreTree, Test18Fixtures.content13)
    newExploreInputExpected(exploreTree, Test18Fixtures.content14)
    newExploreInputExpected(exploreTree, Test18Fixtures.content15)
    newExploreInputExpected(exploreTree, Test18Fixtures.content16)
    newExploreInputExpected(exploreTree, Test18Fixtures.content17)
    newExploreInputExpected(exploreTree, Test18Fixtures.content18)
    newExploreInputExpected(exploreTree, Test18Fixtures.content19)
    newExploreInputExpected(exploreTree, Test18Fixtures.content20)
    newExploreInputExpected(exploreTree, Test18Fixtures.content21)
    newExploreInputExpected(exploreTree, Test18Fixtures.content22)
    newExploreInputExpected(exploreTree, Test18Fixtures.content23)
    newExploreInputExpected(exploreTree, Test18Fixtures.content24)
  }

  test("Bug 19 (2019-05-16") {
    val exploreTree = makeSingleExploreTree
    val content1 =
      """{"backend_id":0,"type":"explore","explore_type":"get_new_path","PC":[{"_type":"TARGET","_symbolic":{"type":"SymbolicEventChosen","id":0,"processIdChosen":1,"eventIdChosen":0,"totalNrOfProcesses":2,"totalNrOfEvents":3,"target":{"processId":1,"targetId":0,"specificEventType":"keydown"}}},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0},"operator":"==","right":{"type":"SymbolicInt","i":17}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0},"operator":"==","right":{"type":"SymbolicInt","i":91}},"_wasTrue":false},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicUnaryExp","operator":"!","argument":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0},"operator":"==","right":{"type":"SymbolicInt","i":18}}},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInputInt","processId":1,"id":0},"operator":"==","right":{"type":"SymbolicInt","i":13}},"_wasTrue":true},{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicInputString","processId":1,"id":1},"_wasTrue":false}],"processes_info":[{"id":0,"totalNrOfEvents":0},{"id":1,"totalNrOfEvents":3}]}"""
    newExploreInputExpected(exploreTree, content1)
  }

}
