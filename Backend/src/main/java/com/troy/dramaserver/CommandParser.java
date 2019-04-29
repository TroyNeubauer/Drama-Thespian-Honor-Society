package com.troy.dramaserver;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.database.Account;

public class CommandParser {
	
	private static final Logger logger = LogManager.getLogger(CommandParser.class);

	private Server server;

	public CommandParser(Server server) {
		this.server = server;
	}

	public boolean parse(String line) {
		if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("end") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
			return true;
		}
		try {
			long start = System.currentTimeMillis();
			String[] commands = line.split(" ");
			if (commands[0].equalsIgnoreCase("register")) {
				System.out.println("Register result: " + server.registerUser(commands[1], commands[2].toCharArray(), commands[3]) + " took " + (System.currentTimeMillis() - start) + " ms");
			}
			if (commands[0].equalsIgnoreCase("auth")) {
				System.out.println((server.areCredentialsValid(commands[1], commands[2].toCharArray()) ? "Authentication succscful!" : "Invalid username or password") + " took "
						+ (System.currentTimeMillis() - start) + " ms");
			}
			if (commands[0].equalsIgnoreCase("list")) {
				if (commands[1].equalsIgnoreCase("users")) {
					server.getDatabase().listUsers();
				}
			}
			if (commands[0].equalsIgnoreCase("delete") || commands[0].equalsIgnoreCase("remove")) {
				if (commands[1].equalsIgnoreCase("user")) {
					String username = commands[2];
					if (server.containsUser(username)) {
						Account acc = server.getDatabase().getUsers().remove(username);
						if (acc != null)
							System.out.println("Succscfully removed user \"" + username + "\"");
					} else {
						System.out.println("Unknown user \"" + username + "\"");
					}

				}
			}
			if (commands[0].equalsIgnoreCase("print")) {
				if (commands[1].equalsIgnoreCase("user")) {
					String username = commands[2];
					if (server.containsUser(username)) {
						System.out.println(server.getAccount(username));
					} else {
						System.out.println("Unknown user \"" + username + "\"");
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Unknown command \"" + line + "\"");
			logger.catching(e);
			
		}
		return false;
	}

}