import {Logger as Log} from "../util/logging";

//NodeJS require statements
const Acorn = require("acorn");
const Astring = require("astring");
const Estraverse = require("estraverse");
const Escope = require("escope");
const prep = require("./marked_preprocessing");


const Chalk = require("chalk");

//CONFIG\\
const modes = {
  basic: "basic", // All variables are considered random inputs, functions that are called/are collected get replaced with an 'empty' function that returns a random input
  complete: "complete"
};
const types = {
  int: 'Math.random()',
  str: '({}).__generate_input_string___()',
  bool: '({}).__generate_input_boolean___()'
}
const types_ast = {
  int: Acorn.parse('Math.random()', {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'}).body[0].expression,
  str: Acorn.parse('({}).__generate_input_string___()', {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'}).body[0].expression,
  bool: Acorn.parse('({}).__generate_input_boolean___()', {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'}).body[0].expression
}
const doPrints = true;
//------\\

// Generate a unique name for each random-input
var UID = 0;
var curr_order = null;
function newName(name, order) {
  // If we are starting handling a new marked function, reset the naming scheme
  if(order !== curr_order){
    UID = 0;
  }
  UID = UID + 1;
  return name + "_ID_" + UID;
}

// Generate the call expression and the variable declarations need to call the function(uses meta data if present)
function generateCallExpressionNodes(node, order){
  let args = node.params.map(param => newName(param.name, order));
  let call_ast = Acorn.parse(node.id.name + "(" + args.join(", ") + ");", {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'}).body[0];
  // Default type
  let arg_types = args.map(arg => types_ast.int);
  // Overwrite the arg_types with the data stored in the node's meta
  if(node.hasOwnProperty('meta') && node.meta.hasOwnProperty("arg_types")){
    if(node.meta.arg_types.length == arg_types.length){
      node.meta.arg_types.forEach((val, idx) => {arg_types[idx] = types_ast[val]});
    } else if(node.meta.arg_types.length > arg_types.length){
      Log.ALL(Chalk.magenta("WARNING: Insufficient argument types provided for function: " + node.meta.name));
    } else {
      Log.ALL(Chalk.magenta("WARNING: To many argument types provided for function: " + node.meta.name));
    }
  }
  if(args.length > 0){
    let declaration_ast = Acorn.parse("const " + args[0] + "= " + "temp", {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'}).body[0];
    declaration_ast.declarations[0].init = arg_types[0];
    for(let i = 1; i < args.length; i++){
      // Make a deep copy
      let new_decl = JSON.parse(JSON.stringify(declaration_ast.declarations[0]));
      // Adjust the 'VariableDeclarator'
      new_decl.id.name = args[i];
      new_decl.init = arg_types[i];
      // Push to declarations array inside 'VariableDeclaration'
      declaration_ast.declarations.push(new_decl);
    }
    return [declaration_ast, call_ast];
  } else {
    return [call_ast];
  }
}

// Generates a dummy function from an actual function node, based on its meta data(if present)
function generateDummyFunction(node){
  // Default value
  let return_value = types.int;
  // Check if this function has any meta data, if so overwrite the return-value
  if(node.meta && node.meta.hasOwnProperty('return_type')){
    return_value = types[node.meta.return_type];
  }
  let args = node.params.map(param => param.name).join(", ");
  let prog = "function " + node.id.name + "(" + args + "){ return " + return_value + ";}";
  let ast = Acorn.parse(prog, {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'});
  return ast.body[0];
}

// Checks the variable node for meta data and returns a 'new' variable node
function handleVariableDeclaration(node){
  // Default type for variables
  let type = types_ast.int;
  if(node.hasOwnProperty('meta')){
    if(node.meta.hasOwnProperty('keep')){
      return node;
    } else if(node.meta.hasOwnProperty('type')){
      type = types_ast[node.meta.type];
    }
  }
  // Alter the node's declaration depending on the 'type' of the variable
  node.declarations[0].init = type;
  return node;
}

// (recursive)Collection process to find all relevant nodes for a give function node, based on mode
function collectRelevantNodes(marked_node, scopeManager, iteration, mode){
  function findRelevantNodes(node) {
    var relevant_nodes: any = {variable_nodes: [], function_nodes: [], class_nodes: []}
    const currentScope = scopeManager.acquire(node);
    // If the node being handled is a synthetic function node, there will be no scope to be found => this function does not need to be handled
    if(currentScope === null) {return relevant_nodes}
    if (currentScope.through) {
      currentScope.through.forEach(reference => {
        if(reference.resolved === null){
          // if it cannot be resolved, 'ignore' it and log it; such things as 'Math' will end up here and other external/internal libs or globals
          if(doPrints){Log.ALL(Chalk.red("Reference.resolved returns null for: '" + reference.identifier.name + "' in " + marked_node.id.name))};
        } else {
          const node_def = reference.resolved.defs[0].node;
          if (node_def && (!node_def.hasOwnProperty('handled') || node_def.handled !== iteration)) {
            node_def.handled = iteration;
            if (node_def.type == 'VariableDeclarator') {
              // Store the reference to the VariableDeclaration, which was stored in the parent field of the VariableDeclarator
              // wrapped in an array as to prevent it from being traversed into
              relevant_nodes.variable_nodes.push(node_def.parent[0]);
            } else if (node_def.type == 'FunctionDeclaration') {
              // Handle the function node based on 'mode' 
              switch(mode){
                case modes.basic:
                  // Check if the function node was marked as 'keep', if so store it for further processing
                  if(node_def.hasOwnProperty("meta") && node_def.meta.hasOwnProperty("keep")){
                    relevant_nodes.function_nodes.push(node_def);
                  } else {
                    // Store a dummy function declaration instead, generated correctly
                    let dummy_node = generateDummyFunction(node_def);
                    relevant_nodes.function_nodes.push(dummy_node);
                  }
                  break;
                case modes.complete:
                  if(node_def.hasOwnProperty("meta") && node_def.meta.hasOwnProperty("ignore")){
                    // Store a dummy function declaration instead, generated correctly
                    let dummy_node = generateDummyFunction(node_def);
                    relevant_nodes.function_nodes.push(dummy_node);
                  } else {
                   // Store the function node for further processing
                    relevant_nodes.function_nodes.push(node_def); 
                  }
                  break;
                default:
                  throw "Unknown marked_functions mode!";
              }
            } else  if (node_def.type == 'ClassDeclaration') {
              //Handle how, store node for usage?
              relevant_nodes.class_nodes.push(node_def);
            } else {
              Log.ALL(Chalk.magenta("Unknown node type found in through: " + node_def + " " + node_def.type));
            }
          }
        }
      })
  
      if (relevant_nodes.function_nodes.length > 0) {
        //recursive call and collect
        relevant_nodes.function_nodes.forEach(function_node => {
          var recursively_found_relevant_nodes = findRelevantNodes(function_node);
          relevant_nodes.variable_nodes = relevant_nodes.variable_nodes.concat(recursively_found_relevant_nodes.variable_nodes);
          relevant_nodes.function_nodes = relevant_nodes.function_nodes.concat(recursively_found_relevant_nodes.function_nodes);
          relevant_nodes.class_nodes = relevant_nodes.class_nodes.concat(recursively_found_relevant_nodes.class_nodes);
        })
      }
    }
    return relevant_nodes;
  }
  // Label the marked_node, such that findRelevantNodes wont recursively get stuck in a endless loop
  marked_node.handled = iteration;
  // Start the recursive node finding process
  const relevant_nodes = findRelevantNodes(marked_node);
  // Add the marked node itself
  relevant_nodes.function_nodes.push(marked_node);
  // Return the data-object containing all relevant nodes
  return relevant_nodes;
}

// Creates a parameter-less function to introduce a function scope around the collected nodes as a final wrapper
function wrapInFunctionScope(name, data, order){
  // A sort function for AST nodes, used to maintain original code order
  function sortAst(node1, node2){
    if(node1.start < node2.start) {
      return -1
    } else if (node1.start > node2.start){
      return 1
    } else {
      return 0
    }
  }
  const function_name = "MARKED____" + order.toString() + "_" + name;
  let script = "function " + function_name + "(){}" + function_name + "();"
  let ast = Acorn.parse(script, {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'});
  //ast = Program, ast.body[0] = FunctionDeclaration, ast.body[0].body = BlockStatement
  ast.body[0].body.body = data.class_nodes.concat(data.variable_nodes).concat(data.function_nodes).sort(sortAst).concat(data.call_nodes);
  // return the array containing function_node and the call_node for said function
  return ast.body;
}

// Handle a single marked function from beginning to end
function handleMarkedFunction(node, scopeManager, mode, order){
  // Recursively find and collect relevant variable decl & function decl nodes
  const relevant_nodes = collectRelevantNodes(node, scopeManager, order, mode);
  // Modify the variable nodes depending on the current mode
  relevant_nodes.variable_nodes.map(var_node => handleVariableDeclaration(var_node));
  // Create the argument declarations needed for the call-expression and the call-expression itself
  const call_expression_nodes = generateCallExpressionNodes(node, order);
  // Add the call expression nodes(declarators for arguments + call itself)
  relevant_nodes.call_nodes = call_expression_nodes;
  // return the relevant nodes wrapped in a functionscope
  return wrapInFunctionScope(node.id.name, relevant_nodes, order);
}

// Used to remove the automatically generated Node.js function wrapper
function handleNodeWrapper(script){
  const function_wrapper = "(function (exports, require, module, __filename, __dirname) {";
  // Check whether there is a Node.js function wrapper present:
  if(script.indexOf(function_wrapper) == 0){
    // Cut off the function wrapper[ends with '});']
    return [script.substring(function_wrapper.length, script.length - 3), true];
  }
  return [script, false];
}

// Do a traversal over the ast and add the correct meta-data to each node that was marked in the script
// and collect all marked functions.
function processMetaData(ast, script){
  // Collect the metadata originating from markings/annotations
  const meta_data = prep.preProcess(script);

  // Used to see if metadata has been stored about this particular node
  // possible types of nodes are VariableDeclaration & FunctionDeclaration
  function isMarked(node){
    return (meta_data.hasOwnProperty(node.start));
  }

  // Perform the AST traversal and add the correct metadata to the marked nodes
  var marked_function_nodes: any = [];
  Estraverse.traverse(ast, {
    enter: function (node, parent) {
      if(node.type === 'VariableDeclaration'){
        // Save a link to the parent(VariableDeclaration) node in each of the 'VariableDeclarator' nodes,
        // this link will be used to find information about the 'VariableDeclarator' later in the program while using the scopeManager
        node.declarations.forEach(decl => decl.parent = [node]);
      }
      if(isMarked(node)) {
        // Add the meta data to the ast node
        node.meta = meta_data[node.start];
        if(node.type === 'FunctionDeclaration' && node.meta.meta_type === "marked"){
          // Store the marked function node, this will be returned from this function
          marked_function_nodes.push(node);
          // Tell the traversal to skip traversing the children of this node
          //return Estraverse.VisitorOption.Skip;
        }
	    }
    }
  });

  return marked_function_nodes;
}

// - Main exported function - \\
export default function minimize(script, mode: any = "basic") {
  // Check for the existence of a Node.js wrapper and prune it if found
  const [adjusted_script, hadWrapper] = handleNodeWrapper(script);

  // Create the ast representation of the (adjusted) script
  const ast = Acorn.parse(adjusted_script, {ecmaVersion: 2015}, {locations:true}, {sourceType: 'module'});

   // Analyze the ast for scoping information
  const scopeManager = Escope.analyze(ast, {ecmaVersion: 6, sourceType: "module"});

  // Process the meta data gathered from the annotations/markings and find all marked functions
  const marked_function_nodes = processMetaData(ast, adjusted_script);

  // Return the unaltered script if no function nodes were marked
  if(marked_function_nodes.length === 0){
    console.log(">>No marked functions found");
    return script;
  }

  // Handle each marked node and store them in an array
  let order = 0;
  let handled_nodes = [];
  for(const fct_node of marked_function_nodes){
    // The wrapped relevant nodes + a call node to call the functionscope wrapper
    const handled_node = handleMarkedFunction(fct_node, scopeManager, mode, order);
    handled_nodes = handled_nodes.concat(handled_node);
    order = order + 1;
  }

  // Construct the new program AST-node
  const new_ast = {
    type: "Program",
    body: handled_nodes
  }
  
  // Generate the string version of the program
  let transformed = Astring.generate(new_ast);
  if(hadWrapper){
    transformed = "(function (exports, require, module, __filename, __dirname) {" + "\"FINISHED_SETUP\"\n" + transformed + "});"
  }

  // Return the fully minimized/adjusted program in string format
  //console.log(transformed);
  return transformed;
}