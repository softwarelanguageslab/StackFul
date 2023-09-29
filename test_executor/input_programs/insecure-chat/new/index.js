const express = require('express');
const app = express();
const path = require('path');
const server = require('http').createServer(app);
const io = require('socket.io')(server);
const port = process.env.PORT || 3000;

const users = {};

class User {
    constructor(name) {
        this.name = name;

        this.active = false;
        this.subscriptions = [];
    }

    getSubscriptions() {
        return this.subscriptions;
    }

    addSubscription(room) {
        const id = room.getId();

        if (this.subscriptions.indexOf(id) === -1)
            this.subscriptions.push(id);
    }

    removeSubscription(room) {
        const id = room.getId();

        const idx = this.subscriptions.indexOf(id);
        if (idx >= 0)
            this.subscriptions.splice(idx, 1);
    }

    setActiveState(b) {
        this.active = b;
    }
}

function addUser(name) {
    const user = new User(name);
    users[name] = user;
    return user;
}

function getUser(name) {
    return users[name];
}

function getUsers() {
    return Object.values(users);
}

const rooms = [];
let roomIdCounter = 0;

class Room {
    constructor(id, name, options) {
        this.id   =  id;
        this.name =  name;

        this.description = options.description || "";
      
        this.forceMembership = !!options.forceMembership;
        this.private         = !!options.private;
        this.direct          = !!options.direct;
  
        this.members = [];
        this.history = [];
    }

    getId() {
        return this.id;
    }

    getMembers() {
        return this.members;
    }

    getMemberCount(){
        return this.members.length;
    }

    addMember(user) {
        if (this.members.indexOf(user.name) === -1)
            this.members.push(user.name);
    }

    removeMember(user) {        
        const idx = this.members.indexOf(user.name);
        if (idx >= 0)
            this.members.splice(idx, 1);
    }

    getHistory() {
        return this.history;
    }

    addMessage(msg) {
        this.history.push(msg);
    }
}

function addRoom(name, options) {
        const id = roomIdCounter++;
        const room = new Room(id, name, options);
        rooms[id] = room;
        return room;
    }

function getRooms() {
    return rooms;
}

function getForcedRooms() {
    return rooms.filter(r => r.forceMembership);
}

function getRoom(id) {
    return rooms[id];
}


function setup() {
    addRoom("general", {forceMembership: true, description: "boring stuff"});
    addRoom("random" , {forceMembership: true, description: "random!"});
    addRoom("private" ,{forceMembership: true, description: "some private channel", private: true});
}
setup();

