import ReadSocket from "./ReadSocket"
import SendOverSocket from "./SendOverSocket"
import ExtendedMap from "../../util/datastructures/ExtendedMap";

/**
 * Creates and maintains ReadSocket and WriteSocket instances by port
 */
export default class SocketStore {

    private static readonly _instance = new SocketStore();

    private _readSockets: ExtendedMap<number, ReadSocket>;
    private _sendSockets: ExtendedMap<number, SendOverSocket>;

    private constructor() {
        this._readSockets = new ExtendedMap();
        this._sendSockets = new ExtendedMap();
    }

    static get instance() {
        return this._instance;
    }

    private makeReadSocket(port: number): ReadSocket {
        const filePath = "/tmp/test_socket_JS_input" + port;
        return new ReadSocket(filePath);
    }

    private makeSendSocket(port: number): SendOverSocket {
        const filePath = "/tmp/test_socket_JS_output" + port;
        return new SendOverSocket(filePath);
    }

    getReadSocket(port: number) {
        let makeFunction = () => this.makeReadSocket(port)
        return this._readSockets.getOrElseInsert(port, makeFunction);
    }

    getSendSocket(port: number) {
        let makeFunction = () => this.makeSendSocket(port);
        return this._sendSockets.getOrElseInsert(port, makeFunction);
    }
}

