package com.troy.dramaserver;

import java.io.*;
import java.util.Scanner;

import org.apache.logging.log4j.*;

public class Main {
	
	private static final Logger logger = LogManager.getLogger(Main.class);

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Server server = null;
		try {
			logger.info("Starting Server");
			server = new Server();
			server.getDatabase().listUsers();
			/*
			 * server.registerUser("Troy_Neubauer", "Furminator1".toCharArray(), "troyneubauer@gmail.com"); server.registerUser("Albert_Ding",
			 * "IAMA'DING_DONG".toCharArray(), "ding@dong.com"); server.registerUser("Justin_Kim", "ILikeCompsci".toCharArray(), "jus@kim.com");
			 * server.registerUser("Chas_Huang", "PenitrateYannie!".toCharArray(), "chashuang_1@gmail.com"); server.registerUser("Drew_Gautier",
			 * "Diplo4lyfe!".toCharArray(), "drew@gmail.com");
			 */

		} catch (Throwable r) {
			logger.fatal("Unhandled exception in main");
			logger.catching(Level.FATAL, r);
			System.exit(1);
		}
		Scanner scanner = new Scanner(System.in);
		CommandParser parser = new CommandParser(server);
		while (true) {
			String line = scanner.nextLine();
			if (parser.parse(line))
				break;
		}

		server.shutdown();
		scanner.close();
	}

}
