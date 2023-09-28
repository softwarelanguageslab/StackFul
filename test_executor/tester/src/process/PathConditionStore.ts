export default class PathConditionStore {

    constructor(private readonly _conditions: any[] = []) {
    }

    private _equivalentTuples(tuple1, tuple2) {
        return tuple1.state === tuple2.state;
    }

    addCondition(condition, eventSequence, state, position, messageInputMap) {
        const newTuple = {condition, eventSequence, state, position, messageInputMap, isExplored: false};
        if (this.getConditions().some((existingTuple) => this._equivalentTuples(existingTuple, newTuple))) {
            return;
        }
        this.getConditions().push(newTuple);
    }

    markExplored(state) {
        console.log(`PathConditionStore.markExplored, trying to mark ${state} as explored`);
        console.log(`PathConditionStore.markExplored, unmarked conditions before marking size = ${this.getUnmarkedConditions().length}`);
        this.getConditions().forEach((tuple) => {
            if (tuple.state === state) {
                console.log(`PathConditionStore.markExplored, found a match`);
                tuple.isExplored = true;
                return;
            }
        });
        console.log(`PathConditionStore.markExplored, unmarked conditions after marking size = ${this.getUnmarkedConditions().length}`);
    }

    getConditions() {
        return this._conditions;
    }

    getMarkedConditions() {
        return this.getConditions().filter((tuple) => tuple.isExplored);
    }

    getUnmarkedConditions() {
        return this.getConditions().filter((tuple) => !tuple.isExplored);
    }
}
