package backend

import backend.communication._
import backend.expression._
import backend.modes.{ExploreTreeMode, Mode}
import backend.tree.constraints.basic_constraints.BranchConstraint
import backend.tree.follow_path._
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import spray.json.JsString

import scala.language.implicitConversions

@Ignore // Tests are too outdated to be useful at the moment
class JSONParsingTest extends AnyFunSuite {

  private implicit val mode: Mode = ExploreTreeMode
  private val solvePathJsonParsing = new SolvePathJsonParsing

  implicit private def stringToJsString(string: String): JsString = {
    JsString(string)
  }

  implicit private def targetTriggeredToString(targetTriggered: TargetTriggered): String = {
    s"""{"processId": ${targetTriggered.processId}, "targetId": ${targetTriggered.targetId}}"""
  }

  implicit private def targetsTriggeredToString(targetsTriggered: List[TargetTriggered]): String = {
    "[" + targetsTriggered.map(targetTriggeredToString).mkString(", ") + "]"
  }

  test("Test solve-mode JSON input 1") {
    val jsInputString: String =
      """{ "backend_id":0, "type": "solve", "PC": [], "branch_seq": "ETTEETET", "event_seq": [{"processId": 0, "targetId": 1}] }"""
    val (_, parsedJson) = ParsedJSON(jsInputString)
    val JSONInput = (new SolvePathJsonParsing).parse(parsedJson)
    JSONInput match {
      case SolveModeInput(pc, eventSequence, branchSequence) =>
        assert(pc.isEmpty)
        assert(
          branchSequence == List(
            ElseDirection,
            ThenDirection,
            ThenDirection,
            ElseDirection,
            ElseDirection,
            ThenDirection,
            ElseDirection,
            ThenDirection))
        assert(eventSequence.length == 1)
        assert(eventSequence.head == TargetTriggered(0, 1))
      case _ => assert(false, s"jsonInput should have been a SolveModeInput, but was $JSONInput")
    }
  }

  test("Test solve-mode JSON input 2") {
    val targetsTriggered = List(
      TargetTriggered(3, 1),
      TargetTriggered(2, 5),
      TargetTriggered(1, 0),
      TargetTriggered(-1, 3))
    val targetsString: String = targetsTriggered
    val jsInputString: String =
      s"""{"backend_id":0,"type": "solve", "PC": [], "branch_seq": "TETETE", "event_seq": $targetsString}"""
    val (_, parsedJson) = ParsedJSON(jsInputString)
    val JSONInput = solvePathJsonParsing.parse(parsedJson)
    JSONInput match {
      case SolveModeInput(pc, eventSequence, branchSequence) =>
        assert(pc.isEmpty)
        assert(
          branchSequence == ("TETETE": Path))
        assert(eventSequence.length == 4)
        eventSequence
          .zip(targetsTriggered)
          .foreach((tuple: (TargetTriggered, TargetTriggered)) => {
            assert(tuple._1 == tuple._2)
          })
      case _ => assert(false, s"jsonInput should have been a SolveModeInput, but was $JSONInput")
    }
  }

