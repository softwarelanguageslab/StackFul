let express = require('express');
let http = require('http');
let path = require('path');
let socketIO = require('socket.io');
let app = express();
let server = http.Server(app);
let io = socketIO(server);
app.set('port', 3000);
app.use(express.static(__dirname + '/static'));
app.get('/', function(request, response) {
  response.sendFile(path.join(__dirname, '/static/index.html'));
});
server.listen(3000, function() {
  console.log('Starting server on port 3000');
  'FINISHED_SETUP';
});
let socket_id = 0;
let player_x = 300, player_y = 300, player_text = "", player_text_time = 0;
io.on('connection', function(socket) {
    socket_id++;
    socket.on('new player', function() {
      "DEBUGGING SERVER ON new player";
    });
    socket.on('movement', function(movement_up, movement_down, movement_left, movement_right) {
      "DEBUGGING SERVER ON movement";
      if (movement_left !== 0) {
        player_x -= 5;
      }
      if (movement_up !== 0) {
        player_y -= 5;
      }
      if (movement_right !== 0) {
        player_x += 5;
      }
      if (movement_down !== 0) {
        player_y += 5;
      }
    });
    socket.on('msg', function(msg_data) {
      "DEBUGGING SERVER ON msg";
      player_text = msg_data;
      player_text_time = 0;
    });
});