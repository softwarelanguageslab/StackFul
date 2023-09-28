import Estraverse from "estraverse";

export const cache = new Map();
function addToCache(serial, nrOfLines) {
  cache.set(serial, nrOfLines);
}
function isCached(serial) {
  return cache.has(serial);
}
function getFromCache(serial) {
  return cache.get(serial);
}

function addLine(line, lines) {
  lines.add(line);
}

function processNodes(nodes, lines) {
  for (let idx in nodes) {
    const arg = nodes[idx];
    if (arg && arg.loc) {
      addLine(arg.loc.start.line, lines);
      addLine(arg.loc.end.line, lines);
    }
  }
}

function makeVisitor() {
  const lines = new Set();
  const visitor = {
    enter: function() {
      processNodes(arguments, lines);
    },
    leave: function() {
      processNodes(arguments, lines);
    },
    getNrOfLines: () => lines.size
  }
  return visitor;
}

export default function traverse(root) {
  if (isCached(root.AranSerial)) {
    return getFromCache(root.AranSerial);
  } else {
    const visitor = makeVisitor();
    Estraverse.traverse(root, visitor);
    const nrOfLines = visitor.getNrOfLines();
    addToCache(root.AranSerial, nrOfLines);
    return nrOfLines;
  }
}

