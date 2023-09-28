#!/bin/bash

BACKEND_HOME=/Users/mvdcamme/PhD/Projects/Concolic_Testing_Backend
# killall node -9
clear && printf '\e[3J'

FRONTEND_HOME=`pwd`
MAIN=$FRONTEND_HOME/built/src/main.js

npm run build


ITERATION_TIME=5400

for APPLICATION in fs_calculator fs_chat fs_insecure_chat fs_life fs_simple_chat fs_tohacks fs_totems fs_whiteboard; do
sleep 10

# Start backend
cd $BACKEND_HOME
sh ./shell.sh explore --log-print e --log-tree e --tree-output-path /Users/mvdcamme/PhD/Projects/JS_Concolic/output/merging_comparisons/improved/5400s/$APPLICATION/non_merging/1/symbolic_tree.dot &
BACKEND_PROCESS=$!
sleep 5

# Start frontend
cd $FRONTEND_HOME
node $MAIN -a $APPLICATION -m explore --explore-events -l EVT -l NDP --output-rel-path /Users/mvdcamme/PhD/Projects/JS_Concolic/output/merging_comparisons/improved/5400s/$APPLICATION/non_merging/1 &
FRONTEND_PROCESS=$!
sleep $ITERATION_TIME

sh kill.sh
killall -9 java
kill -s 9 $BACKEND_PROCESS
kill -s 9 $FRONTEND_PROCESS
killall -9 node
sleep 10
done
