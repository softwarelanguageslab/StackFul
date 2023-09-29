(function(){

	// 3 buttons
	var button1 = document.getElementById("Button1");
	var button2 = document.getElementById("Button2");
	var button3 = document.getElementById("Button3");

	button1.addEventListener("click", function(){ pressedButton(1); });
	button2.addEventListener("click", function(){ pressedButton(2); });
	button3.addEventListener("click", function(){ pressedButton(3); });

	function pressedButton(n) {
		console.log("[click] You pressed button " + n);
		"DEBUGGING CLIENT [click] You pressed button";
	}

	// globe image
	var image = document.getElementById("globe");
	image.addEventListener("mouseover", bigImage);
	image.addEventListener("mouseout", smallImage);

	function bigImage() {
		image.style.height = "64px";
	    image.style.width  = "64px";
		"DEBUGGING CLIENT [mouseover]";
	}

	function smallImage() {
		image.style.height = "32px";
	    image.style.width  = "32px";
		"DEBUGGING CLIENT [mouseout]";
	}
/* same as a 'click'

		<form id="submitButton">
		    Enter your name: <input id="fname" type="text" size="20">
		    <input type="submit">
		</form>
		<br>

	// submit button
	var submitButton = document.getElementById("submitButton");
	var fname = document.forms[0].fname.value;
	submitButton.addEventListener("submit", submitInput);

	function submitInput() {
		fname = document.forms[0].fname.value;
		alert ("Hello " + fname);
	}
*/
	// select input
	var inputField = document.getElementById("inputField");
	inputField.addEventListener("select", selectInput);

	function selectInput() {
		console.log("[select] You selected some input");
		"DEBUGGING CLIENT [select] You selected some input";
	} // select events can be dispatched only on form <input type="text"> and <textarea> elements.

	// which mousebutton
	var whichButton = document.getElementById("whichButton");
	whichButton.addEventListener("click", function(e){ "DEBUGGING CLIENT [click]"; button012(e) });
	whichButton.addEventListener("contextmenu", function(e){ "DEBUGGING CLIENT [contextmenu]"; button012(e) });
	whichButton.addEventListener("auxclick", function(e){ "DEBUGGING CLIENT [auxclick]"; button012(e) });

	function button012(event) {
	  console.log("[click/contextmenu/auxclick] You pressed button: " + event.button);
	  "DEBUGGING CLIENT [click/contextmenu/auxclick] You pressed button";
	}

	// mouse coordinates
	var rect = document.getElementById("coordiv");
	rect.addEventListener("mousemove", function(e){ "DEBUGGING CLIENT [mousemove]"; drawCoor(e) });
	rect.addEventListener("mouseout", function(e){ "DEBUGGING CLIENT [mouseout]"; clearCoor(e) });

	function drawCoor(e) {
	  x = e.clientX;
	  y = e.clientY;
	  coor = "Coordinates: (" + x + "," + y + ")";
	  document.getElementById("demoCoor").innerHTML = coor
	  "DEBUGGING CLIENT [drawCoor]";
	}

	function clearCoor() {
	  document.getElementById("demoCoor").innerHTML = "";
	  "DEBUGGING CLIENT [clearCoor]";
	}

	// double click
	var dbcSentence = document.getElementById("dbevent");
	dbcSentence.addEventListener("dblclick", dbClick);

	function dbClick() {
	  console.log("[dblclick] You double clicked!");
	  "DEBUGGING CLIENT [dblclick] You double clicked!";
	}

	// Key up/down
	var inputUpDown = document.getElementById("keyUpDown");
	inputUpDown.addEventListener("keydown", function(e){ keyDown(e) });
	inputUpDown.addEventListener("keyup", function(e){ keyUp(e) });

	function keyDown(event){
		inputUpDown.style.background = 'red';
		console.log("[keydown] Key '" + event.key + "' pressed");
	    "DEBUGGING CLIENT [keydown] Key pressed";
	}

	function keyUp(event){
		inputUpDown.style.background = '';
		console.log("[keyup] Key '" + event.key + "' released");
		"DEBUGGING CLIENT [keyup] Key released";
	}

	// onblur
	var blurField = document.getElementById("onblurField");
	blurField.addEventListener("blur", leaveInputField);

	function leaveInputField() {
		blurField.value = blurField.value.toUpperCase();
   	    console.log("[blur] element has lost focus");
		"DEBUGGING CLIENT [blur] element has lost focus";
	}

 	// onchange
 	var dropMenu = document.getElementById("dropdown");
 	dropMenu.addEventListener("change", changeDropMenu);

 	function changeDropMenu() {
 		console.log("[change] You changed the value to '" + dropMenu.value + "'");
		"DEBUGGING CLIENT [change] You changed the value to";
 	}

	// onfocus
	var focusField = document.getElementById("focusField");
	focusField.addEventListener("focus", inputFieldFocus);

	function inputFieldFocus() {
		console.log("[focus] element has received focus");
		"DEBUGGING CLIENT [focus] element has received focus";
	}

	//onreset
	var resetField = document.getElementById("resetForm");
	resetField.addEventListener("reset", formReset);

	function formReset() {
		console.log("[reset] input field reset]");
		"DEBUGGING CLIENT [reset] input field reset]";
	}

   // onresize
   window.addEventListener("resize", resizeWindow);

   function resizeWindow(){
     console.log('[resize] window has been resized!');
	 "DEBUGGING CLIENT [resize] window has been resized!";
   }

   // onload
   window.addEventListener("load", loadWindow);

   function loadWindow(){
     console.log('[load] Element succesfully loaded!')
	 "DEBUGGING CLIENT [load] Element succesfully loaded!";
   }
})();



/*
function onDocumentDoubleClick(){
  var targLink = document.getElementById("dbevent");
  var clickEvent = document.createEvent('MouseEvents');
  clickEvent.initEvent('dblclick', true, true);
  targLink.dispatchEvent(clickEvent);
}
*/