  private def makeDummyStringInput(
    stringOp: String,
    args: String =
    """[{"type":"SymbolicInputString","processId":1,"id":0},{"type":"SymbolicString","s":"abc"}]"""
  )
  : String = {
    s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"$stringOp","args":$args},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
  }

  test("Test parsing string operations, string_equal") {
    val content = makeDummyStringInput("string_equal")
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicInputString(RegularId(0, 1)),
            StringEqual,
            SymbolicString("abc")))
    }
  }

  test("Test parsing string operations, string_prefix") {
    val content = makeDummyStringInput("string_prefix")
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicInputString(RegularId(0, 1)),
            StringPrefixOf,
            SymbolicString("abc")))
    }
  }

  test("Test parsing string operations, string_suffix") {
    val content = makeDummyStringInput("string_suffix")
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicInputString(RegularId(0, 1)),
            StringSuffixOf,
            SymbolicString("abc")))
    }
  }

  test("Test parsing string operations, string_includes") {
    val content = makeDummyStringInput("string_includes")
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicInputString(RegularId(0, 1)),
            StringIsSubstring,
            SymbolicString("abc")))
    }
  }

  test("Test parsing string operations, string_replace") {
    val stringOpExp =
      """{"type":"SymbolicStringOperationExp","operator":"string_replace","args":[{"type":"SymbolicInputString","processId":1,"id":0},{"type":"SymbolicString","s":"abc"}, {"type":"SymbolicString","s":"abc"}]}"""
    val content =
      s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"string_equal","args":[{"type":"SymbolicString","s":"abc"},$stringOpExp]},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicString("abc"),
            StringEqual,
            StringOperationProducesStringExpression(
              StringReplace,
              List(
                SymbolicInputString(RegularId(0, 1)),
                SymbolicString("abc"),
                SymbolicString("abc"))
            )
          ))
    }
  }

  test("Test parsing string operations, string_append") {
    val stringOpExp =
      """{"type":"SymbolicStringOperationExp","operator":"string_append","args":[{"type":"SymbolicInputString","processId":1,"id":0},{"type":"SymbolicString","s":"abc"}]}"""
    val content =
      s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"string_equal","args":[{"type":"SymbolicString","s":"abc"},$stringOpExp]},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicString("abc"),
            StringEqual,
            StringOperationProducesStringExpression(
              StringAppend,
              List(
                SymbolicInputString(RegularId(0, 1)),
                SymbolicString("abc")))
          ))
    }
  }

  test("Test parsing string operations, string_at") {
    val stringOpExp =
      """{"type":"SymbolicStringOperationExp","operator":"string_at","args":[{"type":"SymbolicInputString","processId":1,"id":0},{"type":"SymbolicInt","i":1}]}"""
    val content =
      s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"string_equal","args":[{"type":"SymbolicString","s":"abc"},$stringOpExp]},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicString("abc"),
            StringEqual,
            StringOperationProducesStringExpression(
              StringAt,
              List(
                SymbolicInputString(RegularId(0, 1)),
                SymbolicInt(1)))
          ))
    }
  }

  test("Test parsing string operations, string_length") {
    val stringOpExp =
      """{"backend_id":0,"type":"SymbolicStringOperationExp","operator":"string_length","args":[{"type":"SymbolicInputString","processId":1,"id":0}]}"""
    val content =
      s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"string_equal","args":[{"type":"SymbolicString","s":"abc"},$stringOpExp]},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicString("abc"),
            StringEqual,
            StringOperationProducesIntExpression(
              StringLength,
              List(SymbolicInputString(RegularId(0, 1))))
          ))
    }
  }

  test("Test parsing string operations, string_index_of") {
    val stringOpExp =
      """{"type":"SymbolicStringOperationExp","operator":"string_index_of","args":[{"type":"SymbolicInputString","processId":1,"id":0},{"type":"SymbolicString","s":"abc"}]}"""
    val content =
      s"""{"backend_id":0,"type":"solve","PC":[{"processId":1,"_type":"CONSTRAINT","_symbolic":{"type":"SymbolicStringOperationExp","operator":"string_equal","args":[{"type":"SymbolicString","s":"abc"},$stringOpExp]},"_wasTrue":true}],"branch_seq":"","event_seq":[]}"""
    val (_, parsedJson) = ParsedJSON(content)
    solvePathJsonParsing.parse(parsedJson) match {
      case SolveModeInput(pc, _, _) =>
        assert(pc.size == 1)
        assert(pc.head.isTrue)
        assert(pc.head.constraint.isInstanceOf[BranchConstraint])
        val exp = pc.head.constraint.asInstanceOf[BranchConstraint].exp
        assert(
          exp == RelationalExpression(
            SymbolicString("abc"),
            StringEqual,
            StringOperationProducesIntExpression(
              StringIndexOf,
              List(
                SymbolicInputString(RegularId(0, 1)),
                SymbolicString("abc")))
          ))
    }
  }

}
