package backend

import java.io._
import java.nio.file._

import backend.SMTSolveProcesses.SMTSolveProcessId
import backend.communication._
import backend.json._
import backend.modes._
import backend.solvers.SolverResult
import backend.tree.constraints.Constraint
import spray.json._

case class UnknownSMTSolveProcess(id: SMTSolveProcessId) extends Error

class SMTSolveProcesses(
  val numberOfProcesses: Int,
  private val createSMTSolveProcess: SMTSolveProcessId => SMTSolveProcess[_ <: Constraint],
  private val optSaveTo: Option[String],
  val mode: Mode
) {
  type InternalMap = Map[SMTSolveProcessId, SMTSolveProcess[_ <: Constraint]]
  protected val map: InternalMap = 0
    .until(numberOfProcesses)
    .foldLeft(Map(): InternalMap)((map, id) =>
      map + ((id: SMTSolveProcessId) -> createSMTSolveProcess(id)))

  def solve(inputString: String): SolverResult = {
    val (id, parsedJSON) = ParsedJSON(inputString)
    val result = getProcess(id).solve(parsedJSON)
    optSaveTo.foreach(SMTSolveProcesses.save(this, _))
    result
  }

  @throws[UnknownSMTSolveProcess]
  protected def getProcess(id: SMTSolveProcessId): SMTSolveProcess[_] = {
    map.get(id) match {
      case Some(process) => process
      case None => throw UnknownSMTSolveProcess(id)
    }
  }
}

object SMTSolveProcesses {

  type SMTSolveProcessId = Int

  def read(jsonPath: String): SMTSolveProcesses = {
    val content: String = new String(Files.readAllBytes(Paths.get(jsonPath)))
    val jsValue: JsValue = JsonParser(content)
    readFromJson(jsValue)
  }

  private def readFromJson(jsValue: JsValue): SMTSolveProcesses = {
    val jsObject = CommonOperations.jsValueToJsObject(jsValue)
    val mode: Mode = SMTSolveProcessModeJsonWriterReader.read(
      CommonOperations.getField(jsObject, FieldNames.modeField))
    val solveProcessParser = new SMTSolveProcessJsonWriterReader(mode)
    def readSolveProcess(jsValue: JsValue): (SMTSolveProcessId, SMTSolveProcess[_ <: Constraint]) = {
      val jsObject = CommonOperations.jsValueToJsObject(jsValue)
      val id: SMTSolveProcessId = CommonOperations.getIntField(jsObject, FieldNames.idField)
      val solveProcess =
        solveProcessParser.read(CommonOperations.getField(jsObject, FieldNames.solveProcessField))
      (id, solveProcess)
    }
    val optSaveTo: Option[String] =
      CommonOperations.getField(jsObject, FieldNames.saveToField) match {
        case JsString(saveTo) => Some(saveTo)
        case JsNull => None
        case _ => throw UnexpectedFieldType(FieldNames.saveToField, "string|null", jsObject)
      }
    val vector = CommonOperations.getArrayField(jsObject, FieldNames.processesField)
    val list: List[(SMTSolveProcessId, SMTSolveProcess[_ <: Constraint])] = vector.map(readSolveProcess).toList
    val map: Map[Int, SMTSolveProcess[_ <: Constraint]] = list.toMap
    val numberOfProcesses: Int = vector.length
    val createSMTSolveProcess = (id: SMTSolveProcessId) => map(id)
    new SMTSolveProcesses(numberOfProcesses, createSMTSolveProcess, optSaveTo, mode)
  }

  def save(solveProcesses: SMTSolveProcesses, saveTo: String): Unit = {
    val fileWriter = new FileWriter(new File(saveTo), false)
    val jsString = saveToJson(solveProcesses).prettyPrint
    fileWriter.write(jsString)
    fileWriter.close()
  }

  private def saveToJson(solveProcesses: SMTSolveProcesses): JsValue = {
    val solveProcessJsParser = new SMTSolveProcessJsonWriterReader(solveProcesses.mode)
    val mapped: List[JsValue] = solveProcesses.map.toList.map(tuple => {
      val id: SMTSolveProcessId = tuple._1
      val solveProcess: SMTSolveProcess[_ <: Constraint] = tuple._2
      JsObject(
        FieldNames.idField -> JsNumber(id),
        FieldNames.solveProcessField -> solveProcessJsParser.write(solveProcess))
    })
    val saveToJsValue = solveProcesses.optSaveTo match {
      case Some(string) => JsString(string)
      case None => JsNull
    }
    JsObject(
      FieldNames.saveToField -> saveToJsValue,
      FieldNames.modeField -> SMTSolveProcessModeJsonWriterReader.write(solveProcesses.mode),
      FieldNames.processesField -> JsArray(mapped.toVector)
    )
  }

  object FieldNames {
    val idField: String = "id"
    val solveProcessField: String = "solve_process"
    val saveToField: String = "save_to"
    val processesField: String = "processes"
    val modeField: String = "mode"
  }
}
