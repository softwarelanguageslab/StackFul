package backend.communication.execution_state

import backend.communication._
import backend.execution_state.SymbolicStore
import backend.expression.{SymbolicExpression, SymbolicNothingExpression}
import backend.json.SymbolicExpressionJsonWriter
import spray.json._

object StoreElementFields {
  val identifierField: String = "id"
  val expField: String = "exp"

  val symbolicField: String = "symbolic"
}

object JsonParseSymbolicStore
  extends JsonReader[SymbolicStore]
    with JsonWriter[SymbolicStore] {

  import CommonOperations._
  import StoreElementFields._

  override def read(jsValue: JsValue): SymbolicStore = convertSymbolicStore(jsValue)
  def convertSymbolicStore(jsValue: JsValue): SymbolicStore = {
    val jsArray = jsValueToJsArray(jsValue)
    val tuples = jsArray.elements.toList.map(convertStoreElement)
    val nothingExpsExcluded = tuples.filter(_._2 != SymbolicNothingExpression)
    nothingExpsExcluded.toMap
  }
  private[execution_state] def convertStoreElement(jsValue: JsValue): (String, SymbolicExpression) = {
    val jsObject = jsValueToJsObject(jsValue)
    val identifier = getStringField(jsObject, identifierField)
    val jsExp = getObjectField(jsObject, expField)
    val symbolic = getField(jsExp, symbolicField)
    val exp = JsonParseSymbolicExp.symbolicToExp(symbolic)
    (identifier, exp)
  }
  override def write(store: SymbolicStore): JsValue = {
    def writeTuple(tuple: (String, SymbolicExpression)): JsObject = {
      val fields = Map(
        identifierField -> JsString(tuple._1),
        expField -> SymbolicExpressionJsonWriter().write(tuple._2))
      JsObject(fields)
    }
    val tuples = store.toVector.map(writeTuple)
    JsArray(tuples)
  }
}
