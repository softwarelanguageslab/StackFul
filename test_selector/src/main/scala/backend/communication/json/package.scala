package backend.communication

import spray.json._
import backend.expression.SymbolicInputId

package object json {

  implicit val jsonReaderSymbolicInputId: JsonReader[SymbolicInputId] = SymbolicInputIdJsonReaderWriter
  implicit val jsonWriterSymbolicInputId: JsonWriter[SymbolicInputId] = SymbolicInputIdJsonReaderWriter

}
