export interface IFunctionReplacement {
    readonly function: any;
    readonly replacement: any;
}

export function generateReplacement(_function, _replacement): IFunctionReplacement {
    return {
        function: _function,
        replacement: _replacement
    }
}