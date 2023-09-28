/**
 * Stores input type for input Id's
 */
export default class MessageInputMap {
  private _map: Map<any, any>;

  constructor() {
    this._map = new Map();
  }

  addMessageInput(inputId) {
    this._map.set(inputId, "message");
  }

  addRegularInput(inputId) {
    this._map.set(inputId, "regular");
  }

  getInputType(inputId) {
    const type = this._map.get(inputId);
    if (type === undefined) {
      return "unknown";
    } else {
      return type;
    }
  }
}
