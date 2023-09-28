import UIEvent from "./UIEvent";
import UITarget from "../targets/UITarget";

export default class MouseClickEvent extends UIEvent {
    constructor(_UITarget: UITarget, jsEvent, public baseX, public baseY, public symbolicX, public symbolicY) {
        super(_UITarget, jsEvent)
    }
}