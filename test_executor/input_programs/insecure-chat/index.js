const express = require('express');
const app = express();
const path = require('path');
const server = require('http').createServer(app);
const io = require('socket.io')(server);
const port = process.env.PORT || 3000;
const users = {};
function User(User_name) {
    this.name = User_name;
    this.active = false;
    this.subscriptions = [];
}
User.prototype.getSubscriptions = function() {
    return this.subscriptions;
}
User.prototype.addSubscription = function(addSubscription_room) {
    const addSubscription_room_id = addSubscription_room.getId();
    if (this.subscriptions.indexOf(addSubscription_room_id) === -1) {
        this.subscriptions.push(addSubscription_room_id);
    }
}
User.prototype.removeSubscription = function(removeSubscription_room) {
    const removeSubscription_room_id = removeSubscription_room.getId();
    const idx = this.subscriptions.indexOf(removeSubscription_room_id);
    if (idx >= 0){
        this.subscriptions.splice(idx, 1);
    }
}
User.prototype.setActiveState = function(b) {
    this.active = b;
}
function addUser(addUser_name) {
    const addUser_user = new User(addUser_name);
    users[addUser_name] = addUser_user;
    return addUser_user;
}
function getUser(getUser_name) {
    return users[getUser_name];
}
function getUsers() {
    return Object.values(users);
}
const rooms = [];
let roomIdCounter = 0;
function Room(Room_id, Room_name, options) {
    this.id   =  Room_id;
    this.name =  Room_name;
    this.description = options.description || "";
    this.forceMembership = !!options.forceMembership;
    this.private         = !!options.private;
    this.direct          = !!options.direct;
    this.members = [];
    this.history = [];
}
Room.prototype.getId = function() {
    return this.id;
}
Room.prototype.getMembers = function() {
    return this.members;
}
Room.prototype.getMemberCount = function() {
    return this.members.length;
}
Room.prototype.addMember = function(addMember_user) {
    if (this.members.indexOf(addMember_user.name) === -1) {
        this.members.push(addMember_user.name);
    }
}
Room.prototype.removeMember = function(removeMember_user) {        
    const idx = this.members.indexOf(removeMember_user.name);
    if (idx >= 0) {
        this.members.splice(idx, 1);
    }
}
Room.prototype.getHistory = function() {
    return this.history;
}
Room.prototype.addMessage = function(addMessage_msg) {
    this.history.push(addMessage_msg);
}
function addRoom(addRoom_name, options) {
    const addRoom_id = roomIdCounter++;
    const addRoom_room = new Room(addRoom_id, addRoom_name, options);
    rooms[addRoom_id] = addRoom_room;
    return addRoom_room;
}
function getRooms() {
    return rooms;
}
function getForcedRooms() {
    return rooms.filter(r => r.forceMembership);
}
function getRoom(getRoom_id) {
    return rooms[getRoom_id];
}
function setup() {
    addRoom("general", {forceMembership: true, description: "boring stuff"});
    addRoom("random" , {forceMembership: true, description: "random!"});
    addRoom("private" ,{forceMembership: true, description: "some private channel", private: true});
}
setup();
app.use(express.static(path.join(__dirname, 'public')));
function sendToRoom(sendToRoom_room, event, data) {
    io.to('room' + sendToRoom_room.getId()).emit(event, data);
}
function newUser(newUser_name) {
    const newUser_user = addUser(newUser_name);
    const rooms = getForcedRooms();
    rooms.forEach(function(newUser_room) {
        return addUserToRoom(newUser_user, newUser_room);
    });
    return newUser_user;
}
function newRoom(newRoom_name, newRoom_user, options) {
    const newRoom_room = addRoom(newRoom_name, options);
    addUserToRoom(newRoom_user, newRoom_room);
    return newRoom_room;
}
function newChannel(newChannel_name, description, private, newChannel_user) {
    return newRoom(newChannel_name, newChannel_user, {
        description: description,
        private: private
    });
}
function newDirectRoom(newDirectRoom_user_a, newDirectRoom_user_b) {
    const newDirectRoom_room = addRoom(`Direct-${ newDirectRoom_user_a.name }-${ newDirectRoom_user_b.name }`, {
        direct: true,
        private: true
    });
    addUserToRoom(newDirectRoom_user_a, newDirectRoom_room);
    addUserToRoom(newDirectRoom_user_b, newDirectRoom_room);
    return newDirectRoom_room;
}
function getDirectRoom(user_a, user_b) {
    const rooms = getRooms().filter(function(r) { return r.direct && (r.members[0] == user_a.name && r.members[1] == user_b.name || r.members[1] == user_a.name && r.members[0] == user_b.name)});
    if (rooms.length == 1) {
        return rooms[0];
    } else
        return newDirectRoom(user_a, user_b);
}
function addUserToRoom(addUserToRoom_user, addUserToRoom_room) {
    addUserToRoom_user.addSubscription(addUserToRoom_room);
    addUserToRoom_room.addMember(addUserToRoom_user);
    sendToRoom(addUserToRoom_room, 'update_user', {
        room: addUserToRoom_room.getId(),
        username: addUserToRoom_user,
        action: 'added',
        members: addUserToRoom_room.getMembers()
    });
}
function removeUserFromRoom(removeUserFromRoom_user, removeUserFromRoom_room) {
    removeUserFromRoom_user.removeSubscription(removeUserFromRoom_room);
    removeUserFromRoom_room.removeMember(removeUserFromRoom_user);
    sendToRoom(removeUserFromRoom_room, 'update_user', {
        room: removeUserFromRoom_room.getId(),
        username: removeUserFromRoom_user,
        action: 'removed',
        members: removeUserFromRoom_room.getMembers()
    });
}
function addMessageToRoom(addMessageToRoom_roomId, msg, addMessageToRoom_username) {
    const addMessageToRoom_room = getRoom(addMessageToRoom_roomId);
    msg.time = new Date().getTime();
    if (addMessageToRoom_room) {
        sendToRoom(addMessageToRoom_room, 'new message', {
            username: addMessageToRoom_username,
            message: msg.message,
            room: msg.room,
            time: msg.time,
            direct: addMessageToRoom_room.direct
        });
        addMessageToRoom_room.addMessage(msg);
    }
}
function setUserActiveState(socket, setUserActiveState_username, state) {
    const setUserActiveState_user = getUser(setUserActiveState_username);
    if (setUserActiveState_user) {
      setUserActiveState_user.setActiveState(state);
    }
    "DEBUGGING SERVER broadcast emit user_state_change";
    socket.broadcast.emit('user_state_change', {
        username: setUserActiveState_username,
        active: state
    });
}
const socketmap = {};
io.on('connection', function(socket) {
    "DEBUGGING SERVER on connection";
    let loggedIn = false;
    let username = false;
    socket.on('new message', function(new_message_username, message, new_message_room) {
        "DEBUGGING SERVER on new message 1";
        if (loggedIn) {
            const new_message_msg = { message: message, room: new_message_room, direct: false };
            "DEBUGGING SERVER on new message 1";
            console.log(new_message_msg);
            addMessageToRoom(new_message_room, new_message_msg, new_message_username);
            "DEBUGGING SERVER on new message 2";
        }
    });
    socket.on('request_direct_room', function(to) {
        "DEBUGGING SERVER on request_direct_room";
        if (loggedIn) {
            const a = getUser(to);
            const b = getUser(username);
            if (a) {
                if (b) {
                const request_direct_room_room = getDirectRoom(a, b);
                const roomCID = 'room' + request_direct_room_room.getId();
                socket.join(roomCID);
                if (socketmap[a.name])
                    socketmap[a.name].join(roomCID);
                "DEBUGGING SERVER emit update_room 1";
                socket.emit('update_room', request_direct_room_room, true); // { room: request_direct_room_room, moveto: true });
                }
            }
        }
    });
    socket.on('add_channel', function(add_channel_name, description, private) {
        "DEBUGGING SERVER on add_channel";
        if (loggedIn) {
            const add_channel_user = getUser(username);
            const add_channel_room = newChannel(add_channel_name, description, private, add_channel_user);
            const roomCID = 'room' + add_channel_room.getId();
            socket.join(roomCID);
            "DEBUGGING SERVER emit update_room 2";
            socket.emit('update_room', add_channel_room, true); // { room: add_channel_room, moveto: true });
            if (add_channel_room.private !== 0) {
                const publicChannels = getRooms().filter(function(r) { return !r.direct && !r.private});
                "DEBUGGING SERVER broadcast emit update_public_channels";
                socket.broadcast.emit('update_public_channels', publicChannels); // { publicChannels: publicChannels });
            }
        }
    });
    socket.on('join_channel', function(join_channel_id) {
        "DEBUGGING SERVER on join_channel";
        if (loggedIn) {
            const join_channel_user = getUser(username);
            const join_channel_room = getRoom(join_channel_id);
            if (!join_channel_room.direct && !join_channel_room.private) {
                addUserToRoom(join_channel_user, join_channel_room);
                const roomCID = 'room' + join_channel_room.getId();
                socket.join(roomCID);
                "DEBUGGING SERVER emit update_room 3";
                socket.emit('update_room', join_channel_room, true); // {room: join_channel_room, moveto: true});
            }
        } 
    });
    socket.on('add_user_to_channel', function(channel, add_user_to_channel_user) {
        if (loggedIn) {
            const add_user_to_channel_user2 = getUser(add_user_to_channel_user);
            const add_user_to_channel_room = getRoom(channel);
            if (!add_user_to_channel_room.direct) {
                addUserToRoom(add_user_to_channel_user2, add_user_to_channel_room);
                const roomCID = 'room' + add_user_to_channel_room.getId();
                socketmap[add_user_to_channel_user2.name].join(roomCID);
                "DEBUGGING SERVER emit update_room 4";
                socketmap[add_user_to_channel_user2.name].emit('update_room', add_user_to_channel_room, false); // { room: add_user_to_channel_room, moveto: false });
            }
        }
    });
    socket.on('leave_channel', function(leave_channel_id) {
        if (loggedIn) {
            const leave_channel_user = getUser(username);
            const leave_channel_room = getRoom(leave_channel_id);
            if (!leave_channel_room.direct) {
                if (!leave_channel_room.forceMembership) {
                    removeUserFromRoom(leave_channel_user, leave_channel_room);
                    const roomCID = 'room' + leave_channel_room.getId();
                    socket.leave(roomCID);
                    "DEBUGGING SERVER emit remove_room";
                    socket.emit('remove_room', leave_channel_room.getId()); // { room: leave_channel_room.getId() });
                }
            }
        }
    });
    socket.on('join', function(p_username) {
        "DEBUGGING SERVER on JOIN 1";
        if (loggedIn) {
            return;
        }
        username = p_username;
        loggedIn = true;
        "DEBUGGING SERVER on JOIN 2";
        socketmap[username] = socket;
        "DEBUGGING SERVER on JOIN 3";
        const join_user = getUser(username) || newUser(username);
        const rooms = join_user.getSubscriptions().map(function(s) {
            socket.join('room' + s);
            return getRoom(s);
        });
        "DEBUGGING SERVER on JOIN 4";
        const publicChannels = getRooms().filter(function(r) { return !r.direct && !r.private});
        "DEBUGGING SERVER emit login";
        socket.emit('login', getUsers().map(function(u) {
            return { username: u.name,
                     active: u.active };
            }), rooms, publicChannels);
        setUserActiveState(socket, username, true);
    });
    socket.on('disconnect', function() {
        "DEBUGGING SERVER on disconnect";
        if (loggedIn)
            setUserActiveState(socket, username, false);
    });
    socket.on('reconnect', function() {
        "DEBUGGING SERVER on reconnect";
        if (loggedIn) {
            setUserActiveState(socket, username, true);
        }
    });
});
server.listen(port, function() {
    'FINISHED_SETUP';
    console.log('Server listening at port %d', port);
});