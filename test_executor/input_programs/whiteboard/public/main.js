'use strict';
(function() {
  "CLIENT ENTERED";
  let socket = io();
  let canvas = document.getElementsByClassName('whiteboard')[0];
  let colors = document.getElementsByClassName('color');
  let context = canvas.getContext('2d');
  let currentx = 0, currenty = 0, ownStartPosxx = 0, ownStartPosyy = 0, otherStartPosxxx = 0, otherStartPosyyy = 0;
  let currentcolor = 0;
  let drawing = false;
  canvas.addEventListener('mousedown', onMouseDown, false);
  canvas.addEventListener('mouseup', onMouseUp, false);
  canvas.addEventListener('mouseout', onMouseUp, false);
  canvas.addEventListener('mousemove', onMouseMove, false);
  "DEBUGGING CLIENT before socket.on drawing";
  socket.on('drawing', onDrawingEvent);
  "DEBUGGING CLIENT before windowSize";
  const windowSize = 1000;
  "DEBUGGING CLIENT before checkCoord";
  function checkCoord(checkCoord_x, checkCoord_y) {
    if (checkCoord_x >= 0) {
      if (checkCoord_y >= 0) {
        if (checkCoord_x <= windowSize) {
          if (checkCoord_y <= windowSize) {
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  "DEBUGGING CLIENT before drawLine";
  function drawLine(drawLine_x0, drawLine_y0, drawLine_x1, drawLine_y1){
    "DEBUGGING CLIENT drawLine 1";
    if (! checkCoord(drawLine_x1, drawLine_y1)) {
      console.log("DEBUGGING end-coordinate out of bounds");
      return;
    }
    "DEBUGGING CLIENT emit drawing";
    socket.emit("drawing", drawLine_x0, drawLine_y0, drawLine_x1, drawLine_y1);
  }
  "DEBUGGING CLIENT before onMouseDown";
  function onMouseDown(e) {
    drawing = true;
    currentx = e.clientX;
    currenty = e.clientY;
    ownStartPosxx = e.clientX;
    ownStartPosyy = e.clientY;
  }
  function onMouseUp(e){
    if (!drawing) { return; }
    drawing = false;
    drawLine(currentx, currenty, e.clientX, e.clientY);
  }
  "DEBUGGING CLIENT before onMouseMove";
  function onMouseMove(e){
    if (!drawing) { return; }
    drawLine(currentx, currenty, e.clientX, e.clientY);
    currentx = e.clientX;
    currenty = e.clientY;
  }
  "DEBUGGING CLIENT before onDrawingEvent";
  function onDrawingEvent(onDrawingEvent_x0, onDrawingEvent_y0, onDrawingEvent_x1, onDrawingEvent_y1){
    "DEBUGGING CLIENT on onDrawingEvent";
    otherStartPosxxx = onDrawingEvent_x0;
    otherStartPosyyy = onDrawingEvent_y0;
  }
  "DEBUGGING CLIENT before onResize";
  function onResize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  onResize();
})();