import pako from "pako";
import PosixSocket from "posix-socket";
import * as buffer from "buffer";

export default class SendOverSocket {
    private _sockfd: any;
    protected readonly lengthSize = 4;

    constructor(filePath) {
        console.log("SendOverSocket:", filePath);
        this._sockfd = PosixSocket.socket(PosixSocket.AF_UNIX, PosixSocket.SOCK_STREAM, 0);
        PosixSocket.connect(this._sockfd, {
            sun_family: PosixSocket.AF_UNIX,
            sun_path: filePath
        });
    }

    send(message, sendCompressed: boolean = true) { // send
        // Source: https://stackoverflow.com/questions/8609289/convert-a-binary-nodejs-buffer-to-javascript-arraybuffer
        function toArrayBuffer(buf): ArrayBuffer {
            var ab = new ArrayBuffer(buf.length);
            var view = new Uint8Array(ab);
            for (var i = 0; i < buf.length; ++i) {
                view[i] = buf[i];
            }
            return ab;
        }

        if (sendCompressed) {

            // Buffer lay-out:
            //   - this.lengthSize bytes for the length of the compressed message
            //   - this.lengthSize bytes for the length of the uncompressed message
            const headerSize = this.lengthSize * 2;
            const that = this;

            function prepareHeader(bufferLength: number, rawBuffer): void {
                rawBuffer.writeUInt32BE(bufferLength, 0);
                // Write length of uncompressed message, so backend knows the size of the buffer it should allocate
                rawBuffer.writeUInt32BE(message.length, that.lengthSize);
            }

            const compressed = pako.deflate(message);
            const bufferLength = compressed.length + headerSize;
            const rawBuffer = Buffer.alloc(bufferLength);

            prepareHeader(bufferLength, rawBuffer);

            const arrayBuffer = toArrayBuffer(rawBuffer);
            const view = new Uint8Array(arrayBuffer, 0, bufferLength);
            for (let i = 0; i < compressed.length; i++) {
                view[i + headerSize] = compressed[i];
            }

            PosixSocket.send(this._sockfd, arrayBuffer, bufferLength, 0);
        } else {
            const bufferLength = message.length + this.lengthSize;

            const rawBuffer = Buffer.alloc(bufferLength);
            rawBuffer.writeUInt32BE(message.length, 0);

            const arrayBuffer = toArrayBuffer(rawBuffer);
            const view = new Uint8Array(arrayBuffer, 0, bufferLength);
            for (let i = 0; i < message.length; i++) {
                view[i + this.lengthSize] = message[i].charCodeAt(0);
            }

            PosixSocket.send(this._sockfd, arrayBuffer, bufferLength, 0);
        }
    }
}