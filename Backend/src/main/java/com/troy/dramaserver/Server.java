package com.troy.dramaserver;

import java.io.*;
import java.security.SecureRandom;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.database.*;
import com.troy.dramaserver.net.Net;

public class Server {

	public static final File PUBLIC_DIR = new File("./public");

	private static final Logger logger = LogManager.getLogger(Server.class);
	private static final File DATABASE_FILE = new File("./database.dat");

	private Database database;
	private Net net;
	private boolean running = true;

	public Server() {
		new Thread(() -> {
			while (running) {
				try {
					save();
					database.cleanSessions();
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("Save thread encountered an exception!");
					logger.catching(e);
				}
			}
		}).start();
		ObjectInputStream stream = null;
		try {
			stream = new ObjectInputStream(new FileInputStream(DATABASE_FILE));
			Object obj = stream.readObject();
			if (obj instanceof Database) {
				database = (Database) obj;
				database.initSecondList();
				logger.info("Successfully read saved database");
			} else {
				throw new RuntimeException("Not database " + obj.getClass());
			}
		} catch (FileNotFoundException e) {
			database = new Database();
			database.initSecondList();
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
		this.net = new Net(this);
	}

	public boolean registerUser(String email, char[] password, String name) {
		return database.registerUser(email, password, name);
	}

	public boolean areCredentialsValid(String email, char[] password) {
		return database.areCredentialsValid(email, password);
	}

	public void save() {
		if (database != null) {
			try {
				ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(DATABASE_FILE));
				stream.writeObject(database);
				stream.close();
			} catch (Exception e) {
				logger.warn("Unable to save database file!");
				logger.catching(e);
			}
		}
	}

	public void shutdown() {
		synchronized (this) {
			if (running) {
				running = false;
				logger.info("Preparing to shutdown the server");
				save();
				if (net != null) {
					net.cleanUp();
					net.join();
				}
				logger.info("server shutdown");
			}
		}

	}

	public boolean containsUser(String email) {
		return getAccount(email) != null;
	}

	public Account getAccount(String email) {
		return database.getUserByEmail(email);
	}

	public Account getAccount(long id) {
		return database.getUserByID(id);
	}

	public Database getDatabase() {
		return database;
	}

	public SecureRandom getRandom() {
		return database.getRandom();
	}

	public void addSession(byte[] value, long userID) {
		database.addSession(value, userID);
	}

	public boolean hasSession(byte[] session) {
		return database.hasSession(session);
	}

	public Account getSession(byte[] session) {
		SessionData data = database.getSession(session);
		return (data == null) ? null : database.getUserByID(data.getId());
	}

}
