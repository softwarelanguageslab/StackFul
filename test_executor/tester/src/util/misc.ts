export function UITargetToString(UITarget: any) {
    return "" + UITarget.processId + "_" + UITarget.targetId;
}

export function stateInCollection(state: any, collection: any) {
    for (let i in collection) {
        if (state.equalsState(collection[i])) {
            return true;
        }
    }
    return false;
}
