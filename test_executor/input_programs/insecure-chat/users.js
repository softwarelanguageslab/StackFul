const users = {};
function User(name) {
    this.name = name;
    this.active = false;
    this.subscriptions = [];
}
User.prototype.getSubscriptions = function() {
    return this.subscriptions;
}
User.prototype.addSubscription = function(addSubscription_room) {
    const id = addSubscription_room.getId();
    if (this.subscriptions.indexOf(id) === -1) {
        this.subscriptions.push(id);
    }
}
User.prototype.removeSubscription = function(removeSubscription_room) {
    const id = removeSubscription_room.getId();
    const idx = this.subscriptions.indexOf(id);
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