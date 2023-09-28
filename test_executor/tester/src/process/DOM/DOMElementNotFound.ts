export default class DOMElementNotFound extends Error {
  constructor(elementId: string) {
    const message = `Could not find element with id ${elementId};`
    super(message);
  }
}