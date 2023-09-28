package backend.communication

import spray.json.{DeserializationException, JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

object CommonOperations {
  @throws[UnexpectedInputType]
  def jsValueToJsArray(jsValue: JsValue): JsArray = jsValue match {
    case jsArray: JsArray => jsArray
    case _ => throw UnexpectedInputType(jsValue.toString)
  }

  @throws[UnexpectedInputType]
  def jsValueToJsObject(jsValue: JsValue): JsObject = {
    try {
      jsValue.asJsObject
    } catch {
      case _: DeserializationException => throw UnexpectedInputType(jsValue.toString)
    }
  }

  @throws[UnexpectedInputType]
  def jsBooleanToBoolean(jsValue: JsValue): Boolean = jsValue match {
    case jsBoolean: JsBoolean => jsBooleanToBoolean(jsBoolean)
    case _ => throw UnexpectedInputType(jsValue.toString)
  }
  @throws[UnexpectedInputType]
  def jsNumberToInt(jsValue: JsValue): Int = jsValue match {
    case jsNumber: JsNumber => jsNumberToInt(jsNumber)
    case _ => throw UnexpectedInputType(jsValue.toString)
  }
  @throws[UnexpectedInputType]
  def asString(jsValue: JsValue): String = jsValue match {
    case jsString: JsString => jsStringToString(jsString)
    case jsNumber: JsNumber => jsNumberToInt(jsNumber).toString
    case jsBoolean: JsBoolean => jsBooleanToBoolean(jsBoolean).toString
    case _ => throw UnexpectedInputType(jsValue.toString)
  }
  @throws[UnexpectedInputType]
  def jsStringToString(jsValue: JsValue): String = jsValue match {
    case jsString: JsString => jsStringToString(jsString)
    case _ => throw UnexpectedInputType(jsValue.toString)
  }
  @throws[JSONParsingException]
  def getType(jsObject: JsObject): String = {
    jsObject.fields.get("type") match {
      case Some(string: JsString) => string.value.toLowerCase()
      case Some(otherThanString) => throw UnexpectedInputType(otherThanString.toString)
      case None => throw UnexpectedJSONFormat(jsObject)
    }
  }
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def getArrayField(jsObject: JsObject, fieldName: String): Vector[JsValue] = {
    getField(jsObject, fieldName) match {
      case jsArray: JsArray => jsArray.elements
      case _ => throw UnexpectedFieldType(fieldName, "array", jsObject)
    }
  }
  @throws[UnexpectedFieldType]
  def getOptArrayField(jsObject: JsObject, fieldName: String): Option[Vector[JsValue]] = {
    jsObject.fields.get(fieldName) match {
      case None => None
      case Some(jsArray: JsArray) => Some(jsArray.elements)
      case _ => throw UnexpectedFieldType(fieldName, "array", jsObject)
    }
  }
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def getBooleanField(jsObject: JsObject, fieldName: String): Boolean = {
    getField(jsObject, fieldName) match {
      case jsBoolean: JsBoolean => jsBooleanToBoolean(jsBoolean)
      case _ => throw UnexpectedFieldType(fieldName, "boolean", jsObject)
    }
  }
  @throws[UnexpectedFieldType]
  def getOptBooleanField(jsObject: JsObject, fieldName: String): Option[Boolean] = {
    jsObject.fields.get(fieldName) match {
      case None => None
      case Some(jsBoolean: JsBoolean) => Some(jsBooleanToBoolean(jsBoolean))
      case _ => throw UnexpectedFieldType(fieldName, "boolean", jsObject)
    }
  }
  def jsBooleanToBoolean(jsBoolean: JsBoolean): Boolean = {
    jsBoolean.value
  }
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def getIntField(jsObject: JsObject, fieldName: String): Int = {
    getField(jsObject, fieldName) match {
      case jsNumber: JsNumber => jsNumberToInt(jsNumber)
      case _ => throw UnexpectedFieldType(fieldName, "int", jsObject)
    }
  }
  @throws[UnexpectedFieldType]
  def getOptIntField(jsObject: JsObject, fieldName: String): Option[Int] = {
    jsObject.fields.get(fieldName) match {
      case None => None
      case Some(jsNumber: JsNumber) => Some(jsNumberToInt(jsNumber))
      case _ => throw UnexpectedFieldType(fieldName, "int", jsObject)
    }
  }
  def jsNumberToInt(jsNumber: JsNumber): Int = {
    jsNumber.value.toIntExact
  }
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def getObjectField(jsObject: JsObject, fieldName: String): JsObject = {
    getField(jsObject, fieldName) match {
      case jsObject: JsObject => jsObject
      case _ => throw UnexpectedFieldType(fieldName, "object", jsObject)
    }
  }
  @throws[UnexpectedJSONFormat]
  def getField(jsObject: JsObject, fieldName: String): JsValue = {
    jsObject.fields.get(fieldName) match {
      case Some(value) => value
      case None => throw MissingField(fieldName, jsObject)
    }
  }
  @throws[UnexpectedFieldType]
  def getOptObjectField(jsObject: JsObject, fieldName: String): Option[JsObject] = {
    jsObject.fields.get(fieldName) match {
      case None => None
      case Some(jsObject: JsObject) => Some(jsObject)
      case _ => throw UnexpectedFieldType(fieldName, "object", jsObject)
    }
  }
  @throws[UnexpectedJSONFormat]
  @throws[UnexpectedFieldType]
  def getStringField(jsObject: JsObject, fieldName: String): String = {
    getField(jsObject, fieldName) match {
      case jsString: JsString => jsStringToString(jsString)
      case _ => throw UnexpectedFieldType(fieldName, "string", jsObject)
    }
  }
  @throws[UnexpectedFieldType]
  def getOptStringField(jsObject: JsObject, fieldName: String): Option[String] = {
    jsObject.fields.get(fieldName) match {
      case None => None
      case Some(jsString: JsString) => Some(jsStringToString(jsString))
      case _ => throw UnexpectedFieldType(fieldName, "string", jsObject)
    }
  }
  def jsStringToString(jsString: JsString): String = {
    jsString.value
  }
  /*
   * Retrieves either a string field from the jsObject or attempts to cast the field to a string, if possible.
   */
  @throws[UnexpectedFieldType]
  def getAsStringField(jsObject: JsObject, fieldName: String): String = {
    getField(jsObject, fieldName) match {
      case jsBoolean: JsBoolean => jsBooleanToBoolean(jsBoolean).toString
      case jsNumber: JsNumber => jsNumberToInt(jsNumber).toString
      case jsString: JsString => jsStringToString(jsString)
      case _ => throw UnexpectedFieldType(fieldName, "string", jsObject)
    }
  }
}
