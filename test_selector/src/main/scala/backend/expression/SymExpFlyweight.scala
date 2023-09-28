package backend.expression

import backend.SmartHash

import scala.collection.mutable.{Map => MMap}

object SymExpFlyweight {

  private val bools: MMap[Key[Boolean], SymbolicBool] = MMap.empty
  private val floats: MMap[Key[Float], SymbolicFloat] = MMap.empty
  private val ints: MMap[Key[Int], SymbolicInt] = MMap.empty
  private val strings: MMap[Key[String], SymbolicString] = MMap.empty

  private val boolInputs: MMap[Key[SymbolicInputId], SymbolicInputBool] = MMap.empty
  private val floatInputs: MMap[Key[SymbolicInputId], SymbolicInputFloat] = MMap.empty
  private val intInputs: MMap[Key[SymbolicInputId], SymbolicInputInt] = MMap.empty
  private val stringInputs: MMap[Key[SymbolicInputId], SymbolicInputString] = MMap.empty
  def makeSymBool(bool: Boolean, identifier: Option[String]): SymbolicBool = {
    bools.getOrElseUpdate(Key(bool, identifier), new SymbolicBool(bool, identifier))
  }
  def makeSymFloat(float: Float, identifier: Option[String]): SymbolicFloat = {
    floats.getOrElseUpdate(Key(float, identifier), new SymbolicFloat(float, identifier))
  }
  def makeSymInt(int: Int, identifier: Option[String]): SymbolicInt = {
    ints.getOrElseUpdate(Key(int, identifier), new SymbolicInt(int, identifier))
  }
  def makeSymString(string: String, identifier: Option[String]): SymbolicString = {
    strings.getOrElseUpdate(Key(string, identifier), new SymbolicString(string, identifier))
  }
  def makeSymInputBool(id: SymbolicInputId, identifier: Option[String]): SymbolicInputBool = {
    boolInputs.getOrElseUpdate(Key(id, identifier), new SymbolicInputBool(id, identifier))
  }
  def makeSymInputFloat(id: SymbolicInputId, identifier: Option[String]): SymbolicInputFloat = {
    floatInputs.getOrElseUpdate(Key(id, identifier), new SymbolicInputFloat(id, identifier))
  }
  def makeSymInputInt(id: SymbolicInputId, identifier: Option[String]): SymbolicInputInt = {
    intInputs.getOrElseUpdate(Key(id, identifier), new SymbolicInputInt(id, identifier))
  }
  def makeSymInputString(id: SymbolicInputId, identifier: Option[String]): SymbolicInputString = {
    stringInputs.getOrElseUpdate(Key(id, identifier), new SymbolicInputString(id, identifier))
  }
  private case class Key[T](value: T, identifier: Option[String]) extends SmartHash

}
