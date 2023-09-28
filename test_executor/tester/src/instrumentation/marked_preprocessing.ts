import {Logger as Log} from "../util/logging";

//NodeJS require statements
const Acorn = require("acorn");

const fs = require("fs");
const path = require("path");
const Chalk = require("chalk");
const IO = require("../util/io_operations");

//CONFIG\\
var doPrints = false;
// The regular expressions used to identify the annotations concerning functions and variables
const outerRegExprs = {
  /*
  match everything between '[m]' and '[\m]' followed by 'function' on the next line,
   to be lenient towards programmer mistakes and typos extra measures are taken such that:
   =>no newline(\n),newpage(\f) or cariage return(\r) character is allowed,
   ----this prevents matches across multiple lines where a terminating '[\m]' is forgotten
   =>any occurence of '[' or ']' or '\' is not allowed
   ----this prevents situations where '[m]arg: int; [/m] arg: str; ... [\m]' is accepted as valid syntax and matched as a whole.
   ----(the above situation could occur after a few wrong Ctrl-V's) 
   ----this is also prevented by the fact that the inner match-part is greedy and would match only the first occurence of the full sequence
   ----however this solution has an added benefit of catching compounded mistakes such as: '[m]arg: int; /m] arg: str; ... [\m]' )
  */
  MARKED: /\[m\](?<meta>[^[\]\n\r\f]*?)\[\/m\][^\n]*\n[ \t]*(?<end_group>function[ \t]+(?<fct_name>[^\n\(\t]+)[ \t]*\()/g,
  /*
  similar to MARKED, just different 'tag' [f] and [/f]
  */
  FUNCTION: /\[f\](?<meta>[^[\]\n\r\f]*?)\[\/f\][^\n]*\n[ \t]*(?<end_group>function[ \t]+(?<fct_name>[^\n\(\t]+)[ \t]*\()/g,
  /*
  Analog to MARKED, match everything between '[v]' and '[/v]' followed by a variable declarator on the next line,
  the same precautions are taken to be lenient towards user errors.
  */
  VARIABLE: /\[v\](?<meta>[^[\]\n\r\f]*?)\[\/v\][^\n]*\n[ \t]*(?<end_group>(?<decl>let|const|var)[ \t]+(?<var_name>[^\n\= \t]+)[ \t]*=)/g
}

const innerRegExprs = {
  /*
  match on exactly 'args:' and atleast one argument type(int|string|bool),
    is not strict on spaces: eg 'args:int,string;' and 'args:  int ,  string ;' are both accepted
    will create a named grouping of the types themselves[spaces and ',' included].
    The named group can then be further processed as needed.
  */
  ARGS: /(?:args:[ \t]*(?<types>(?:(?:int|str|bool)[ \t]*,[ \t]*)*(?:int|str|bool))[ \t]*;)/g,
  /*
  match on exactly 'ret:' and exactly one argument type(int|string|bool),
    is not strict on spaces(analog to ARGS).
    contains one named group, this is the type of the return-value in string form;
    this requires no further processing.
  */
  RET: /(?:ret:[ \t]*(?<type>(?:int|str|bool))[ \t]*;)/,
  /*
  matches on exactly 'keep' followed by any number of spaces and ';',
    does not contain any named group.
  */
  KEEP: /keep[ \t]*;/,
   /*
  matches on exactly 'ignore' followed by any number of spaces and ';',
    does not contain any named group.
  */
  IGNORE: /ignore[ \t]*;/,
  /*
  match on exactly 'type:' and exactly one variable type(int|string|bool),
    has one named group, this is the type of the annotated variable in string form;
    this requires no further processing.
  */
  TYPE: /type:[ \t]*(?<type>(?:int|str|bool))[ \t]*;/
}

//string.matchAll(regex) does not exist for node 10.x.x, therefore we implement our own version; for this to function however it is required that the used regex use the 'g' global flag
//see 'https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/matchAll' for further info
function matchAll(regex, string){
  let matches: any = [];
  let match;
  while((match = regex.exec(string)) !== null){
    let match_obj = {index:match.index, groups:match.groups, match: match[0]};
    matches.push(match_obj);
  }
  return matches;
}

function handleVariableMeta(match){
  let meta_string = match.groups.meta;
  let keep_match = meta_string.match(innerRegExprs.KEEP);
  let type_match = meta_string.match(innerRegExprs.TYPE);

  const data: any = {name: match.groups.var_name, meta_type: "variable"};
  if(keep_match !== null){
    data.keep = true;
  } else if(type_match !== null){
    data.type = type_match[1];
  }
  
  return data;
}

function handleKeepIgnoreReturn(meta_string, data){
  let keep_match = meta_string.match(innerRegExprs.KEEP);
  let ignore_match = meta_string.match(innerRegExprs.IGNORE);
  let ret_match = meta_string.match(innerRegExprs.RET);
  if(keep_match !== null){
    data.keep = true;
  }
  if(ignore_match !== null){
    data.ignore = true;
  }
  if(ret_match !== null){
    data.return_type = ret_match[1]; // Save the value captured in the capture group
  }

  return data;
}

// Handles the FUNCTION annotations
function handleFunctionMeta(match){
  const meta_string = match.groups.meta;
  const data = {name: match.groups.fct_name, meta_type: "function"};
  return handleKeepIgnoreReturn(meta_string, data);
}

// Handles the MARKED annotations
function handleMarkedMeta(match){
  let meta_string = match.groups.meta;
  let args_match = meta_string.match(innerRegExprs.ARGS);

  const data: any = {name: match.groups.fct_name, meta_type: "marked"};
  if(args_match !== null){
    let types: any = [];
    let matches: any = matchAll(/int|str|bool/g, args_match)
    for(const type_match of matches){
      types.push(type_match.match); // Push the found type-string to the types array
    }
    data.arg_types = types;
  }

  return handleKeepIgnoreReturn(meta_string, data);
}

export function preProcess(script){
  //Translate index of match to index of relevant part
  function translateIndexToLoc(match){
    const new_index = match.index + match.match.length - match.groups.end_group.length;
    return new_index;
  }
  
  const meta_data = {};
  
  const marked_matches = matchAll(outerRegExprs.MARKED, script);
  for(const match of marked_matches){
    let mrk_idx = translateIndexToLoc(match);
    meta_data[mrk_idx] = handleMarkedMeta(match);
  }

  const function_matches = matchAll(outerRegExprs.FUNCTION, script);
  for(const match of function_matches){
    let fct_idx = translateIndexToLoc(match);
    meta_data[fct_idx] = handleFunctionMeta(match);
  }

  const variable_matches = matchAll(outerRegExprs.VARIABLE, script);
  for(const match of variable_matches){
    let var_idx = translateIndexToLoc(match);
    meta_data[var_idx] = handleVariableMeta(match);
  }
  if(doPrints){
    Log.ALL(meta_data);
  }
  return meta_data;
}

function test_app(path){
  function readJSFile(script){
    const buffer = fs.readFileSync(script);
    const fileContent = buffer.toString();
    return fileContent;
  }
  let script = readJSFile(path);
  Log.ALL(preProcess(script));
}