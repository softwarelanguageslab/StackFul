export const IntPlus = "+";
export const IntMinus = "-";
export const IntTimes = "*";
export const IntDiv = "/";
export const IntGreaterThan = ">";
export const IntGreaterThanEqual = ">=";
export const IntLessThan = "<";
export const IntLessThanEqual = "<=";
export const IntEqual = "==";
export const IntNonEqual = "!=";
export const IntegerInverse = "-";
export const BooleanNot = "!";
export const BooleanAnd = "&&";
export const BooleanOr = "||";

// string operations that produce a boolean           // usage:
export const StringEqual = "string_equal";            // V string1 == string2 OR string1 === string2
export const StringPrefix = "string_prefix";          // V string1.startsWith(string2)
export const StringSuffix = "string_suffix";          // V string1.endsWith(string2)
export const StringSubstring = "string_substring";    // V string1.substring(n1, n2)
export const StringIncludes = "string_includes";      // V string1.includes(string2)

// string operations that produce strings
export const StringAppend = "string_append";          // V string1 + string2 OR string1.concat(string2)
export const StringReplace = "string_replace";        // V string1.replace(string2, string3)  -> replaces occurences of string2 in string1 by string3
export const StringAt = "string_at";                  // V string1.charAt(n1)

// string operations that produce integers
export const StringLength = "string_length";          // V string1.length
export const StringIndexOf = "string_index_of";       // V string1.indexOf(string2)

// Regular expressions
export const NewRegExp = "regex_new";
export const RegExpTest = "regex_test";