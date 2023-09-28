import Linvail from "linvail";
import Nothing from "../symbolic_expression/nothing";

export type wild = undefined | null | boolean | number | string
export type tame = Function
export type dirty = {
    base: wild | tame;
    meta: string;
    symbolic: any;
    varName: string
}
export type value = wild | tame | dirty

class Tainter {
    private _counter: number;
    private _membrane: any;
    readonly capture: (val: any) => any
    readonly release: (val: any) => any
    readonly sandbox;

    constructor() {
        this._counter = 0;
        this._membrane = {
            taint: this.taint,
            clean: this.clean
        }
        const {capture, release, sandbox} = Linvail(this._membrane);
        this.capture = capture;
        this.release = release;
        this.sandbox = sandbox;
    }

    //Lambda functions preserve *this* context in typescript
    taint = (tame: wild | tame, symExp?: any): dirty => 
        ({
            base: tame,
            meta: "#" + (++this._counter),
            symbolic: symExp || new Nothing(),
            varName: ""
        })

    clean = (dirty: dirty): any => dirty.base;

    taintAndCapture = (object: wild | tame, symExp?: any): dirty => this.taint(this.capture(object), symExp);

    cleanAndRelease = ($$object: dirty): any => {
        return this.release(this.clean($$object))
    };
}

export default new Tainter(); 