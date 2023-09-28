import * as SymTypes from "./supported_symbolic_types";
import {SymbolicExpression} from "./symbolic_expressions";

export default class SymbolicMessageInput extends SymbolicExpression {
    private messageType: any;
    private id: any;
    private processId: any;
    private message_input_type: string;

    constructor(messageType, id, processId) {
        super("SymbolicMessageInput");
        this.messageType = messageType;
        this.id = id;
        this.processId = processId;
        this.message_input_type = SymTypes.SupportedSymbolicExpressionTypes.Int;
    }

    toString() {
        return `SymbolicMessageInput(messageType: ${this.messageType}, id: ${this.id}), pid: ${this.processId}`;
    }

    isSameSymValue(other) {
        return (other instanceof SymbolicMessageInput) && this.messageType === other.messageType && this.id === other.id && this.processId === other.processId;
    }

    instantiate(ignored) {
        return new SymbolicMessageInput(this.messageType, this.id, this.processId);
    }
}