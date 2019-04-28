package com.troy.dramaserver;

import com.troy.dramaserver.database.Account;

public class CommandParser {

	private Server server;

	public CommandParser(Server server) {
		this.server = server;
	}

	public boolean parse(String line) {
		if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("end") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
			return true;
		}
		try {
			String[] commands = line.split(" ");
			if (commands[0].equalsIgnoreCase("register")) {
				try {
					server.registerUser(commands[1], commands[2].toCharArray(), commands[3]);
				} catch (IllegalStateException e) {
					System.out.println("Unable to register user. User with name \"" + commands[1] + "\" already exists!");
					return false;
				}
				char[] password = new char[commands[2].length()];
				password[0] = commands[2].charAt(0);
				password[password.length - 1] = commands[2].charAt(password.length - 1);
				for (int i = 1; i < password.length - 1; i++)
					password[i] = '*';
				System.out.println("registering user {username \"" + commands[1] + "\", password \"" + new String(password) + "\" email \""
						+ commands[3] + "\"");
			}
			if (commands[0].equalsIgnoreCase("auth")) {
				System.out.println(server.areCredentialsValid(commands[1], commands[2].toCharArray()) ? "Authentication succscful!"
						: "Invalid username or password");
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
		}
		return false;
	}

}