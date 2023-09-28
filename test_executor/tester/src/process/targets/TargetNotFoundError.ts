export default class TargetNotFoundError extends Error {
    constructor(private id: number | string) {
        super(`Target with id ${id} does not exist`);
    }
}