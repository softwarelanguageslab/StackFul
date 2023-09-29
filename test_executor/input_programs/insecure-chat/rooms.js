const rooms = [];
let roomIdCounter = 0;
function Room(id, name, options) {
    this.id   =  id;
    this.name =  name;
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
Room.prototype.addMessage = function() {
    this.history.push(msg);
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