'use strict';

(function() {
  "CLIENT ENTERED";

  var socket = io();
  var canvas = document.getElementsByClassName('whiteboard')[0];
  var colors = document.getElementsByClassName('color');
  var context = canvas.getContext('2d');

  const ownStartPos = {};
  const otherStartPos = {};

  var current = {
    color: 'black'
  };
  var drawing = false;

  canvas.addEventListener('mousedown', onMouseDown, false);
  canvas.addEventListener('mouseup', onMouseUp, false);
  canvas.addEventListener('mouseout', onMouseUp, false);
  canvas.addEventListener('mousemove', throttle(onMouseMove, 10), false);
  
  //Touch support for mobile devices
  // canvas.addEventListener('touchstart', onMouseDown, false);
  // canvas.addEventListener('touchend', onMouseUp, false);
  // canvas.addEventListener('touchcancel', onMouseUp, false);
  // canvas.addEventListener('touchmove', throttle(onMouseMove, 10), false);

  // for (var i = 0; i < colors.length; i++){
  //   colors[i].addEventListener('click', onColorUpdate, false);
  // }

  socket.on('drawing', onDrawingEvent);

  window.addEventListener('resize', onResize, false);
  onResize();

  function drawLine(x0, y0, x1, y1, color, emit){
    "DEBUGGING CLIENT drawLine 1";

    // context.beginPath();
    // context.moveTo(x0, y0);
    // context.lineTo(x1, y1);
    // context.strokeStyle = color;
    // context.lineWidth = 2;
    // context.stroke();
    // context.closePath();

    if (!emit) { return; }

    socket.emit("drawing", {
      x0: x0,
      y0: y0,
      x1: x1,
      y1: y1,
      color: "green"
    });
  }

  function onMouseDown(e) {
    drawing = true;
    current.x = e.clientX;
    current.y = e.clientY;
    ownStartPos.xx = e.clientX;
    ownStartPos.yy = e.clientY;
    "DEBUGGING CLIENT onMouseDown";
  }

  function onMouseUp(e){
    if (!drawing) { return; }
    drawing = false;
    drawLine(current.x, current.y, e.clientX, e.clientY, current.color, true);
    "DEBUGGING CLIENT onMouseUp 2";
  }

  function onMouseMove(e){
    if (!drawing) { return; }
    drawLine(current.x, current.y, e.clientX, e.clientY, current.color, true);
    current.x = e.clientX;
    current.y = e.clientY;
  }

  function onColorUpdate(e){
    current.color = e.target.className.split(' ')[1];
  }

  // limit the number of events per second
  function throttle(callback, delay) {
    return function() {
      callback.apply(null, arguments);
    };
  }

  function onDrawingEvent(data){
    otherStartPos.xxx = data.x0;
    otherStartPos.yyy = data.y0;
    drawLine(data.x0, data.y0, data.x1, data.y1, data.color, false);
    if ((otherStartPos.xxx) === 500 && (otherStartPos.yyy) === 500 && ownStartPos.xx === 500 && ownStartPos.yy === 500) {
      "ERROR";
    }
    "DEBUGGING CLIENT onDrawingEvent 2";
  }

  // make the canvas fill its parent
  function onResize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }

})();
