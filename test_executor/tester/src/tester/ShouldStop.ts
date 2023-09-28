import {jsPrint} from "@src/util/io_operations";

export abstract class ShouldStop {
	public abstract shouldStop(predicateExpression, wasTrue: boolean): boolean;
}

export class ShouldAlwaysStop extends ShouldStop {
	public shouldStop(predicateExpression, wasTrue: boolean): boolean {
		return true;
	}
}

export class ShouldNeverStop extends ShouldStop {
	public shouldStop(): boolean {
		return false;
	}
}

export class ShouldStopAfterPath extends ShouldStop {
	constructor(protected _path: string) {
		super();
	}

	public shouldStop(predicateExpression, wasTrue: boolean): boolean {
		jsPrint(`ShouldStop: expression = ${predicateExpression}, this._path = ${this._path}`);
		if (this._path === "") {
			return true;
		}
		if (wasTrue && this._path[0] !== "t") {
			throw new Error(`Should not happen: expected to follow false-branch: ${predicateExpression}`);
		} else if (!wasTrue && this._path[0] !== "e") {
			throw new Error(`Should not happen: expected to follow true-branch: ${predicateExpression}`);
		}
		this._path = this._path.substr(1);
		return false;
	}
}