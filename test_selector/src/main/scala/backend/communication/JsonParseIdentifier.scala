package backend.communication

import spray.json.{JsObject, JsString}

object JsonParseIdentifier {
  def ignoreIdentifier(identifier: String): Boolean = {
    identifier == "__empty__"
  }
  def parseIdentifier(jsObject: JsObject): Option[String] = {
    CommonOperations.getField(jsObject, ExpFields.identifierField) match {
      case JsString(string) if ignoreIdentifier(string) => None
      case JsString(string) => Some(string)
      case _ => None
    }
  }
}
