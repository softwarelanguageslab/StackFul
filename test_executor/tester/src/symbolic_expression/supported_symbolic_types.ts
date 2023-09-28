export enum SupportedSymbolicExpressionTypes {
  Boolean       = "boolean",
  Float         = "float",
  Int           = "int",
  String        = "string",
  Object        = "object",
  Unsupported   = "unsupported"
}

export type SupportedSymbolicExpressionType = boolean | number | string | object | undefined;

export function determineType(value) {
  const type = typeof value;
  if (type === "boolean") {
    return SupportedSymbolicExpressionTypes.Boolean;
  } else if (type === "number" || value instanceof Number) {
    if (Number.isInteger(value)) {
      return SupportedSymbolicExpressionTypes.Int;
    } else {
      return SupportedSymbolicExpressionTypes.Float;
    }
  } else if (type === "string" || value instanceof String) {
    return SupportedSymbolicExpressionTypes.String;
  } else if (type === "object" || value instanceof Object) {
    return SupportedSymbolicExpressionTypes.Object;
  } else {
    return SupportedSymbolicExpressionTypes.Unsupported;
  }
}

export function isBoolean(value) {
  return determineType(value) === SupportedSymbolicExpressionTypes.Boolean;
}
export function isFloat(value) {
  return determineType(value) === SupportedSymbolicExpressionTypes.Float;
}
export function isInt(value) {
  return determineType(value) === SupportedSymbolicExpressionTypes.Int;
}
export function isString(value) {
  return determineType(value) === SupportedSymbolicExpressionTypes.String;
}

export function isObject(value) {
  return determineType(value) === SupportedSymbolicExpressionTypes.Object;
}