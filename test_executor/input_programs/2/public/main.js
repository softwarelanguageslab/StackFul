let var1 = 0;
let var2 = 0;
let var3 = 0;
let var4 = 0;

const x = Math.random()

function b1() {
	if(x > 250) {
		"DEBUGGING THEN-B1..."
	} else {
		"DEBUGGING ELSE-B1..."
	}
}

function b2() {
	if(x > 700) {
		"DEBUGGING THEN-B2..."
	} else {
		"DEBUGGING ELSE-B2..."
	}
}
 
document.getElementById("Button1").addEventListener("click", function (e) {
	b1();
})

document.getElementById("Button2").addEventListener("click", function (e) {
	b2();
})