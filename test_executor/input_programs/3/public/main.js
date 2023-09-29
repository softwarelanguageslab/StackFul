let var1 = 0;

function f() {
	function b1() {
		// if (Math.random() < 9875) {} else {}
		var1 = 42;
		// if (Math.random() < 5675) {} else {}
		"DEBUGGING C b1";
		"EVENT_HANDLER_END";
	}

	function b2() {
		// if (Math.random() < 9871) {} else {}
		var1 = 98;
		// if (Math.random() < 5671) {} else {}
		"DEBUGGING C b2";
		"EVENT_HANDLER_END";
	}

	function b3() {
		// if (Math.random() < 9879) {} else {}
		if (var1 === 42) {
			"DEBUGGING C b3 then";
		} else {
			"DEBUGGING C b3 else";
		}
		// if (Math.random() < 5679) {} else {}
		
		"EVENT_HANDLER_END";
	}
	 
	document.getElementById("Button3").addEventListener("click", function (e) {
		b3();
	})
	document.getElementById("Button1").addEventListener("click", function (e) {
		b1();
	})
	document.getElementById("Button2").addEventListener("click", function (e) {
		b2();
	})
}

f();