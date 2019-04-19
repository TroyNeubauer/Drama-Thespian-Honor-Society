var https = require("https"),
    url = require("url"),
    path = require("path"),
    fs = require("fs");

var privateKey  = fs.readFileSync('/etc/letsencrypt/live/dramaserver.tk/privkey.pem', 'utf8');
var certificate = fs.readFileSync('/etc/letsencrypt/live/dramaserver.tk/cert.pem', 'utf8');

var credentials = {key: privateKey, cert: certificate};

server = https.createServer(credentials, function(request, response) {
	var contentTypesByExtension = {
		'.html': "text/html",
		'.css':  "text/css",
		'.js':   "text/javascript"
	};
	var uri = url.parse(request.url).pathname;
	var filename = path.join("/home/ubuntu/files", uri);

	if(fs.existsSync(filename)) {
		//console.log("found " + uri + " at " + filename);

		if (fs.statSync(filename).isDirectory()) {
			filename = path.join(filename, "index.html");
		}

		fs.readFile(filename, "binary", function(err, file) {
			if(err) {
				//console.log("Failed to read existing file " + filename);
				response.writeHead(500, {"Content-Type": "text/plain"});
				response.write(err + "\n");
				response.end();
				return;
			}
			var headers = {};
			var contentType = contentTypesByExtension[path.extname(filename)];
			if (contentType) headers["Content-Type"] = contentType;
			response.writeHead(200, headers);
			response.write(file, "binary");
			response.end();
		});
	} else {
		response.writeHead(404, {"Content-Type": "text/plain"});
		response.write("404 Not Found.\n");
		console.log("Coundnt file file: " + uri + " at " + filename);
		response.end();
		return;
	}
});
server.on("error", function(error) {
	console.error("Another instance of the server is already running. Aborting!");
	console.error("Error is: " + error);
	process.exit();
});
server.listen(443);
