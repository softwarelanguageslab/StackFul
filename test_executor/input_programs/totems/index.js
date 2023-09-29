let express = require("express");
let app = express();
let http = require("http").Server(app);
let io = require("socket.io")(http);
let port = 3000;
app.use(express.static(__dirname + '/public'));
let connectCounter = 0
let totemList = {};
let canvasSize = [1200, 750];
function insideCanvas(x, y) {
  if (x > 0) {
    if (x < canvasSize[0]) {
      if (y > 0) {
        if (y < canvasSize[1]) {
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
io.on("connection", function(socket) {
  "DEBUGGING SERVER CONNECTION";
  socket.on("chat message", function(id, msg) {
    "DEBUGGING SERVER chat message";
    io.emit("chat message", msg);
    io.send();
  });
  socket.on("position update", function(x, y) {
    "DEBUGGING SERVER position update";
    if (insideCanvas(x, y)) {
      "DEBUGGING SERVER insideCanvas";
      io.emit("position update", x, y);
    }
  });
  socket.on("get totem", (id) => {
    "DEBUGGING SERVER get totem start";
    connectCounter++
    console.log("clientCount", connectCounter);
    "DEBUGGING SERVER get totem 2";
    if (connectCounter % 2 === 1) {
      "DEBUGGING SERVER get totem 2.T.1";
      totemList[id] = "bird";
    } else {
      "DEBUGGING SERVER get totem 2.F.1";
      totemList[id] = "monkey";
    }
    "DEBUGGING SERVER get totem 3";
    io.emit("recieve totemList", totemList);
    "DEBUGGING SERVER get totem end";
  });
});
http.listen(port, function() {
  console.log("listening on *:" + port);
  "FINISHED_SETUP"; //trap this
});
"DEBUGGING SERVER end";