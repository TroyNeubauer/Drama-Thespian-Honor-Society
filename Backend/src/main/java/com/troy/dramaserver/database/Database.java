package com.troy.dramaserver.database;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;

public class Database implements Serializable {

	private static final int ITERATIONS = 10000, HASH_BYTES = 64, SALT_BYTES = 32, PEPPER_BYTES = 16;

	private HashMap<String, Account> users = new HashMap<String, Account>();
	private byte[] pepper = new byte[PEPPER_BYTES];

	public Database() {
		SecureRandom random = new SecureRandom();
		random.nextBytes(pepper);
		System.out.println("Created pepper: " + Arrays.toString(pepper));
	}

	private ArrayList<PointEntry> points = new ArrayList<PointEntry>();

	public void listUsers() {
		System.out.println("User database currently tracking " + users.size() + " users:");
		for (Account account : users.values()) {
			System.out.println("\t" + account);
		}
	}

	public String registerUser(String username, char[] password, String email) {
		return null;
	}

	public boolean areCredentialsValid(String username, char[] password) {
		return false;
	}

	public HashMap<String, Account> getUsers() {
		return users;
	}

}
