package com.troy.dramaserver;

import java.io.*;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.database.*;
import com.troy.dramaserver.net.Net;

public class Server {

	private static final File DATABASE_FILE = new File("./database.dat");
	private static final Logger logger = LogManager.getLogger(Server.class);

	private Database database;
	private Net net;

	public Server() {
		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(DATABASE_FILE));
			Object obj = stream.readObject();
			if (obj instanceof Database) {
				database = (Database) obj;
				logger.info("Successfully read saved database");
			} else {
				throw new RuntimeException("Not database " + obj.getClass());
			}
		} catch (FileNotFoundException e) {
			database = new Database();
			logger.info("Creating new database");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e2) {
					logger.warn("Unable to close input database stream");
					logger.catching(e2);
				}
			}
		}
	}

	public String registerUser(String username, char[] password, String email) {
		return database.registerUser(username, password, email);
	}

	public boolean areCredentialsValid(String username, char[] password) {
		return database.areCredentialsValid(username, password);
	}

	public void shutdown() {
		logger.info("Preparing to shutdown the server");
		if (net != null)
			net.cleanUp();
		if (database != null) {
			try {
				ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(DATABASE_FILE));
				stream.writeObject(database);
				stream.close();
				logger.info("Successfully saved database");
			} catch (Exception e) {
				logger.warn("Unable to save database file!");
				logger.catching(e);
			}
		}
		if (net != null)
			net.join();
		logger.info("server shutdown");
	}

	public boolean containsUser(String username) {
		return getAccount(username) != null;
	}

	public Account getAccount(String username) {
		return database.getUsers().get(username);
	}

	public Database getDatabase() {
		return database;
	}

}
