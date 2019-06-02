package com.troy.dramaserver.database;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.Security;

import io.netty.util.collection.LongObjectHashMap;

public class Database implements Serializable {

	private static final Logger logger = LogManager.getLogger(Database.class);

	private static final int ITERATIONS = 10000, HASH_BYTES = 64, SALT_BYTES = 32, PEPPER_BYTES = 16;

	// maps emails to users
	private HashMap<String, Account> users = new HashMap<String, Account>();
	private transient LongObjectHashMap<String> idsToEmails;

	// Sessions to account ID's
	private HashMap<byte[], Long> sessions = new HashMap<byte[], Long>();

	private ArrayList<PointEntry> approvedPoints = new ArrayList<PointEntry>(), waitingPoints = new ArrayList<PointEntry>();
	private long nextUserID = 1;

	private byte[] pepper = new byte[PEPPER_BYTES];
	private transient SecureRandom random = new SecureRandom();

	public Database() {
		random.nextBytes(pepper);
	}

	public void initSecondList() {
		idsToEmails = new LongObjectHashMap<String>();
		for (Account account : users.values()) {
			idsToEmails.put(account.getUserID(), account.getEmail());
		}
	}

	public void listUsers() {
		System.out.println("User database currently tracking " + users.size() + " users:");
		for (Account account : users.values()) {
			System.out.println("\t" + account);
		}
	}

	public boolean registerUser(String email, char[] password, String name) {
		logger.info("registering user {name \"" + name + "\", email \"" + email + "\", password length: " + password.length);

		if (users.containsKey(email)) {
			logger.info("Failed to register user - email taken");
			return false;
		}

		byte[] salt = new byte[SALT_BYTES];
		if (random == null)
			random = new SecureRandom();
		random.nextBytes(salt);

		byte[] hash = Security.getHashedPassword(password, salt, pepper, ITERATIONS, HASH_BYTES);
		for (int i = 0; i < password.length; i++)// Zero out the password so that it doesnt stay around in memory
			password[i] = (char) 0x00;
		Account account = new Account(email, hash, name, salt, nextUserID++);
		users.put(email, account);
		idsToEmails.put(account.getUserID(), account.getEmail());
		logger.info("Successfully registered user");
		return false;
	}

	public boolean areCredentialsValid(String email, char[] password) {
		long start = System.currentTimeMillis();
		boolean result;
		Account account = null;
		if (email == null || password == null || email.length() == 0 || password.length == 0) {
			result = false;
		} else {
			account = users.get(email);
			if (account == null) {
				result = false;
			} else {

				byte[] storedHash = account.getPassword();
				byte[] computedHash = Security.getHashedPassword(password, account.getSalt(), pepper, ITERATIONS, HASH_BYTES);
				for (int i = 0; i < password.length; i++)
					password[i] = (char) 0x00;// Don't keep the password in memory
				result = Arrays.equals(storedHash, computedHash);
			}
		}
		long ms = System.currentTimeMillis() - start;
		logger.info((result ? "Successful" : "Unsuccessful") + " login by user " + (account == null ? null : users.get(email).getName()) + " determined in " + ms + " ms");
		return result;
	}

	public Account getUser(long id) {
		return getUser(idsToEmails.get(id));
	}

	public Account getUser(String email) {
		if (email == null)
			return null;
		return users.get(email);
	}

	public Account removeUser(String email) {
		if (email == null)
			return null;
		Account account = users.remove(email);
		if (account != null) {
			idsToEmails.remove(account.getUserID());
		}
		return account;
	}

	public Account removeUser(long id) {
		return removeUser(idsToEmails.get(id));
	}
	
	public HashMap<byte[], Long> getSessions() {
		return sessions;
	}
}
