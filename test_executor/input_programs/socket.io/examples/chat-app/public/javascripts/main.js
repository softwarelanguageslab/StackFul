"CLIENT ENTERED";
"DEBUGGING CLIENT 1";
function Chat (socket) {
  this.socket = socket;
}
Chat.prototype.sendMessage = function (room, msg) {
  "DEBUGGING CLIENT emit message";
  this.socket.emit('message', {text: msg, room: room});
}
Chat.prototype.changeRoom = function (room) {
  "DEBUGGING CLIENT emit join";
  this.socket.emit('join', {newRoom: room});
}
Chat.prototype.processCommand = function (command, arg) {
  const parsedCmd = command;
  let msg = false;
  if (parsedCmd === "/join") {
    this.changeRoom(arg);
  } else if (parsedCmd === "/nick") {
    "DEBUGGING CLIENT emit nameAttempt";
    this.socket.emit('nameAttempt', arg);
  } else {
    msg = true;
  }
  return msg;
}
"DEBUGGING CLIENT 2";
function ChatUI (socket) {
  this.chat = new Chat(socket)
  this.form = document.getElementById('form_button');
  this.msgList = document.querySelector('ul#msg-list');
  this.roomList = document.querySelector('ul#room-list');
  this.input = document.querySelector('input');
  this.room = document.querySelector('#room');
  this.submitHandler();
}
ChatUI.prototype.getInput = function () {
  "DEBUGGING CLIENT getInput";
  return this.input.value;
}
ChatUI.prototype.getInput2 = function () {
  "DEBUGGING CLIENT getInput2";
  return this.input.value;
}
ChatUI.prototype.setRoom = function (room) {
  this.room.textContent = room;
}
ChatUI.prototype.sendMsg = function (room) {
  this.chat.sendMessage(room, this.getInput());
}
ChatUI.prototype.addMsg = function (msg) {
  const newMessage = document.createElement('li');
  newMessage.textContent = msg;
  this.msgList.appendChild(newMessage);
}
ChatUI.prototype.addRoom = function (room) {
  const newRoom = document.createElement('li');
  newRoom.textContent = room;
  this.roomList.appendChild(newRoom);
}
ChatUI.prototype.submitHandler = function () {
  "DEBUGGING CLIENT submitHandler";
  document.getElementById("form_button").addEventListener('mousedown', (e) => {
    e.preventDefault();
    this.processUserInput();
  });
}
ChatUI.prototype.processUserInput = function () {
  "DEBUGGING CLIENT processUserInput 1";
  const msg = this.getInput();
  let response;
  if (msg[0] === '/') {
    "DEBUGGING CLIENT processUserInput /";
    response = this.chat.processCommand(msg, this.getInput2());
    if (response) {
      this.addMsg(response);
    }
  } else {
    this.sendMsg(this.room.textContent);
    this.addMsg(msg);
  }
}
document.addEventListener('DOMContentLoaded', () => {
  const socket = io();
  const myChat = new ChatUI(socket);
  socket.on('nameResult', (result) => {
    "DEBUGGING CLIENT on nameResult";
    let msg;
    if (result.success) {
      msg = "Name changed to " + result.name + ".";
    } else {
      msg = result.message;
    }
    myChat.addMsg(msg);
  })
  socket.on('joinResult', (result) => {
    "DEBUGGING CLIENT on joinResult";
    myChat.setRoom(result.room);
    myChat.addMsg('Room changed.');
  })
  socket.on('message', (message) => {
    "DEBUGGING CLIENT on message";
    myChat.addMsg(message.text);
  })
  socket.on('rooms', (rooms) => {
    "DEBUGGING CLIENT on rooms";
    myChat.roomList.innerHTML = '';
    rooms.forEach(room => myChat.addRoom(room));
  })
  myChat.input.focus();
  "DEBUGGING CLIENT emit rooms";
  socket.emit('rooms', 42);
})