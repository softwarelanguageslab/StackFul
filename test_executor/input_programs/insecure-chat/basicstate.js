function setup(Users, Rooms) {
    addRoom("general", {forceMembership: true, description: "boring stuff"});
    addRoom("random" , {forceMembership: true, description: "random!"});
    addRoom("private" ,{forceMembership: true, description: "some private channel", private: true});
}