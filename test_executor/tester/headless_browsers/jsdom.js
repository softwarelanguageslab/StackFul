const http = require("http");

Reflect.defineProperty(global, "process", {
	value:global.process,
	writable:false,
	configurable:false
});

const jsdom = require("jsdom");
const { JSDOM } = jsdom;

const resourceLoader = new jsdom.ResourceLoader({
  proxy: "http://127.0.0.1:8080",
  fetch: function(url, options) {
  	console.log("fetching url", url);
  }
});

const port = process.argv[2];
const url = process.argv[3];
// const alias = process.argv[4];
// console.log("alias", alias);

const options = { 
	url: "http://localhost:" + port + url,
	contentType: "text/html",
	includeNodeLocations: true,
	referrer: "localhost:" + port,
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
	path: url,
	hostname: "localhost",
	port: 8080,
	setHost: false,
	headers: {
		"Host": "localhost:" + port
	}
}, (res) => {
	res.on("data", (chunk) => {
		body += chunk.toString("utf8");
	});
	res.on("end", () => {
		const dom = new JSDOM(body, options);
		dom.window._process = process;
	});
}).end();

