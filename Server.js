const express = require('express');
const fs = require('fs');
const https = require('https');
const crypto = require('crypto');
const serialize = require('serialize-javascript');

function deserialize(serializedJavascript){
  return eval('(' + serializedJavascript + ')');
}

const app = express();
const databaseName = "database.json";
const pbkdf2 = require('pbkdf2')
const hashIterations = 1000, keyLength = 64, saltLength = 32;

if(!fs.existsSync(databaseName)) {
	fs.writeFileSync(databaseName, "{}\n");
	console.log("Created new database file");
}

var users = deserialize(fs.readFileSync(databaseName));

console.log("Read databse file: " + serialize(users, 2));

function hash(salt, password) {
	return pbkdf2.pbkdf2Sync(password, salt, hashIterations, keyLength, 'sha512')
}

function registerUser(name, username, email, password) {
	const salt = crypto.randomBytes(saltLength);
	const hashedPassword = hash(salt, password);
	users[username] = {
		name: name,
		username: username,
		email: email,
		password: hashedPassword,
		salt: salt,
		picture: "",
		inductionDate: new Date(0),
		gradYear: 0,
		studentId: 0,
		phoneNumber: "",
		address: ""
	};
	console.log("Registered user: " + username + " email: " + email + " password: " + hashedPassword.toString('hex'));
}

app.get('/__auth', function (req, res) {
	//req.query
	if((req.query["username"] == undefined && req.query["email"] == undefined) || req.query["password"] == undefined) {
		res.send("Invalid request");
	} else {
		user = undefined;
		if(req.query["username"] != undefined) {//lookup username
			user = users[req.query["username"]];
		} else {//lookup email
			for (let [key, value] of Object.entries(users)) {
				if (value["email"] == req.query["email"]) {
					user = value;
					break;
				}
			}
		}
		if(user == undefined) {
			res.send("User not found ");
		} else {
			passwordHash = user["password"];
			computed = hash(user["salt"], user["password"]);
			if(passwordHash == computed) {
				res.send("Good auth for user: " + user["name"]);
			} else {
				res.send("Bad password for user: " + user["name"] +" \ncomputed: " computed + "\nactual: " + passwordHash);
			}
		}
	}
});

app.get('/__signup', function (req, res) {
	//req.query
	if(req.query["name"] == undefined || req.query["username"] == undefined || req.query["email"] == undefined || req.query["password"] == undefined) {
		res.send("Invalid request");
	} else {
		registerUser(req.query["name"], req.query["username"], req.query["email"], req.query["password"])
		res.send(serialize(users, 2));
	}
});

app.use(express.static('public'));

https.createServer({
	key: fs.readFileSync("/etc/letsencrypt/live/dramaserver.tk/privkey.pem"),
	cert: fs.readFileSync("/etc/letsencrypt/live/dramaserver.tk/cert.pem")
}, app).listen(443, function () {
	console.log("Ready to serve clients...");
})

process.on('SIGINT', function () {
	console.log("Stopping server");
	fs.writeFile(databaseName, serialize(users), function(err) {
		if(err) {
			console.log("Could not save database file: " + err);
		}
		console.log("Saved database file");
		process.exit(0);
	});
});
