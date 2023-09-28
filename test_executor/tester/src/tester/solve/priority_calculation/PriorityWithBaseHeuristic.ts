import ConfigurationReader from "@src/config/user_argument_parsing/Config";
import { SymJSAnalysisEnum } from "@src/config/user_argument_parsing/SymJS";
import ReadWrite from "../read_write";
import PriorityByName from "./priorityByName";
import PriorityByValue from "./priorityByValue";
import Event from "@src/tester/Event.js";

/*
    Provides method calculatePriorityWithBaseHeuristic to many subclasses
*/

export abstract class PriorityWithBaseHeuristic {
	constructor(protected _readWrite: ReadWrite) {
	}
	calculatePriorityWithBaseHeuristic(eventSeq: Event[], event: any) {
		switch (ConfigurationReader.config.PRIORITY_BASE) {
			case SymJSAnalysisEnum.named:
				let pNamed = new PriorityByName(this._readWrite)
				return pNamed.calculatePriority(eventSeq, event); //delegate to a so-called base heuristic
				break;
			case SymJSAnalysisEnum.valued:
				let pValued = new PriorityByValue(this._readWrite);
				return pValued.calculatePriority(eventSeq, event); //delegate to a so-called base heuristic
				break;
			default:
				throw "Unknown type of base priority calculation heuristic (choose between symJsNamed or symJSValued)";
		}
	}
}