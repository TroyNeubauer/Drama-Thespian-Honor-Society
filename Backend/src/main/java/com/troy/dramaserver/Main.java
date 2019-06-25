package com.troy.dramaserver;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

	private static final Logger logger = LogManager.getLogger(Main.class);

	public static Server server = null;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (server != null) {
					logger.info("Initiating shutdown from shutdown hook");
					server.shutdown();
				}
			}
		});

		try {
			logger.info("Starting Server");
			server = new Server();
			server.getDatabase().listUsers();

		} catch (Throwable r) {
			logger.fatal("Unhandled exception in main");
			logger.catching(Level.FATAL, r);
			System.exit(1);
		}
		Scanner scanner = new Scanner(System.in);
		CommandParser parser = new CommandParser(server);
		while (true) {
			try {
				String line = scanner.nextLine();
				if (parser.parse(line))
					break;
			} catch (NoSuchElementException e) {// There isn't stdin
				try {
					Thread.sleep(100);// Wait
				} catch (Exception e1) {
				}
			}
		}
		scanner.close();
		server.shutdown();
		server = null;// prevent double shutdown
	}
}
