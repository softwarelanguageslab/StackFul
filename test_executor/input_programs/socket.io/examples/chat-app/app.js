const express = require('express');
const app = express();
const http = require('http').Server(app);
const path = require('path');
const io = require('socket.io');
let chat;
const nickNames = {};
let namesUsed = [];
const currentRoom = {};
const chatServer = {};
chatServer.assignGuestName = function (socket, nickNames, namesUsed) {
  const assignGuestName_name = "Guest_1";
  nickNames[socket.id] = assignGuestName_name;
  socket.emit('nameResult', {
    success: true,
    name: assignGuestName_name
  });
  namesUsed.push(assignGuestName_name);
};
chatServer.handleClientDisconnection = function (socket) {
  socket.on('disconnect', () => {
    "DEBUGGING SERVER on disconnect";
    const nameIdx = namesUsed.indexOf(nickNames[socket.id]);
    delete nickNames[socket.id];
    namesUsed = namesUsed.slice(0, nameIdx).concat(namesUsed.slice(nameIdx + 1));
  });
};
chatServer.listRooms = function (socket) {
  const rooms = Object.keys(socket.rooms);
  return rooms.filter(r => r !== socket.id);
};
chatServer.handleNameChangeAttempts = function (socket, handleNameChangeAttempts_nickNames, namesUsed) {
  socket.on('nameAttempt', (nameAttempt_name) => {
    "DEBUGGING SERVER on nameAttempt";
    if (nameAttempt_name === 'guest') {
      'ERROR name cannot be "guest"';
    } else {
      if (!namesUsed.includes(nameAttempt_name)) {
        const prevName = handleNameChangeAttempts_nickNames[socket.id];
        const prevNameIdx = namesUsed.indexOf(prevName);
        handleNameChangeAttempts_nickNames[socket.id] = nameAttempt_name;
        namesUsed = namesUsed.slice(0, prevNameIdx).concat(namesUsed.slice(prevNameIdx + 1)).concat(nameAttempt_name);
        "DEBUGGING SERVER emit nameResult 1";
        socket.emit('nameResult', {
          success: true,
          name: nameAttempt_name
        });
        "DEBUGGING SERVER emit message 1";
        socket.emit('message', {
          text: prevName + " is now known as " + nameAttempt_name + "."
        });
      } else {
        "DEBUGGING SERVER emit nameResult 2";
        socket.emit('nameResult', {
          success: false,
          message: 'That name is already in use.'
        });
      }
    }
  })
}
chatServer.handleMessageBroadcast = function (socket) {
  socket.on('message', (message) => {
    "DEBUGGING SERVER on message";
    "DEBUGGING SERVER emit message 2";
    socket.emit('message', {
      text: nickNames[socket.id] + ": " + message.text
    });
  });
};
chatServer.handleRoomJoining = function (socket) {
  socket.on('join', (room) => {
    "DEBUGGING SERVER on join";
    socket.leave(currentRoom[socket.id]);
    this.joinRoom(socket, room.newRoom);
  })
}
chatServer.joinRoom = function (socket, room) {
  socket.join(room);
  currentRoom[socket.id] = room;
  socket.emit('joinResult', {room});
  socket.emit('message', {
    text: nickNames[socket.id] + " has joined " + room + "."
  });
  chat.of('/').in(room).clients((err, sockets) => {
    if (err) return console.error(err)
    const usersInRoom = sockets.map(sId => nickNames[sId]).join(', ')
    const usersInRoomSummary = "Users currently in " + room + ": "  + usersInRoom;
    "DEBUGGING SERVER emit message 3";
    socket.emit('message', {text: usersInRoomSummary});
  });
};
chatServer.listen = function (server) {
chat = io(server);
chat.on('connection', (socket) => {
  "DEBUGGING SERVER on connection";
  this.assignGuestName(
    socket, nickNames, namesUsed
  );
  this.joinRoom(socket, 'lobby')
  this.handleMessageBroadcast(socket)
  this.handleNameChangeAttempts(socket, nickNames, namesUsed)
  this.handleRoomJoining(socket)
  socket.on('rooms', () => {
    "DEBUGGING SERVER on rooms";
    let rooms = [];
  	for (let s in chat.sockets.sockets) {
  	  rooms = rooms.concat(this.listRooms(chat.sockets.sockets[s]));
  	}
  	rooms = Array.from(new Set(rooms));
    "DEBUGGING SERVER emit rooms";
    socket.emit('rooms', rooms);
  });
  this.handleClientDisconnection(socket);
	});
};
chatServer.listen(http);
const PORT = 3000;
app.use(express.static(__dirname + '/public'));
http.listen(PORT, () => {
  console.log("listening on " + PORT);
  "FINISHED_SETUP";
})