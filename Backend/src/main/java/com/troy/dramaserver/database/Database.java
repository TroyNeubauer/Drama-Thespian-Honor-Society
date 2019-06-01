package com.troy.dramaserver.database;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;

import com.troy.dramaserver.Security;

public class Database implements Serializable {

	private static final int ITERATIONS = 10000, HASH_BYTES = 64, SALT_BYTES = 32, PEPPER_BYTES = 16;

	private HashMap<String, Account> users = new HashMap<String, Account>();

	private ArrayList<PointEntry> approvedPoints = new ArrayList<PointEntry>(), waitingPoints = new ArrayList<PointEntry>();
	private long nextUserID = 1;

	private byte[] pepper = new byte[PEPPER_BYTES];
	private transient SecureRandom random = new SecureRandom();

	public Database() {
		random.nextBytes(pepper);
	}

	public void listUsers() {
		System.out.println("User database currently tracking " + users.size() + " users:");
		for (Account account : users.values()) {
			System.out.println("\t" + account);
		}
	}

	public String registerUser(String username, char[] password, String email) {
		System.out.println("registering user {username \"" + username + "\", password length \"" + password.length + "\" email \"" + email + "\"");

		if (users.containsKey(username)) {
			return "{\"success\"=\"Username taken\"}";
		}
		byte[] salt = new byte[SALT_BYTES];
		if (random == null) {
			random = new SecureRandom();
		}
		random.nextBytes(salt);

		byte[] hash = Security.getHashedPassword(password, salt, pepper, ITERATIONS, HASH_BYTES);
		for (int i = 0; i < password.length; i++)//Zero out the password so that it doesnt stay around in memory
			password[i] = (char) 0x00;
		Account account = new Account(username, hash, email, salt, nextUserID++);
		users.put(username, account);

		return "{\"success\"=true}";
	}

	public boolean areCredentialsValid(String usernameOrEmail, char[] password) {
		if (usernameOrEmail == null || password == null || usernameOrEmail.length() == 0 || password.length == 0)
			return false;
		Account account = null;
		if (users.containsKey(usernameOrEmail))
			account = users.get(usernameOrEmail);
		else
			account = hasEmail(usernameOrEmail);
		if (account == null)
			return false;

		byte[] storedHash = account.getPassword();
		byte[] computedHash = Security.getHashedPassword(password, account.getSalt(), pepper, ITERATIONS, HASH_BYTES);
		for (int i = 0; i < password.length; i++)
			password[i] = (char) 0x00;// Don't keep the password in memory
		return Arrays.equals(storedHash, computedHash);
	}

	private Account hasEmail(String email) {
		for (Account account : users.values()) {
			if (account.getEmail().equals(email))
				return account;
		}
		return null;
	}

	public HashMap<String, Account> getUsers() {
		return users;
	}

}
