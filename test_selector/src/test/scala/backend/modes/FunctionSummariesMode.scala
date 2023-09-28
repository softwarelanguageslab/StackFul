package backend.modes

import backend.{SMTSolveProcesses, TestConfigs}
import backend.modes._
import backend.solvers._
import org.scalatest.Ignore

@Ignore // Tests are too outdated to be useful at the moment
class FunctionSummariesMode extends SolvePathTest {

  implicit val mode: Mode = FunctionSummariesMode
  private def makeSingleFunctionSummaries: SMTSolveProcesses =
    new SMTSolveProcesses(1, _ => new FunctionSummaries(TestConfigs.symbolicTreePath), None, FunctionSummariesMode)

  test("Bug 20 (2019-08-20") {
    val functionSummaries = makeSingleFunctionSummaries
    functionSummaries.solve(
      """{"backend_id":0,"type":"function_summaries","fs_type":"compute_prefix","prefix":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":0},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":1},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":2},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":3},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":4},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":5},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":false},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","args":[{"type":"SymbolicString","s":"object"},{"type":"SymbolicString","s":"object"}],"operator":"string_equal"},"_isTrue":true}]}""") match {
      case NewInput(_, _) | InputAndEventSequence(_, _, _) =>
      case other => assert(false, s"Got a $other instead")
    }
    functionSummaries.solve(
      """{"backend_id":0,"type":"function_summaries","fs_type":"explore_function","function_id":1,"prefix":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":0},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":1},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":2},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":3},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":4},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":5},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":false},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","args":[{"type":"SymbolicString","s":"object"},{"type":"SymbolicString","s":"object"}],"operator":"string_equal"},"_isTrue":true}],"suffix":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":0},"operator":"<","right":{"type":"SymbolicInt","i":1}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":1},"operator":"<","right":{"type":"SymbolicInt","i":1}},"_isTrue":false},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","args":[{"type":"SymbolicString","s":"object"},{"type":"SymbolicString","s":"object"}],"operator":"string_equal"},"_isTrue":true}]}""") match {
      case NewInput(_, _) | InputAndEventSequence(_, _, _) =>
      case other => assert(false, s"Got a $other instead")
    }

    val finalResult = functionSummaries.solve(
      """{"backend_id":0,"type":"function_summaries","fs_type":"explore_function","function_id":1,"prefix":[],"suffix":[{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":0},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":1},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":2},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":3},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":4},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":true},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicRelationalExp","left":{"type":"SymbolicInt","i":5},"operator":"<","right":{"type":"SymbolicInt","i":5}},"_isTrue":false},{"processId":0,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","args":[{"type":"SymbolicString","s":"object"},{"type":"SymbolicString","s":"object"}],"operator":"string_equal"},"_isTrue":true}]}""")
    assert(finalResult == SymbolicTreeFullyExplored)
  }

}
