
var http = require("http"),
    url = require("url"),
    path = require("path"),
    fs = require("fs")
    port = 8192;

server = http.createServer(function(request, response) {
	var contentTypesByExtension = {
		'.html': "text/html",
		'.css':  "text/css",
		'.js':   "text/javascript"
	};
	var uri = url.parse(request.url).pathname;
	var filename = path.join("./files/", uri);

	if(fs.existsSync(filename)) {
		console.log("found " + uri + " at " + filename);

		if (fs.statSync(filename).isDirectory()) filename += '/index.html';

		fs.readFile(filename, "binary", function(err, file) {
			if(err) {
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
		response.write("404 Not Found\n");
		console.log("Coundnt file file: " + uri + " at " + filename + " or " + filename2);
		response.end();
		return;
	}
});
server.on("error", function(error) {
	console.error("Another instance of the server is already running. Aborting!");
	console.error("Error is: " + error);
	process.exit();
});
server.listen(port);


var io = require('socket.io').listen(server);
