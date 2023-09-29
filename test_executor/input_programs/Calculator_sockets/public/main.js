"CLIENT ENTERED";
"DUMMY LINE";
let socket = io();
let inputLeft = 0;
let inputOp = -10;
let inputRight = 0;
let leftInitialised = false;
const resultElement = document.getElementById("Result");
function isAnOperator(isAnOperator_value) {
	if (isAnOperator_value === -1) {
		return true;
	} else if (isAnOperator_value === -2) {
		return true;
	} else if (isAnOperator_value === -3) {
		return true;
	} else if (isAnOperator_value === -4) {
		return true;
	} else if (isAnOperator_value === -5) {
		return true;
	} else {
		return false;
	}
}
function compute() {
	if (! leftInitialised) {
		resultElement.innerHTML = "Invalid expression";
	} else if (inputOp === -4) {
		if (inputRight === 0) {
			resultElement.innerHTML = "Cannot divide by zero";
		} else {
			"DEBUGGING CLIENT emit 1";
			socket.emit("compute", inputOp, inputLeft, inputRight);
		}
	} else if (! isAnOperator(inputOp)) {
		resultElement.innerHTML = "Not applying an operator";
	} else {
		"DEBUGGING CLIENT emit 2";
		socket.emit("compute", inputOp, inputLeft, inputRight);
	}
	inputLeft = 0;
	inputOp = -10;
	inputRight = 0;
	leftInitialised = false;
}
function handleComputation(handleComputation_value) {
	"DEBUGGING CLIENT handleComputation 1";
	if (! isAnOperator(handleComputation_value)) {
		const number = (leftInitialised) ? inputRight : inputLeft;
		const newNumber = (number * 10) + handleComputation_value;
		if (leftInitialised) {
			inputRight = newNumber;
		} else {
			inputLeft = newNumber;
		}
		resultElement.innerHTML = newNumber;
	} else if (handleComputation_value === -5) {
		compute();
	} else {
		leftInitialised = true;
		inputOp = handleComputation_value;
		inputRight = 0;
		resultElement.innerHTML = resultElement.innerHTML + " " + handleComputation_value + " ";
	}
}
socket.on("computation_result", function (data) {
	"DEBUGGING C computation_result";
	resultElement.innerHTML = data;
});
"DEBUGGING CLIENT before Button0 registration";
document.getElementById("Button0").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button0";
	handleComputation(0);
	"EVENT_HANDLER_END"
})
"DEBUGGING CLIENT before Button/ registration";
document.getElementById("Button/").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button/";
	handleComputation(-4);
	"EVENT_HANDLER_END"
})
"DEBUGGING CLIENT before Button= registration";
document.getElementById("Button=").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button=";
	handleComputation(-5);
	"EVENT_HANDLER_END"
})
document.getElementById("Button1").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button1";
	handleComputation(1);
	"EVENT_HANDLER_END"
})
document.getElementById("Button+").addEventListener("click", function (e) {
	handleComputation(-1);
	"EVENT_HANDLER_END"
})
document.getElementById("Button2").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button2";
	handleComputation(2);
	"EVENT_HANDLER_END"
})
document.getElementById("Button3").addEventListener("click", function (e) {
	"DEBUGGING CLIENT Button3";
	handleComputation(3);
	"EVENT_HANDLER_END"
})
"DEBUGGING CLIENT before Button4 registration";
document.getElementById("Button4").addEventListener("click", function (e) {
	handleComputation(4);
	"EVENT_HANDLER_END"
})
document.getElementById("Button5").addEventListener("click", function (e) {
	handleComputation(5);
	"EVENT_HANDLER_END"
})
document.getElementById("Button6").addEventListener("click", function (e) {
	handleComputation(6);
	"EVENT_HANDLER_END"
})
document.getElementById("Button7").addEventListener("click", function (e) {
	handleComputation(7);
	"EVENT_HANDLER_END"
})
document.getElementById("Button8").addEventListener("click", function (e) {
	handleComputation(8);
	"EVENT_HANDLER_END"
})
document.getElementById("Button9").addEventListener("click", function (e) {
	handleComputation(9);
	"EVENT_HANDLER_END"
})
document.getElementById("Button-").addEventListener("click", function (e) {
	handleComputation(-2);
	"EVENT_HANDLER_END"
})
document.getElementById("Button*").addEventListener("click", function (e) {
	handleComputation(-3);
	"EVENT_HANDLER_END"
})
"DEBUGGING CLIENT after Button registration";