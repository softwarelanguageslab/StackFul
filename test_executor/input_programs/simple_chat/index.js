const express = require('express');
let app = express();
let http = require('http').Server(app);
let io = require('socket.io')(http);
let port = 3000;
app.use(express.static(__dirname + '/public'));
  function shouldCensor(word) {
    if (word === "swear word") {
      return true;
    } else if (word.startsWith("swear")) {
      return true;
    } else {
      return false;
    }
  }
io.on('connection', function(socket){
    "DEBUGGING SERVER on connection";
    socket.on('chat_message', function(msg) {
        "DEBUGGING SERVER on chat_message";
        console.log("chat_message received", msg);
        if (shouldCensor(msg)) {
  		    "ERROR SERVER censored";
        } else {
            "DEBUGGING SERVER emit chat_message";
	       io.emit('chat_message', msg);
  	   }
    });
});
http.listen(port, function(){
  console.log('listening on *:' + port);
  "FINISHED_SETUP";
});