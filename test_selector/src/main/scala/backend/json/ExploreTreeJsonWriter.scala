package backend.json

import backend.{PathConstraint, RegularPCElement, RegularPCElementConstantChecker, RegularPCElementOptimizer}
import backend.communication.{CommonOperations, UnexpectedFieldType}
import backend.modes._
import backend.reporters.{EventConstraintPCReporter, PCReporter, Reporter}
import backend.tree.{RemoveConstantsSanitizer, SymbolicNode}
import backend.tree.constraints.event_constraints.EventConstraintAllInOne
import backend.tree.constraints.{Constraint, ConstraintNegater, EventConstraint, Optimizer}
import backend.tree.search_strategy.BreadthFirstSearchCached
import spray.json._

class ReporterJsonWriterReader[C <: Constraint : ConstraintNegater : Optimizer : JsonReader : JsonWriter](
  val reporter: PCReporter[C, RegularPCElement[C]]
)(
  implicit val mode: Mode
)
  extends JsonWriter[Reporter[PathConstraint[C], C]]
    with JsonReader[Reporter[PathConstraint[C], C]] {
  implicit val unitJsonWriterReader: UnitJsonWriterReader.type = UnitJsonWriterReader

  val symbolicNodeWriter = new SymbolicNodeJsonWriterReader[C]

  def read(jsValue: JsValue): Reporter[PathConstraint[C], C] = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val optTreeJsValue = CommonOperations.getField(jsObject, FieldNames.treeField)
    val optTree: Option[SymbolicNode[C]] = optTreeJsValue match {
      case JsNull => None
      case treeJsValue: JsObject => Some(symbolicNodeWriter.read(treeJsValue))
      case _ => throw UnexpectedFieldType(FieldNames.treeField, "null|object", jsObject)
    }

    reporter.optRoot = optTree
    reporter
  }

  def write(reporter: Reporter[PathConstraint[C], C]): JsValue = {
    val symbolicTree = reporter.getRoot match {
      case None => JsNull
      case Some(root) => symbolicNodeWriter.write(root)
    }
    JsObject(FieldNames.treeField -> symbolicTree)
  }

  object FieldNames {
    val treeField: String = "tree"
  }
}

object UnitJsonWriterReader extends JsonWriter[Unit] with JsonReader[Unit] {
  def write(value: Unit): JsNull.type = JsNull
  def read(jsValue: JsValue): Unit = ()
}

object ExploreTreeJsonWriter extends JsonWriter[ExploreTree[EventConstraint, RegularPCElement[EventConstraint]]]
  with JsonReader[ExploreTree[EventConstraint, RegularPCElement[EventConstraint]]] {

  def read(jsValue: JsValue): ExploreTree[EventConstraint, RegularPCElement[EventConstraint]] = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val prescribeEvents =
      CommonOperations.getBooleanField(jsObject, FieldNames.prescribeEventsField)
    val treeOutput = CommonOperations.getStringField(jsObject, FieldNames.treeOutputPathField)
    val allInOne = EventConstraintAllInOne
    //    implicit val negater: ConstraintNegater[EventConstraint] = allInOne.constraintNegater
    import allInOne._

    val reporterReader = makeReporterWriterReader
    val reporterJsValue = CommonOperations.getObjectField(jsObject, FieldNames.reporterField)
    val reporter = reporterReader.read(reporterJsValue)
    val exploreTree = new ExploreTree(
      prescribeEvents,
      treeOutput,
      (root: SymbolicNode[EventConstraint]) => new BreadthFirstSearchCached[EventConstraint](root),
      reporter,
      allInOne,
      RemoveConstantsSanitizer,
      RegularPCElementOptimizer(),
      RegularPCElementConstantChecker())
    exploreTree
  }

  implicit val unitJsonReader: JsonReader[Unit] = UnitJsonWriterReader
  implicit val unitJsonWriter: JsonWriter[Unit] = UnitJsonWriterReader
  def makeReporterWriterReader: ReporterJsonWriterReader[EventConstraint] = {
    val allInOne = EventConstraintAllInOne
    import allInOne._
    implicit val mode: Mode = ExploreTreeMode
    new ReporterJsonWriterReader(new EventConstraintPCReporter(ExploreTreeMode))
  }
  def write(exploreTree: ExploreTree[EventConstraint, RegularPCElement[EventConstraint]]): JsObject = {
    JsObject(
      FieldNames.prescribeEventsField -> JsBoolean(exploreTree.prescribeEvents),
      FieldNames.treeOutputPathField -> JsString(exploreTree.treeOutputPath),
      FieldNames.reporterField -> makeReporterWriterReader.write(exploreTree.reporter)
    )
  }
  object FieldNames {
    val prescribeEventsField: String = "prescribe_events"
    val treeOutputPathField: String = "tree_output"
    val reporterField: String = "reporter"
  }

}
