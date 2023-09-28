import PosixSocket from "posix-socket";
import {TextDecoder} from "util";

export default class ReadSocket {
    private _sockfd: any;
    private _sockfd1: null;
    
    constructor(filePath) {
        console.log("ReadSocket:", filePath);
        this._sockfd = PosixSocket.socket(PosixSocket.AF_UNIX, PosixSocket.SOCK_STREAM, 0);
        PosixSocket.bind(this._sockfd, {
            sun_family: PosixSocket.AF_UNIX,
            sun_path: filePath
        });
        PosixSocket.listen(this._sockfd, 1);
        this._sockfd1 = null;
    }

    read(expectedMessageLength: number = -1) {
        const that = this;

        // Source: https://stackoverflow.com/questions/8609289/convert-a-binary-nodejs-buffer-to-javascript-arraybuffer
        function toRawBuffer(ab) {
            var buf = Buffer.alloc(ab.byteLength);
            var view = new Int8Array(ab);
            for (var i = 0; i < buf.length; ++i) {
                buf[i] = view[i];
            }
            return buf;
        }

        function calcMessageLength(buf) {
            var total = 0;
            var i = 0;
            while (i < 4) {
                const x = buf[i];
                total = total << 8;
                total += x;
                i++;
            }
            return total;
        }

        function readMessageLength(sockfd) {
            const messageLengthArrayBuffer = new ArrayBuffer(4);
            const size1 = PosixSocket.recv(sockfd, messageLengthArrayBuffer, 4, 0);
            const messageLengthRawBuffer = toRawBuffer(messageLengthArrayBuffer);
            const messageLength = calcMessageLength(messageLengthRawBuffer);
            return messageLength;
        }

        function readMessage(messageLength, sockfd) {
            const messageArrayBuffer = new ArrayBuffer(messageLength);
            const receivedMessageLength = PosixSocket.recv(sockfd, messageArrayBuffer, messageArrayBuffer.byteLength, 0);
            const view = new Uint8Array(messageArrayBuffer, 0, receivedMessageLength)
            const messageString = new TextDecoder().decode(view);
            return messageString;
        }

        function getSockfd1() {
            if (that._sockfd1 === null) {
                const address = {};
                that._sockfd1 = PosixSocket.accept(that._sockfd, address);
            }
            return that._sockfd1;
        }

        function closeSocketFd() {
            PosixSocket.close(that._sockfd1);
            that._sockfd1 = null;
        }

        const sockfd1 = getSockfd1();
        const messageLength = (expectedMessageLength < 0) ? readMessageLength(sockfd1) : expectedMessageLength;
        const messageString = readMessage(messageLength, sockfd1);
        closeSocketFd();
        return messageString;
    }
}