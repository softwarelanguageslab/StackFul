#!/bin/bash

DEPENDENCY_GRAPH_BIN=./node_modules/dependency-cruiser/bin/dependency-cruise.js
TESTER_PATH=../tester

$DEPENDENCY_GRAPH_BIN -x "node_modules|built" -T dot $TESTER_PATH/src/main.ts | dot -T svg > dependencies.svg