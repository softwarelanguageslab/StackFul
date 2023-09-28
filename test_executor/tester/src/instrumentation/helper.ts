import tainter from "./tainter";

export function doRegularApply($$function, $$value2, $$values, serial) {
    const $f = tainter.clean($$function);
    const $$result = Reflect.apply($f, $$value2, $$values);
    return $$result;
}
