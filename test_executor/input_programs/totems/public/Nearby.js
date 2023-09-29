class NearbyFinder {
  calculateDistances() {
    const ownId = socket.io.engine.id;
    let myPostion = state.userPositions[ownId];
    if (!myPostion) return;

    let distances = {};
    for (let positionKey in state.userPositions) {
      if (positionKey === ownId) continue;
      let comparePosition = state.userPositions[positionKey];
      let a = myPostion[0] - comparePosition[0];
      let b = myPostion[1] - comparePosition[1];
      let c = Math.sqrt(a * a + b * b);
      distances[positionKey] = c;
    }
    return distances;
  }

  findNearbyUsers(distances) {
    let nearbyUsers = [];
    for (let userId in distances) {
      if (distances[userId] < 190) {
        nearbyUsers.push(userId);
      }
    }
    return nearbyUsers;
  }

  showNearbyUsers(nearbyUserIds) {
    const ownId = socket.io.engine.id;

    let position = [700, 20];
    let dismensions = [200, 50];
    let i = 0;
    for (let userId of nearbyUserIds) {
      i += 1;
      fill(255);
      rect(position[0], position[1], dismensions[0], dismensions[1]);
      fill(0);
      text("another user", position[0] + 20, position[1] + i * 25);
    }
  }
}

export default NearbyFinder