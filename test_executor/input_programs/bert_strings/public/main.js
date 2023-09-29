(function(){
	var textInput = document.getElementById("text_input");
	var val = textInput.value;
	function newRegExp(e) {
		var a = new RegExp(e);
		var result = a.test("aaa");
		return result;
	}
	function stringEq(s) {
		if (val === s) {
			"DEBUGGING stringEq then";
		} else {
			"DEBUGGING stringEq else";
		}
	}
	function stringConcat(s) {
		if (val === s + "abc") {
			"DEBUGGING stringConcat then";
		} else {
			"DEBUGGING stringConcat else";
		}
	}
	function stringLength(s) {
		if (val.length === s.length) {
			"DEBUGGING stringLength then";
		} else {
			"DEBUGGING stringLength else";
		}
	}
	function stringIndex(s) {
		if (val.indexOf("g") === 1) {
			"DEBUGGING stringIndex then";
		} else {
			"DEBUGGING stringIndex else";
		}
	}
	function stringSubstring(s) {
		if (val.substring(7, 13) === s.substring(7, 13)) {
			"DEBUGGING stringSubstring then";
		} else {
			"DEBUGGING stringSubstring else";
		}
	}
	function stringIncludes() {
		if (val.includes("ana")) {
			"DEBUGGING stringIncludes then";
		} else {
			"DEBUGGING stringIncludes else";
		}
	}
	function stringReplace(s) {
		var result = s.replace("Apple", "Banana");
		var result2 = result.replace("Kiwi", "Banana");
		if (val === result2) {
			"DEBUGGING stringReplace then";
		} else {
			"DEBUGGING stringReplace else";
		}
	}
	function stringCharAt(s) {
		if (val.charAt(1) === s.charAt(1)) {
			"DEBUGGING stringCharAt then";
		} else {
			"DEBUGGING stringCharAt else";
		}
	}
	function stringStartsWith(s) {
		if (val.startsWith("Saturday")) {
			"DEBUGGING stringStartsWith then";
		} else {
			"DEBUGGING stringStartsWith else";
		}
	}
	function stringEndsWith(s) {
		if (val.endsWith("plans")) {
			"DEBUGGING stringEndsWith then";
		} else {
			"DEBUGGING stringEndsWith else";
		}
	}
	stringEq("abc");
	stringConcat("def");
	stringIndex("abcdefghijklm");
	stringSubstring("Apple, Banana, Kiwi");
	stringIncludes();
	stringReplace("Apple, Banana, Kiwi");
	stringCharAt("Apple");
	stringStartsWith();
	stringEndsWith();
	newRegExp("a+");
})();