import {IfStatement, LogicalExpression, Node, WhileStatement} from "estree";
import * as GTS from "@src/tester/global_tester_state";

export default class EstreeOperations {
    // Estree API documentation:
    // https://web.archive.org/web/20210314002546/https://developer.mozilla.org/en-US/docs/Mozilla/Projects/SpiderMonkey/Parser_API

    protected _nodes: any[];
    constructor(aran) {
        this._nodes = aran.nodes;
    }

    public getParameterName(serial: number, idx: number) {
        return this._nodes[serial].params[idx].name;
    }

    public refersToFunDeclaration(serial: number): boolean {
        const aranNode = this._nodes[serial];
        return aranNode && aranNode.type === "FunctionDeclaration";
    }

    // Returns false if aranNode is not an assignment node
    public getAssignmentIdentifier(aranNode) {
        const type = aranNode.type;
        if (type === "AssignmentExpression" && aranNode.left.type === "Identifier") {
            return aranNode.left.name;
        } else if (aranNode.type === "ExpressionStatement") {
            return this.getAssignmentIdentifier(aranNode.expression);
        } else {
            return false;
        }
    }

    public isUserAssignment(identifier: string, serial: number): boolean {
        function isAssignmentType(type: string): boolean {
            switch (type) {
                case "AssignmentExpression": return true;
                case "ForStatement": return true; // For increment or initialisation statements
                case "UpdateExpression": return true;
                case "VariableDeclaration": return true;
                default: return false;
            }
        }
        const node = this._nodes[serial];
        if (node) {
            if (isAssignmentType(node.type)) {
                return true;
            } else if (node.type === "ExpressionStatement") {
                return isAssignmentType(node.expression.type);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Types of nodes not completely correct: aran actually inserts additional fields (e.g., AranSerial and AranParentSerial) to all nodes
    protected getIfStatementFrameSerial(node: IfStatement, testWasTrue: boolean): number | undefined {
        function isBlockStatement(node: Node): boolean {
            return node.type === "BlockStatement";
        }
        function selectAranNodeSerial(nodeToEvaluateNext): number | undefined {
            if (nodeToEvaluateNext === null) {
                return undefined;
            } else if (isBlockStatement(nodeToEvaluateNext)) {
                return nodeToEvaluateNext.AranSerial;
            } else {
                return (node as any).AranSerial;
            }
        }
        const selectedSerial = (testWasTrue) ? selectAranNodeSerial(node.consequent) : selectAranNodeSerial(node.alternate);

        /*
        Immediately set the appropriate flag so that the leave trap for the then- or else-statement can find this serial.
         */
        if (selectedSerial !== undefined) {
            GTS.globalTesterState?.setBranchSerialToEncounter(selectedSerial);
        }
        return selectedSerial;
    }

    protected getForStatementFrameSerial(node: WhileStatement, testWasTrue: boolean): number | undefined {
        if (testWasTrue) {

        } else {
            return undefined;
        }
    }

    protected getLogicalExpressionFrameSerial(node: LogicalExpression, testWasTrue: boolean): number | undefined {
        if (testWasTrue) {
            return undefined;
        } else {
            return undefined;
        }
    }

    protected getWhileStatementFrameSerial(node: WhileStatement, testWasTrue: boolean): number | undefined {
        if (testWasTrue) {

        } else {
            return undefined;
        }
    }

    public getBranchFrameSerial(branchStatementSerial: number, testWasTrue: boolean): number | undefined {
        const node = this._nodes[branchStatementSerial];
        switch (node.type) {
            case "ForStatement": return this.getForStatementFrameSerial(node, testWasTrue);
            case "IfStatement": return this.getIfStatementFrameSerial(node, testWasTrue);
            case "LogicalExpression": return this.getLogicalExpressionFrameSerial(node, testWasTrue);
            case "WhileStatement": return this.getWhileStatementFrameSerial(node, testWasTrue);
            default: throw new Error(`Unsupported node type encountered: ${node.type}`);
        }
    }

    public userDefinedTest(serial: number): boolean {
        const node = this._nodes[serial];
        if (node) {
            return node.type === "IfStatement" ||
                   node.type === "LogicalExpression" ||
                   node.type === "ForStatement" ||
                   node.type === "WhileStatement";
        } else {
            return false;
        }
    }
}