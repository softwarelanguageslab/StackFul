import fs from "fs";

/**
 * Synchronous file/folder operations and jsPrint
 * @type {module:fs}
 */

export function appendFile(path, content) {
  fs.appendFileSync(path, content);
}

export function clearFile(path) {
  fs.writeFileSync(path, "");
}
export function readFile(path) {
  return fs.readFileSync(path, 'utf8');
}
export function writeFile(path, content) {
	fs.writeFileSync(path, content);
}
export function makeDir(dirPath, options) {
  if (! fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, options);
  }
}

export function remove(path, options) {
  if (fs.existsSync(path)) {
    fs.rmSync(path, options);
  }
}

export function removeDir(dirPath) {
  if (fs.existsSync(dirPath)) {
    remove(dirPath, {recursive: true, force: true});
  }
}


export function readJSON(path) {
  const fileContents = readFile(path);
  return JSON.parse(fileContents);
}

/**
 * delegates to console.log
 */
export function jsPrint(...args) {
  console.log(...args);
}
