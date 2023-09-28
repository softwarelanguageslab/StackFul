"use strict";
const http = require("http");
Reflect.defineProperty(global, "process", {
    value: global.process,
    writable: false,
    configurable: false
});
const jsdom = require("jsdom");
const { JSDOM } = jsdom;
const resourceLoader = new jsdom.ResourceLoader({
    proxy: "http://127.0.0.1:8080",
    fetch: function (url, options) {
        console.log("fetching url", url);
    }
});

// const url = "http://54.38.159.127:3000/#/login/?META-splitter=8000&META-meta-alias=meta&META-alias=client_1"
// const port = 3000

const port = process.argv[2]; // 3000
const hostname = process.argv[3]; // "54.38.159.127"
const path = process.argv[4]; // "/#/login/?META-splitter=8000&META-meta-alias=meta&META-alias=client_1"
const url = "http://" + hostname + ":" + port + path;

console.log("port = ", port)
console.log("hostname = ", hostname)
console.log("path = ", path)

// const alias = process.argv[4];
// console.log("alias", alias);
const options = {
    // url: "http://localhost:" + port + url,
    url: url,
    contentType: "text/html",
    includeNodeLocations: true,
    referrer: "localhost:8080",
    resources: resourceLoader,
    runScripts: "dangerously",
    storageQuota: 10000000,
    beforeParse(window) {
        window.process = process;
    }
};
let body = "";
http.request({
    method: "GET",
    path: path,
    hostname: "localhost",
    port: 8080,
    setHost: false,
    headers: {
        "Host": hostname + ":" + port
    }
}, (res) => {
    res.on("data", (chunk) => {
        console.log("data cb")
        body += chunk.toString("utf8");
    });
    res.on("end", () => {
        const dom = new JSDOM(body, options);
        dom.window._process = process;
    });
}).end();
//# sourceMappingURL=jsdom.js.map