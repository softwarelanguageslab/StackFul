#!/bin/bash
JS_INPUT_SOCKET1=/tmp/test_socket_JS_input9877
JS_OUTPUT_SOCKET1=/tmp/test_socket_JS_output9876
JS_INPUT_SOCKET2=/tmp/test_socket_JS_input7789
JS_OUTPUT_SOCKET2=/tmp/test_socket_JS_output6789

# JS_INPUT_SOCKET=/Users/mvdcamme/PhD/Projects/JS_Concolic/test_socket_JS_input
# JS_OUTPUT_SOCKET=/Users/mvdcamme/PhD/Projects/JS_Concolic/test_socket_JS_output

rm -f $JS_INPUT_SOCKET1 $JS_OUTPUT_SOCKET1 $JS_INPUT_SOCKET2 $JS_OUTPUT_SOCKET2

PORT_9876=$(lsof -ti tcp:9876)
PORT_6789=$(lsof -ti tcp:6789)
if [[ ! -z "$PORT_9876" ]]; then
	kill -9 $PORT_9876
fi
if [[ ! -z "$PORT_6789" ]]; then
	kill -9 $PORT_6789
fi
