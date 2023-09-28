#!/bin/bash

JAR_LOCATION=./target/scala-2.13/Concolic_Testing_Backend-assembly-0.1.jar
# BACKEND=./target/pack/bin/backend

clear && printf '\e[3J'
sh kill.sh

# JAVA_OPTS="$JAVA_OPTS -J-Xmx14g -XX:+HeapDumpOnOutOfMemoryError"
# BW_JAVA_OPTS="-J-Xmx14g -XX:+HeapDumpOnOutOfMemoryError"

if [[ $1 == verify ]]
then
	java -jar $JAR_LOCATION -m $1 -i 9876 -o 9877 -n 2 "${@:2}"
else
	java -jar $JAR_LOCATION -m $1 -i 9876 -o 9877 "${@:2}"
fi
