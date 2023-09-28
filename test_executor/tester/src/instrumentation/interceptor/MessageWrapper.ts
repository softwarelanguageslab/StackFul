export default class MessageWrapper {
    private static readonly MARK = "++MARKED++";

    static wrapMessage(processId, messageId, message) {
        return [this.MARK, processId, messageId, message];
    }

    static getMessageMark(msg) {
        return msg[0];
    }

    static isWrappedMessage(msg) {
        return Array.isArray(msg) && msg.length >= 4 && this.getMessageMark(msg) === this.MARK;
    }

    static getMessageProcessId(msg) {
        return msg[1];
    }

    static getMessageId(msg) {
        return msg[2];
    }

    static getMessageValues(msg) {
        return msg[3];
    }

}





