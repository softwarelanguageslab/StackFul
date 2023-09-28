#!/bin/bash

clear && printf '\e[3J'

FRONTEND_DIR=`pwd`
FRONTEND_MAIN=$FRONTEND_DIR/built/src/main.js
BACKEND_DIR=$FRONTEND_DIR/../../Concolic_Testing_Backend
BACKEND_JAR=$BACKEND_DIR/target/scala-2.13/Concolic_Testing_Backend-assembly-0.1.jar

# Set the proper escape character for printing colour in the console
if [[ "$OSTYPE" == "linux-gnu" ]]; then
	COLOR_ESCAPE_CHAR=e
elif [[ "$OSTYPE" == "darwin"* ]]; then
	COLOR_ESCAPE_CHAR=x1B
fi

test_failed=false
exit_code=0
failed_at_application=none

do_tests() {
	for application in $applications
	do
		# killall node -9
		# clear && printf '\e[3J'
		# npm run build

		# Kill and relaunch the backend
		sh $BACKEND_DIR/kill.sh
		timeout -s KILL $sleep_length java -jar $BACKEND_JAR $scala_args &
		BACKEND_CODE=$?
		sleep 2

		# Launch the tester itself and give it some time to complete
		timeout -s KILL $sleep_length sh $FRONTEND_DIR/shell.sh -a $application $node_args
		FRONTEND_CODE=$?
		# sleep $sleep_length

		echo frontend_code $FRONTEND_CODE
		echo backend_code $BACKEND_CODE

		# Check whether either of the tester processes timed out
		if [ $FRONTEND_CODE -eq 124 ] || [ $BACKEND_CODE -eq 124 ]; then
			test_failed=true
			exit_code=124
			failed_at_application=$application
			if [ $FRONTEND_CODE -eq 124 ]; then
				echo -e "\\$COLOR_ESCAPE_CHAR[101mFrontend process was still running\\$COLOR_ESCAPE_CHAR[0m"
			else
				echo -e "\\$COLOR_ESCAPE_CHAR[101mBackend process was still running\\$COLOR_ESCAPE_CHAR[0m"
			fi
			exit $exit_code
		else
			# Inspect the exit code
			if [ $FRONTEND_CODE -eq 0 ] && [ $BACKEND_CODE -eq 0 ]; then
				exit_code=0
				echo -e "\\$COLOR_ESCAPE_CHAR[42mProcess finished successfully\\$COLOR_ESCAPE_CHAR[0m"
			else
				test_failed=true
				exit_code=1
				failed_at_application=$application
				echo -e "\\$COLOR_ESCAPE_CHAR[101mProcess finished with exit code $FRONTEND_CODE\\$COLOR_ESCAPE_CHAR[0m"
				exit $exit_code
			fi
		fi
	done
}

test_or_exit() {
	if $test_failed
	then
		echo -e "\\$COLOR_ESCAPE_CHAR[101mFailed at application $application\\$COLOR_ESCAPE_CHAR[0m"
		exit $exit_code
	else
		do_tests
	fi
}

applications="merging1 merging2 merging3 merging4 merging5 merging6 merging7"
scala_args="-m merge -i 9876 -o 9877"
node_args=" -m explore -t --merge"
sleep_length=300s
test_or_exit

applications="merging1 merging2 merging3 merging4 merging5 merging6 merging7"
scala_args="-m explore -i 9876 -o 9877"
node_args=" -m explore -t"
sleep_length=600s
test_or_exit

exit 0

# applications="fixt_merging6_19 fixt_merging6_20 fixt_merging6_106 fixt_merging6_107 fixt_merging6_109 fixt_merging6_110 merging_scoping5"
# scala_args="-m merge -i 9876 -o 9877"
# node_args="-t -m explore --merging"
# sleep_length=60
# test_or_exit

# applications="fixt_fs_verify_intra_1 fixt_fs_verify_intra_2"
# scala_args="-m verify -i 9876 -o 9877 -n 2"
# node_args="-t -m verify --initial-event --input-suffix=individual --intra-iterations=5"
# sleep_length=150
# test_or_exit

# applications="fixt_fs_verify_intra_3"
# scala_args="-m verify -i 9876 -o 9877 -n 2"
# node_args="-t -m verify --initial-event --input-suffix=individual --intra-iterations=20"
# sleep_length=300
# test_or_exit
