import {Node, SimpleCallExpression} from "estree";
import estraverse from "estraverse";

function makeNewCallee(originalCalleeNode, calleeIdentifier) {
  for (let key in originalCalleeNode) {
    delete originalCalleeNode[key];
  }
  originalCalleeNode.type = estraverse.Syntax.Identifier;
  originalCalleeNode.name = calleeIdentifier;
  return originalCalleeNode;
}

function makeMathRandom() {
  return {
    type: estraverse.Syntax.MemberExpression,
    object: {
      type: estraverse.Syntax.Identifier,
      name: "Math"
    },
    property: {
      type: estraverse.Syntax.Identifier,
      name: "random"
    }
  };
}

function makeRandomArgument() {
  return {
    type: estraverse.Syntax.CallExpression,
    callee: makeMathRandom(),
    arguments: []
  }
}

function makeArguments(nrOfArguments) {
  const theArguments = new Array(nrOfArguments);
  for (let i = 0; i < nrOfArguments; i++) {
    theArguments[i] = makeRandomArgument();
  }
  return theArguments;
}

function overwriteFunctionCall(ast: Node, start, end, calleeIdentifier, nrOfArguments) {
  estraverse.traverse(ast, {
    leave: function (node, parent) {
      if (node.loc!.start === start && node.loc!.end === end && node.type === estraverse.Syntax.CallExpression) {
        const castedNode = node as SimpleCallExpression;
        castedNode.callee = makeNewCallee(castedNode.callee, calleeIdentifier);
        castedNode.arguments = makeArguments(nrOfArguments);
      }
    }
  });
}

module.exports = overwriteFunctionCall;