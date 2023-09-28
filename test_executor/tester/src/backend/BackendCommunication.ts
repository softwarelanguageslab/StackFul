import {jsPrint} from "@src/util/io_operations";
import {Logger as Log} from "@src/util/logging";
import SocketStore from "./socket_comm/SocketStore";
import {SymbolicExpression} from "@src/symbolic_expression/symbolic_expressions";
import * as SymTypes from "@src/symbolic_expression/supported_symbolic_types";
import ReadSocket from "@src/backend/socket_comm/ReadSocket";
import {BackendResult} from "@src/backend/BackendResult";

export default abstract class BackendCommunication {

  constructor(protected _inputPort: number, protected _outputPort: number, protected _backendId: number) {
    SocketStore.instance.getReadSocket(this._inputPort);
  }

  expectResult<T>(callback: (any) => T): T {
    const readSocket: ReadSocket = SocketStore.instance.getReadSocket(this._inputPort);
    const messageString = readSocket.read();
    Log.BCK("Received message:", messageString);
    const parsed = JSON.parse(messageString);
    return callback(parsed);

  }

  receiveSolverResult(): BackendResult {
    const result: BackendResult = this.expectResult(this.handleBackendResult);
    Log.BCK("BackendCommunication.receiveSolverResult, result =", result);
    return result;
  }

  protected _sendOverSocket(message) {
    const sendSocket = SocketStore.instance.getSendSocket(this._outputPort);
    sendSocket.send(message);
    Log.BCK(`Frontend sending: ${message}`);
  }

  sendTerminationMessage() {
    const wrapped = {backend_id: this._backendId, type: "successfully_terminated"};
    const message = JSON.stringify(wrapped);
    this._sendOverSocket(message);
  }

   abstract handleBackendResult(parsedJSON): BackendResult;

  /**
   * Construct object containing type, concrete and symbolic value
   * @param concrete Concrete value
   * @param symbolic symbolic value
   */
  protected static wrapConcreteSymbolic(concrete, symbolic) {
    const type = SymTypes.determineType(concrete);
    if (type === SymTypes.SupportedSymbolicExpressionTypes.Unsupported) {
      jsPrint("Sending unsupported symbolic type", typeof(concrete));
    }
    return { type: type, concrete: concrete, symbolic: symbolic };
  }

  /**
   * Filter to exclude Unsupported symbolic expression types
   * @param wrappedArgs
   */
  protected static filterWrappedArgs(wrappedArgs: SymbolicExpression[]) {
    return wrappedArgs.filter((wrappedArg) => wrappedArg.type !== SymTypes.SupportedSymbolicExpressionTypes.Unsupported);
  }

}
