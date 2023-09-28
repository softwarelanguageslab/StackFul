package backend.communication

import java.net.UnixDomainSocketAddress
import backend.solvers.SolverResult
import spray.json._

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class SendSocket(val socket: Int) {
  private val socketJSInputPath: String = "/tmp/test_socket_JS_input" + socket
  private val address: UnixDomainSocketAddress = UnixDomainSocketAddress.of(socketJSInputPath)
  private var optSocketChannel: Option[SocketChannel] = None

  private var isShutdown: Boolean = true
  private def openSocketOutput(): SocketChannel = {
    val socket = SocketChannel.open(address)
    optSocketChannel = Some(socket)
    isShutdown = false
    socket
  }
  private def shutdownSocketOutput(socket: SocketChannel): Unit = {
    socket.shutdownOutput()
    socket.finishConnect()
    isShutdown = true
  }

  private def getSocket: SocketChannel = optSocketChannel match {
    case None =>
      openSocketOutput()
    case Some(socket) =>
      if (isShutdown) {
        openSocketOutput()
      } else {
        socket
      }
  }

  def close(): Unit = {
    optSocketChannel.foreach(_.close())
  }

  private var timesSent = 0

  def send(solverResult: SolverResult): Unit = {
    val outputJSON: String = solverResult.toJson.toString
    val messageLength = outputJSON.length
    val buffer = ByteBuffer.allocate(messageLength + 4)
    buffer.clear()
    val bytes = makeByteArray(messageLength)
    (0.until(messageLength)).foreach(i => {
      bytes.update(i + 4, outputJSON.charAt(i).toByte)
    })
    buffer.put(bytes)
    buffer.position(0)
    val socket = getSocket
    socket.write(buffer)
    buffer.clear()
    shutdownSocketOutput(socket)

  }

  private def makeByteArray(messageLength: Int): Array[Byte] = {
    val bytes = new Array[Byte](4 + messageLength)
    var mutableMessageLength = messageLength
    (0 to 3).foreach(i => {
      val byte: Byte = (mutableMessageLength % 256).toByte
      bytes.update(3 - i, byte)
      mutableMessageLength = mutableMessageLength >> 8
    })
    bytes
  }
}
