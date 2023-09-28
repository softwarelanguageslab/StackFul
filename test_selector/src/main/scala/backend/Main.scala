package backend

import backend.SMTSolveProcesses.SMTSolveProcessId
import backend.logging._
import backend.modes._
import backend.tree.constraints.Constraint

// Simple server
import backend.communication._

case class StartupConfiguration(
  mode: Mode = ExploreTreeMode,
  inputPort: Int = 9876,
  outputPort: Int = 9877,
  numberOfInstances: Int = 1,
  optSaveTo: Option[String] = None,
  optReadFrom: Option[String] = None,
  useDebug: Boolean = false,
  printLogging: LogLevel = Normal,
  treeLogging: LogLevel = Verbose,
  treeOutputPath: String = "output/execution_trees/symbolic_tree.dot"
)

object Main {

  var useDebug: Boolean = false

  def main(args: Array[String]): Unit = {
    val usage =
      "usage: backend [-m explore|solve|fs|verify|merge] [-i number] [-o number] [-n number]"

    val parser = new scopt.OptionParser[StartupConfiguration]("SMT Backend") {
      head("SMT Backend")
      help("help").text("prints the help message")
      opt[Option[Mode]]('m', "mode")
        .action({ (x, c) =>
          c.copy(mode = x.getOrElse(c.mode))
        })
        .text("Mode in which to start the backend [default: explore]")
      opt[Int]('i', "input-port")
        .action({ (x, c) =>
          c.copy(inputPort = x)
        })
        .text("Input port to use [default: 9876]")
      opt[Int]('o', "output-port")
        .action({ (x, c) =>
          c.copy(outputPort = x)
        })
        .text("Output port to use [default: 9877]")
      opt[Int]('n', "instances")
        .action({ (x, c) =>
          c.copy(numberOfInstances = x)
        })
        .text("Number of instances [default: 1]")
      opt[Option[String]]("saveto")
        .action({ (x, c) =>
          c.copy(optSaveTo = x)
        })
        .text("Save execution to file [default: none]")
      opt[Option[String]]("readfrom")
        .action({ (x, c) =>
          c.copy(optReadFrom = x)
        })
        .text("Read execution from file [default: none]")
      opt[Option[String]]("tree-output-path")
        .action({ (x, c) =>
          c.copy(treeOutputPath = x.getOrElse(c.treeOutputPath))
        })
        .text("Save symbolic execution tree to file [default: output/execution_trees/symbolic_tree.dot]")
      opt[Option[LogLevel]]("log-print")
        .action({ (optX, c) => optX.fold(c)(x => c.copy(printLogging = x)) })
        .text("Logging mode for printing output")
      opt[Option[LogLevel]]("log-tree")
        .action({ (optX, c) => optX.fold(c)(x => c.copy(treeLogging = x)) })
        .text("Logging mode for generating execution tree visualisations")
      opt[Unit]('d', "debug")
        .action({ (_, c) => c.copy(useDebug = true) })
        .text("Enable debugging mode")
    }
    parser.parse(args, StartupConfiguration()) match {
      case None => println(usage)
      case Some(config) =>
        println(
          s"mode = ${config.mode}, inputPort = ${config.inputPort}, outputPort = ${config.outputPort}, log-tree = ${config.treeLogging}, log-print = ${config.printLogging}")
        Logger.setLogLevel(config.printLogging)
        useDebug = config.useDebug
        listen(config)
    }
  }

  private def listen(config: StartupConfiguration): Unit = {
    val socketJSOutputPath = "/tmp/test_socket_JS_output"
    val defaultTreeOutput = "output/execution_trees/symbolic_tree.dot"
    val smtSolveProcesses: SMTSolveProcesses = config.optReadFrom match {
      case None => createNewSMTSolveProcesses(config, config.treeOutputPath, socketJSOutputPath)
      case Some(readFrom) => SMTSolveProcesses.read(readFrom)
    }
    val receiveSocket = ReceiveSocket.createSocket(socketJSOutputPath, smtSolveProcesses)(config)
    receiveSocket.start()
  }

  implicit val modeRead: scopt.Read[Option[Mode]] = scopt.Read.reads((input: String) =>
    input.toLowerCase match {
      case "e" | "explore" => Some(ExploreTreeMode)
      case "f" | "fs" | "function_summaries" => Some(FunctionSummariesMode)
      case "m" | "merge" | "merging" => Some(MergingMode)
      case "s" | "solve" => Some(SolvePathsMode)
      case "v" | "verify" | "verify_intra" => Some(VerifyIntraMode)
      case _ => None
    })
  implicit val logLevelRead: scopt.Read[Option[LogLevel]] = scopt.Read.reads((input: String) =>
    input.toLowerCase match {
      case "e" | "error" => Some(Error)
      case "n" | "normal" => Some(Normal)
      case "v" | "verbose" => Some(Verbose)
      case "d" | "debugging" => Some(Debugging)
      case _ => None
    })
  implicit val stringRead: scopt.Read[Option[String]] = scopt.Read.reads(Some[String])

  private def createNewSMTSolveProcesses(
    config: StartupConfiguration,
    treeOutputPath: String,
    socketJSOutputPath: String
  ): SMTSolveProcesses = {
    implicit val mode: Mode = config.mode
    def createProcess(id: SMTSolveProcessId): SMTSolveProcess[_ <: Constraint] =
      createSMTSolveProcess(mode, treeOutputPath, id, config)
    new SMTSolveProcesses(config.numberOfInstances, createProcess, config.optSaveTo, mode)
  }

}
