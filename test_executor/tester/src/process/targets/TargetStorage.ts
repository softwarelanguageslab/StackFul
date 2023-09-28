import Target from "@src/process/targets/Target";
import UITarget from "@src/process/targets/UITarget";
import HTMLUITarget from "@src/process/targets/HTMLUITarget";

export default class TargetStorage {
    private readonly storage: Target[];

    constructor() {
        this.storage = [];
    }

    get size(): number {
        return this.storage.length;
    }

    getTarget(id: number): Target | undefined {
        return this.storage[id];
    }

    pushTarget(target: Target): number {
        const length = this.storage.length
        this.storage[length] = target;
        return length;
    }

    addTarget(id: number, target: Target): void {
        this.storage[id] = target;
    }

    isHtmlElementRegistered(htmlElement: HTMLElement): UITarget | null {
        for (let it of this.storage.values()) {
            if (it instanceof HTMLUITarget) {
                if (it.htmlElement === htmlElement) {
                    return it;
                }
            }
        }
        return null;
    }
}