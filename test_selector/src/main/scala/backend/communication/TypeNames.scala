package backend.communication

object TypeNames {
  val boolTypeName = "boolean"
  val floatTypeName = "float"
  val intTypeName = "int"
  val stringTypeName = "string"

  def isBool(name: String): Boolean = name == boolTypeName
  def isFloat(name: String): Boolean = name == floatTypeName
  def isInt(name: String): Boolean = name == intTypeName
  def isString(name: String): Boolean = name == stringTypeName
}
