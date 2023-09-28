import UIEvent from "./UIEvent";
import UITarget from "../targets/UITarget";

export default class KeyEvent extends UIEvent {
    constructor(_UITarget: UITarget, jsEvent, public base, public symbolic) {
        super(_UITarget, jsEvent);
    }
}