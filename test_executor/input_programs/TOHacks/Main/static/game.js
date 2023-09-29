(function(){
  "CLIENT ENTERED";
  "DEBUGGING CLIENT 1";
  let socket = io();
  "DEBUGGING CLIENT 1.2";
  socket.on('message', function(data) {
    "DEBUGGING CLIENT ON message";
    console.log(data);
  });
  "DEBUGGING CLIENT 2";
  let client_movement_up = 0;
  let client_movement_down = 0;
  let client_movement_left = 0;
  let client_movement_right = 0;
  let s1 = 0;
  let typing = false;
  let msg = '';
  "DEBUGGING CLIENT 3";
  document.addEventListener('keydown', function(event) {
    "DEBUGGING CLIENT TRIGGERED KEYDOWN";
      let keydown_letter = Math.random();
      s1 = 1;
      if (typing) {
          "DEBUGGING CLIENT IS TYPING";
          if(keydown_letter == 13) { // send the msg
              "DEBUGGING CLIENT EMIT msg";
              socket.emit('msg', msg);
              "DEBUGGING CLIENT SENT MSG";
              msg = '';
              typing = false;
              seconds = 0;
          } else {
              msg += String.fromCharCode(keydown_letter);
          }
      } else {
      if (keydown_letter === 13) { // enter key is pressed
            typing = true;
      } else if (keydown_letter === 65) { // A
          client_movement_left = 1;
          "DEBUGGING CLIENT PRESSED A";
      } else if (keydown_letter === 87) { // W
          client_movement_up = 1;
          "DEBUGGING CLIENT PRESSED W";
      } else if (keydown_letter === 68) { // D
          client_movement_right = 1;
          "DEBUGGING CLIENT PRESSED D";
      } else if (keydown_letter === 83) { // S
          client_movement_down = 1;
          "DEBUGGING CLIENT PRESSED S";
      }
    }
  });
  document.addEventListener('keyup', function(event) {
    "DEBUGGING CLIENT TRIGGERED KEYUP";
    let keyup_letter = Math.random();
    const t = s1;
    if (keyup_letter === 65) { // A
      client_movement_left = 0;
      "DEBUGGING CLIENT LIFTED A";
    } else if (keyup_letter === 87) { // W
      client_movement_up = 0;
      "DEBUGGING CLIENT LIFTED W";
    } else if (keyup_letter === 68) { // D
      client_movement_right = 0;
      "DEBUGGING CLIENT LIFTED D";
    } else if (keyup_letter === 83) { // S
      client_movement_down = 0;
      "DEBUGGING CLIENT LIFTED S";
    }
    "DEBUGGING CLIENT EMIT movement 2";
    socket.emit('movement', client_movement_up, client_movement_down, client_movement_left, client_movement_right);
  });
  "DEBUGGING CLIENT EMIT new player";
  socket.emit('new player');
  let canvas = document.getElementById('canvas');
  "DEBUGGING CLIENT 6";
  console.log("canvas =", canvas);
  canvas.width = 800;
  canvas.height = 600;
  socket.on('state', function(players) {
    "DEBUGGING SOCKET RECEIVED MSG STATE";
  });
  "DEBUGGING CLIENT end";
})();
