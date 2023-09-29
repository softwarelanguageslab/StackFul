(function() {
  "CLIENT ENTERED";
  let _publicChannels = undefined;
  "DEBUGGING CLIENT 1";
  "DEBUGGING CLIENT 1.1.1";
  const $window = $(window);
  "DEBUGGING CLIENT 1.1.2";
  const $messages      = $('.messages'); // Messages area
  "DEBUGGING CLIENT 1.1.3";
  const $inputMessage  = $('#input-message'); // Input message input box
  "DEBUGGING CLIENT 1.1.4";
  const $usernameLabel = $('#user-name');
  const $userList      = $('#user-list');
  const $roomList      = $('#room-list');
  "DEBUGGING CLIENT 1.2";
  const usernameDummyObject = {};
  let username = usernameDummyObject.__generate_input_string___();
  "DEBUGGING CLIENT 1.3";
  $usernameLabel.text(username);
  let connected = false;
  let socket = io();
  let modalShowing = false;
  "DEBUGGING CLIENT 2";
  $('#addChannelModal').on('hidden.bs.modal', () => modalShowing = false)
                       .on('show.bs.modal',   () => modalShowing = true);
  let users = {};
  function updateUsers(p_users) {
    p_users.forEach(u => users[u.username] = u);
    updateUserList();
  }
  function updateUser(username, active) {
    if (!users[username])
      users[username] = {username: username};

    users[username].active = active;
    updateUserList();
  }
  function updateUserList() {
    const $uta = $("#usersToAdd");
    $uta.empty();
    $userList.empty();
    for (let [un, user] of Object.entries(users)) {
      if (username !== user.username)
        $userList.append(`
          <li onclick="setDirectRoom(this)" data-direct="${user.username}" class="${user.active ? "online" : "offline"}">${user.username}</li>
        `);
        $uta.append(`
          <button type="button" class="list-group-item list-group-item-action" data-dismiss="modal" onclick="addToChannel('${user.username}')">${user.username}</button>
        `); 
    };
  }
  "DEBUGGING CLIENT 3";
  document.getElementById("Button1").addEventListener("click", function (e) {
    for (let [un, user] of Object.entries(users)) {
      if (username !== user.username) {
        addToChannel(user.username);
      }
    }
  });
  let rooms = [];
  function updateRooms(p_rooms) {
    rooms = p_rooms;
    updateRoomList();
  }
  function updateRoom(room) {
    rooms[room.id] = room;
    updateRoomList();
  }
  function removeRoom(id) {
    delete rooms[id];
    updateRoomList();
  }
  function updateRoomList() {
    $roomList.empty();
    rooms.forEach(r => {
      if (!r.direct)
        $roomList.append(`
          <li onclick="setRoom(${r.id})"  data-room="${r.id}" class="${r.private ? "private" : "public"}">${r.name}</li>
        `);
    });
  }
  document.getElementById("Button3").addEventListener("click", function (e) {
    rooms.forEach(r => {
      if (!r.direct) {
        setRoom(r.id);
      }
    });
  });
  function updateChannels(channels) {
    const c = $("#channelJoins");
    c.empty();
    channels.forEach(r => {
      if (!rooms[r.id]) 
        c.append(`
          <button type="button" class="list-group-item list-group-item-action" data-dismiss="modal" onclick="joinChannel(${r.id})">${r.name}</button>
        `); 
    });
  }
  document.getElementById("Button4").addEventListener("click", function (e) {
    if (_publicChannels !== undefined) {
      _publicChannels.forEach(r => {
        if (!rooms[r.id]) {
          joinChannel(r.id);
        }
      });
    }
  });
  "DEBUGGING CLIENT 4";
  let currentRoom = false;
  function setRoom(id) {
    let oldRoom = currentRoom;
    const room = rooms[id];
    currentRoom = room;
    $messages.empty();
    room.history.forEach(m => addChatMessage(m));
    $userList.find('li').removeClass("active");
    $roomList.find('li').removeClass("active");
    if (room.direct) {
      const idx = room.members.indexOf(username) == 0 ? 1 : 0;
      const user = room.members[idx];
      setDirectRoomHeader(user);
      $userList.find(`li[data-direct="${user}"]`)
        .addClass("active")
        .removeClass("unread")
        .attr('data-room', room.id);
    } else {
      $('#channel-name').text("#" + room.name);
      $('#channel-description').text(`👤 ${room.members.length} | ${room.description}`);
      $roomList.find(`li[data-room=${room.id}]`).addClass("active").removeClass("unread");
    }
    $('.roomAction').css('visibility', (room.direct) ? "hidden" : "visible");
  }
  window.setRoom = setRoom;
  function setDirectRoomHeader(user) {
    $('#channel-name').text(user);
    $('#channel-description').text(`Direct message with ${user}`);
  }
  "DEBUGGING CLIENT 5";
  function setToDirectRoom(user) {
    setDirectRoomHeader(user);
    "DEBUGGING CLIENT emit request_direct_room";
    socket.emit('request_direct_room', user); // {to: user}
  }
  window.setDirectRoom = (el) => {
    const user = el.getAttribute("data-direct");
    const room = el.getAttribute("data-room");

    if (room) {
      setRoom(parseInt(room));
    } else {
      setToDirectRoom(user);
    }
  }
  function sendMessage() {
    let message = Math.random(); // $inputMessage.val();
    if (message !== 0) {
      if (connected) {
        if (currentRoom !== false) {
          $inputMessage.val('');
          const sendMessage_msg = {username: username, message: message, room: currentRoom.id};
          addChatMessage(sendMessage_msg);
          "DEBUGGING CLIENT emit new message";
          socket.emit('new message', username, message, currentRoom.id);
        }
      }
    }
  }
  "DEBUGGING CLIENT 6";
  function addChatMessage(addChatMessage_msg) {
    let time = new Date(addChatMessage_msg.time).toLocaleTimeString('en-US', { hour12: false, 
                                                        hour  : "numeric", 
                                                        minute: "numeric"});
    $messages.append(`
      <div class="message">
        <div class="message-avatar"></div>
        <div class="message-textual">
          <span class="message-user">${addChatMessage_msg.username}</span>
          <span class="message-time">${time}</span>
          <span class="message-content">${addChatMessage_msg.message}</span>
        </div>
      </div>
    `);
    $messages[0].scrollTop = $messages[0].scrollHeight;
  }
  function messageNotify(messageNotify_msg) {
    if (messageNotify_msg.direct)
      $userList.find(`li[data-direct="${messageNotify_msg.username}"]`).addClass('unread');
    else
      $roomList.find(`li[data-room=${messageNotify_msg.room}]`).addClass("unread");
  }
  function addChannel() {
    const name = $("#inp-channel-name").val();
    const description = $("#inp-channel-description").val();
    const private = $('#inp-private').is(':checked');
    "DEBUGGING CLIENT emit add_channel";
    socket.emit('add_channel', name, description, private); // {name: name, description: description, private: private});
  }
  window.addChannel = addChannel;
  "DEBUGGING CLIENT 7";
  function joinChannel(id) {
    "DEBUGGING CLIENT emit join_channel";
    socket.emit('join_channel', id); // {id: id});
  }
  window.joinChannel = joinChannel;
  function addToChannel(user) {
    "DEBUGGING CLIENT emit add_user_to_channel";
    socket.emit('add_user_to_channel', currentRoom.id, user); // {channel: currentRoom.id, user: user});   
  }
  window.addToChannel = addToChannel;
  function leaveChannel() {
    "DEBUGGING CLIENT emit leave_channel";
    socket.emit('leave_channel', currentRoom.id); // {id: currentRoom.id});   
  }
  window.leaveChannel = leaveChannel;
  "DEBUGGING CLIENT 8";
  document.getElementById("Button2").addEventListener("keydown", (event) => {
    "DEBUGGING CLIENT 8.1";
    if(modalShowing) {
      "DEBUGGING CLIENT 8.2";
      return;
    }
    "DEBUGGING CLIENT 8.3";
    if (!event.ctrlKey) {
      if (!event.metaKey) {
        if (!event.altKey) {
          "DEBUGGING CLIENT 8.4";
          $inputMessage.focus();
          "DEBUGGING CLIENT 8.5";
        }
      }
    }
    "DEBUGGING CLIENT 8.6";
    if (event.which === 13) {
      "DEBUGGING CLIENT 8.7";
      sendMessage();
      "DEBUGGING CLIENT 8.8";
    }
    "DEBUGGING CLIENT 8.9";
    if (event.which === 13) {
      "DEBUGGING CLIENT 8.10";
      event.preventDefault();
      "DEBUGGING CLIENT 8.11";
    } else if (event.which === 10) {
      "DEBUGGING CLIENT 8.10";
      event.preventDefault();
      "DEBUGGING CLIENT 8.11";
    }
  });
  "DEBUGGING CLIENT 9";
  socket.on('login', (users, rooms, publicChannels) => {
    "DEBUGGING CLIENT on login";
    connected = true;
    _publicChannels = publicChannels;
    updateUsers(users);
    updateRooms(rooms);
    updateChannels(publicChannels);
    if (rooms.length > 0) {
      setRoom(rooms[0].id);
    }
  });
  "DEBUGGING CLIENT 10";
  socket.on('update_public_channels', (publicChannels) => {
    "DEBUGGING CLIENT on update_public_channels";
    updateChannels(publicChannels);
  });
  socket.on('new message', (new_message_msg) => {
    "DEBUGGING CLIENT on new message";
    const roomId = new_message_msg.room;
    const room = rooms[roomId];
    if (room) {
      room.history.push(new_message_msg);
    }
    if (roomId == currentRoom.id)
      addChatMessage(new_message_msg);
    else
      messageNotify(new_message_msg);
  });
  "DEBUGGING CLIENT 11";
  socket.on('update_user', data => {
    "DEBUGGING CLIENT on update_user";
    const room = rooms[data.room];
    if (room) {
      room.members = data.members;
      if (room === currentRoom)
        setRoom(data.room);
    }
  });
  socket.on('user_state_change', (data) => {
    "DEBUGGING CLIENT on user_state_change";
    updateUser(data.username, data.active);
  });
  socket.on('update_room', (room, moveto) => {
    "DEBUGGING CLIENT on update_room";
    updateRoom(room);
    if (moveto)
      setRoom(room.id);
  });
  socket.on('remove_room', (room) => {
    "DEBUGGING CLIENT on remove_room";
    removeRoom(room);
    if (currentRoom.id == room)
      setRoom(0);
  });
  socket.emit('join', username);
  "DEBUGGING CLIENT end";
})();