app.use(express.static(path.join(__dirname, 'public')));
function sendToRoom(room, event, data) {
    io.to('room' + room.getId()).emit(event, data);
}
function newUser(name) {
    const user = addUser(name);
    const rooms = getForcedRooms();
    rooms.forEach(room => {
        addUserToRoom(user, room);
    });
    return user;
}
function newRoom(name, user, options) {
    const room = addRoom(name, options);
    addUserToRoom(user, room);
    return room;
}
function newChannel(name, description, private, user) {
    return newRoom(name, user, {
        description: description,
        private: private
    });
}
function newDirectRoom(user_a, user_b) {
    const room = addRoom(`Direct-${ user_a.name }-${ user_b.name }`, {
        direct: true,
        private: true
    });
    addUserToRoom(user_a, room);
    addUserToRoom(user_b, room);
    return room;
}
function getDirectRoom(user_a, user_b) {
    const rooms = getRooms().filter(r => r.direct && (r.members[0] == user_a.name && r.members[1] == user_b.name || r.members[1] == user_a.name && r.members[0] == user_b.name));
    if (rooms.length == 1) {
        return rooms[0];
    } else
        return newDirectRoom(user_a, user_b);
}
function addUserToRoom(user, room) {
    user.addSubscription(room);
    room.addMember(user);
    sendToRoom(room, 'update_user', {
        room: room.getId(),
        username: user,
        action: 'added',
        members: room.getMembers()
    });
}
function removeUserFromRoom(user, room) {
    user.removeSubscription(room);
    room.removeMember(user);
    sendToRoom(room, 'update_user', {
        room: room.getId(),
        username: user,
        action: 'removed',
        members: room.getMembers()
    });
}
function addMessageToRoom(roomId, msg, username) {
    const room = getRoom(roomId);
    msg.time = new Date().getTime();
    if (room) {
        sendToRoom(room, 'new message', {
            username: username,
            message: msg.message,
            room: msg.room,
            time: msg.time,
            direct: room.direct
        });
        room.addMessage(msg);
    }
}
function setUserActiveState(socket, username, state) {
    const user = getUser(username);
    if (user) {
      user.setActiveState(state);
    }
    socket.broadcast.emit('user_state_change', {
        username: username,
        active: state
    });
}
const socketmap = {};
io.on('connection', socket => {
    let loggedIn = false;
    let username = false;
    socket.on('new message', (username, message, room) => {
        if (loggedIn) {
            const msg = { message: message, room: room, direct: false };
            "DEBUGGING SERVER new_message 1";
            console.log(msg);
            addMessageToRoom(room, msg, username);
            "DEBUGGING SERVER new_message 2";
        }
    });
    socket.on('request_direct_room', (to) => {
        if (loggedIn) {
            const a = getUser(to);
            const b = getUser(username);
            if (a && b) {
                const room = getDirectRoom(a, b);
                const roomCID = 'room' + room.getId();
                socket.join(roomCID);
                if (socketmap[a.name])
                    socketmap[a.name].join(roomCID);
                socket.emit('update_room', room, true); // { room: room, moveto: true });
            }
        }
    });
    socket.on('add_channel', (name, description, private) => {
        if (loggedIn) {
            const user = getUser(username);
            const room = newChannel(name, description, private, user);
            const roomCID = 'room' + room.getId();
            socket.join(roomCID);
            socket.emit('update_room', room, true); // { room: room, moveto: true });
            if (room.private !== 0) {
                const publicChannels = getRooms().filter(r => !r.direct && !r.private);
                socket.broadcast.emit('update_public_channels', publicChannels); // { publicChannels: publicChannels });
            }
        }
    });
    socket.on('join_channel', (id) => {
        if (loggedIn) {
            const user = getUser(username);
            const room = getRoom(id);
            if (!room.direct && !room.private) {
                addUserToRoom(user, room);
                const roomCID = 'room' + room.getId();
                socket.join(roomCID);
                socket.emit('update_room', room, true); // {room: room, moveto: true});
            }
        } 
    });
    socket.on('add_user_to_channel', (channel, user) => {
        if (loggedIn) {
            const user = getUser(user);
            const room = getRoom(channel);
            if (!room.direct) {
                addUserToRoom(user, room);
                const roomCID = 'room' + room.getId();
                socketmap[user.name].join(roomCID);
                socketmap[user.name].emit('update_room', room, false); // { room: room, moveto: false });
            }
        }
    });
    socket.on('leave_channel', (id) => {
        if (loggedIn) {
            const user = getUser(username);
            const room = getRoom(id);
            if (!room.direct && !room.forceMembership) {
                removeUserFromRoom(user, room);
                const roomCID = 'room' + room.getId();
                socket.leave(roomCID);
                socket.emit('remove_room', room.getId()); // { room: room.getId() });
            }
        }
    });
    socket.on('join', (p_username) => {
        "DEBUGGING SERVER JOIN";
        if (loggedIn) {
            return;
        }
        username = p_username;
        loggedIn = true;
        socketmap[username] = socket;
        const user = getUser(username) || newUser(username);
        const rooms = user.getSubscriptions().map(s => {
            socket.join('room' + s);
            return getRoom(s);
        });
        const publicChannels = getRooms().filter(r => !r.direct && !r.private);
        socket.emit('login', getUsers().map(u => ({
                username: u.name,
                active: u.active
            })), rooms, publicChannels);
        setUserActiveState(socket, username, true);
    });
    socket.on('disconnect', () => {
        if (loggedIn)
            setUserActiveState(socket, username, false);
    });
    socket.on('reconnect', () => {
        if (loggedIn) {
            setUserActiveState(socket, username, true);
        }
    });
});
server.listen(port, () => {
    'FINISHED_SETUP';
    console.log('Server listening at port %d', port);
});