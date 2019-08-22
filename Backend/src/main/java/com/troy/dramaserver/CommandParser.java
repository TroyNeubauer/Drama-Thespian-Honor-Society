package com.troy.dramaserver;

import java.util.*;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.database.*;

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
			if (commands[0].equalsIgnoreCase("disapprove")) {
				if (server.getDatabase().transferPoint(Long.parseLong(commands[1]), server.getDatabase().getApprovedPoints(), server.getDatabase().getPendingPoints())) {
					System.out.println("Successfully approved point entry");
				} else {
					System.out.println("Failed to approved point entry");
				}
			}
			if (commands[0].equalsIgnoreCase("approve")) {
				if (server.getDatabase().transferPoint(Long.parseLong(commands[1]), server.getDatabase().getPendingPoints(), server.getDatabase().getApprovedPoints())) {
					System.out.println("Successfully approved point entry");
				} else {
					System.out.println("Failed to approved point entry");
				}
			}
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
				} else if (commands[1].equalsIgnoreCase("pending")) {
					System.out.println("Points pending approval:");
					for (PointEntry entry : server.getDatabase().getPendingPoints()) {
						System.out.println("\n" + entry);
					}
				} else if (commands[1].equalsIgnoreCase("approved")) {
					System.out.println("Points with approval:");
					for (PointEntry entry : server.getDatabase().getApprovedPoints()) {
						System.out.println(entry);
					}
				} else if (commands[1].equalsIgnoreCase("user")) {
					String username = commands[2];
					if (server.containsUser(username)) {
						System.out.println(server.getAccount(username));
					} else {
						System.out.println("Unknown user \"" + username + "\"");
					}
				} else if (commands[1].equalsIgnoreCase("session")) {
					String session = commands[2];
					SessionData data = server.getDatabase().getSession(Base64.getDecoder().decode(session));
					if (data == null)
						System.out.println("No session found");
					else
						System.out.println(data);
				} else if (commands[1].equalsIgnoreCase("sessions")) {
					if (commands.length > 2) {
						int count = Integer.parseInt(commands[2]);
						System.out.println("Showing the " + count + " most recent sessions:");
						List<SessionData> sessions = server.getDatabase().getRecentSessions(count);
						for (SessionData session : sessions) {
							System.out.println('\t' + session.toString());
						}
					} else {
						List<SessionData> sessions = server.getDatabase().getAllSessions();
						System.out.println("All sessions ever created: (" + sessions.size() + ")");
						for (SessionData session : sessions) {
							System.out.println('\t' + session.toString());
						}
					}
				} else if (commands[1].equalsIgnoreCase("valid-sessions")) {
					List<SessionData> sessions = server.getDatabase().getAllValidSessions();
					System.out.println("All valid sessions: (" + sessions.size() + ")");
					for (SessionData session : sessions) {
						System.out.println('\t' + session.toString());
					}
				} else if (commands[1].equalsIgnoreCase("deleted")) {
					System.out.println("All deleted accounts");
					for (Account account : server.getDatabase().getDeletedAccounts()) {
						System.out.println('\t' + account.toString());
					}
				}
			}
			if (commands[0].equalsIgnoreCase("delete") || commands[0].equalsIgnoreCase("remove")) {
				if (commands[1].equalsIgnoreCase("user")) {
					Account acc = server.getDatabase().removeUserByLookup(commands[2]);
					if (acc != null)
						System.out.println("Succscfully removed user \"" + acc.getName() + "\"");
					else
						System.out.println("Unknown user \"" + commands[2] + "\"");

				}
				if (commands[1].equalsIgnoreCase("point")) {
					if (server.getDatabase().removePoint(Long.parseLong(commands[2])))
						System.out.println("Succscfully point id " + commands[2]);
					else
						System.out.println("Unknown point id " + commands[2]);

				}
			}
			if (commands[0].equalsIgnoreCase("admin")) {
				Account acc = server.getDatabase().lookupUser(commands[1]);
				acc.setAdmin(Boolean.valueOf(commands[2]));
			}
		} catch (Exception e) {
			System.out.println("Unknown command \"" + line + "\"");
			logger.catching(e);

		}
		return false;
	}

}