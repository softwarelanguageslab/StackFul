"CLIENT ENTERED";
"DEBUGGING CLIENT start";
let ownPositionX = 0;
let ownPositionY = 0;
let otherX = 0;
let otherY = 0;
let state_canvasSize = [1200, 750];
let state_userText = "";
let state_chatHistory = [];
let state_usersNearby = false;
let state_showChat = false;
let state_totemList = {};
let state_playerWasPlaced = false;
const ownId = Math.random();
let socket = io();
let key;
socket.on("recieve totemList", totemList => {
  "DEBUGGING CLIENT recieve totemList";
  state_totemList = totemList;
});
socket.on("chat message", msg => {
  "DEBUGGING CLIENT chat message";
  state_showChat = true;
  state_chatHistory.push(msg);
});
socket.on("position update", (newX, newY) => {
  "DEBUGGING CLIENT position update";
  otherX = newX;
  otherY = newY;
  if (ownPositionX - otherX < 200) {
    if (ownPositionY - otherY < 200) {
      "DEBUGGING CLIENT state_usersNearby set to true";
      state_usersNearby = true;
    }
  }
});
let mouseX = 0;
let mouseY = 0;
function mouseInside(pos, dim) {
  if (mouseX > pos[0]) {
    if (mouseX < pos[0] + dim[0]) {
      if (mouseY > pos[1]) {
        if (mouseY < pos[1] + dim[1]) {
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
function insideCanvas(x, y) {
  if (x > 0) {
    if (x < state_canvasSize[0]) {
      if (y > 0) {
        if (y < state_canvasSize[1]) {
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
function mousePressed(e) {
  mouseX = e.clientX;
  mouseY = e.clientY;
  let nearbyPos = [700, 20];
  let nearbyDim = [200, 50];
  if (! insideCanvas(e.clientX, e.clientY)) return;
  if (state_usersNearby) {
    "DEBUGGING CLIENT state_usersNearby";
    if (mouseInside(nearbyPos, nearbyDim)) {
      "DEBUGGING CLIENT state_usersNearby and mouseInside";
      state_showChat = true;
      return;
    }
  }
  "DEBUGGING CLIENT 2.0"
  if (!state_playerWasPlaced) {
    "DEBUGGING CLIENT 2.1";
    socket.emit("get totem", ownId);
    state_playerWasPlaced = true;
    ownPositionX = mouseX;
    ownPositionY = mouseY;
  } else {
    "DEBUGGING CLIENT 2.2";
    socket.emit("position update", e.clientX, e.clientY);
  }
  "DEBUGGING CLIENT 2.end";
}
function keyPressed(e) {
  "DEBUGGING CLIENT key pressed";
  if (e.keyCode === 13) {
    "DEBUGGING CLIENT key 13";
    socket.emit("chat message", ownId, e.keyCode);
    state_userText = "";
  }
}
document.addEventListener("click", mousePressed);
document.addEventListener("keypress", keyPressed);
"DEBUGGING CLIENT 4";