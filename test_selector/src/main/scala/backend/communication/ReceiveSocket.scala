package backend.communication

import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.util.zip.Inflater
import backend.ExitCodes._
import backend.logging.Logger
import backend.modes.SuccessfullyTerminatedException
import backend.solvers.SolveError
import backend.{HasExitCode, _}

import java.nio.ByteBuffer
import java.nio.channels.{ServerSocketChannel, SocketChannel}

class ReceiveSocket(val solveProcesses: SMTSolveProcesses, val fileAddress: String)(
  implicit val config: StartupConfiguration
) {
  private val serverChannel: ServerSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
  private val socketAddress: UnixDomainSocketAddress = UnixDomainSocketAddress.of(fileAddress)
  serverChannel.bind(socketAddress)

  private var sendSocket: Option[SendSocket] = None

  def start(): Unit = {
    val channel: SocketChannel = serverChannel.accept()
    while (true) {
      println("\n---------- START INTERACTION ----------\n")
      val content: String = listenForInput(channel)
      Logger.v(s"(${config.inputPort} RECEIVED INPUT $content")
      if (sendSocket.isEmpty) {
        sendSocket = Some(new SendSocket(config.outputPort))
      }
      try {
        val solverResult = solveProcesses.solve(content)
        Logger.v(s"doServeSocket, solverResult = $solverResult")
        sendSocket.get.send(solverResult)
      } catch {
        case CommunicationException => unforeseenConsequence(CommunicationException)
        case SuccessfullyTerminatedException => unforeseenConsequence(SuccessfullyTerminatedException)
        case ex: JSONParsingException => unforeseenConsequence(ex)
        case ex: SolveError => unforeseenConsequence(ex)
        case ex: Error => unforeseenConsequence(ex)
        case ex: Exception => unforeseenConsequence(ex)
      }
      println("\n---------- END INTERACTION ----------\n\n\n\n\n")
    }
  }

  private def listenForInput(socketChannel: SocketChannel): String = {

    // Subtract the 8 bytes that specify the lengths of the compressed/decompressed messages
    val compressedMessageLength = readMessageLength(socketChannel) - 8
    val decompressedMessageLength = readMessageLength(socketChannel) + 1
    val output: Array[Byte] = new Array[Byte](compressedMessageLength)
    println(s"TestSocket.listenForInput: compressedMessageLength = $compressedMessageLength")
    println(s"TestSocket.listenForInput: decompressedMessageLength = $decompressedMessageLength")
    if (compressedMessageLength < 0) {
      throw CommunicationException
    }

    var remainingBytesToRead = compressedMessageLength
    var totalBytesRead = 0
    val limit = 5000
    while (remainingBytesToRead > 0) {
      val buffer = ByteBuffer.allocate(limit)
      var bytesRead = socketChannel.read(buffer)
      remainingBytesToRead -= bytesRead
      buffer.flip()
      var i = 0
      while (i < bytesRead) {
        val byteRead = buffer.get()
        output.update(totalBytesRead, byteRead)
        i += 1
        totalBytesRead += 1
      }
    }

    decompressMessage(compressedMessageLength, decompressedMessageLength, output)
  }

  private def decompressMessage(messageLength: Long, bufferSize: Int, output: Array[Byte]): String = {

    // https://docs.oracle.com/javase/6/docs/api/java/util/zip/Deflater.html
    val decompresser = new Inflater
    decompresser.setInput(output, 0, messageLength.toInt)
    val result = new Array[Byte](bufferSize)
    val resultLength = decompresser.inflate(result)
    decompresser.end()

    // Decode the bytes into a String
    val outputString = new String(result, 0, resultLength, "UTF-8")
    outputString
  }

  private def readMessageLength(socketChannel: SocketChannel): Int = {
    val buffer = ByteBuffer.allocate(4)
    socketChannel.read(buffer)
    buffer.flip()
    var messageLength: Int = 0
    (1 to 4).foreach(_ => {
      messageLength = messageLength << 8
      val signedByte = buffer.get()
      val unsignedByte = if (signedByte >= 0) signedByte else 256 + signedByte
      messageLength += unsignedByte
    })
    messageLength
  }

  private def unforeseenConsequence[T <: Throwable : HasExitCode](ex: T): Unit = {
    val exitCode = implicitly[HasExitCode[T]].exitCode(ex)
    if (exitCode != ExitCodes.noError) {
      ex.printStackTrace()
    }
    serverChannel.close()
    System.exit(exitCode)
  }
}

object ReceiveSocket {

  def createSocket(filePath: String, SMTSolveProcesses: SMTSolveProcesses)(
    implicit config: StartupConfiguration
  ): ReceiveSocket = {
    val fullFilePath = filePath + config.inputPort
    val newSocket = new ReceiveSocket(SMTSolveProcesses, fullFilePath)
    newSocket
  }

}
