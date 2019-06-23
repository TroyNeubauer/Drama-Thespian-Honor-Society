package com.troy.dramaserver.database;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;
import org.joda.time.DateTime;

import com.troy.dramaserver.Security;
import com.troy.dramaserver.net.NetAPIs;

import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.concurrent.FastThreadLocal;

public class Database implements Serializable {

	private static final Logger logger = LogManager.getLogger(Database.class);

	private static final int ITERATIONS = 10000, HASH_BYTES = 64, SALT_BYTES = 32, PEPPER_BYTES = 16;

	// maps emails to users
	private HashMap<String, Account> users = new HashMap<String, Account>();
	private transient LongObjectHashMap<String> idsToEmails;

	// Sessions to account ID's
	private HashMap<ByteArrayWrapper, SessionData> sessions = new HashMap<ByteArrayWrapper, SessionData>();

	private ArrayList<PointEntry> approvedPoints = new ArrayList<PointEntry>(), waitingPoints = new ArrayList<PointEntry>();
	private long nextUserID = 1;

	private byte[] pepper = new byte[PEPPER_BYTES];

	private static final FastThreadLocal<SecureRandom> random = new FastThreadLocal<SecureRandom>() {
		protected SecureRandom initialValue() throws Exception {
			return new SecureRandom();
		};
	};

	public Database() {
		random.get().nextBytes(pepper);

	}

	public void initSecondList() {
		idsToEmails = new LongObjectHashMap<String>();
		synchronized (users) {
			for (Account account : users.values()) {
				idsToEmails.put(account.getUserID(), account.getEmail());
			}
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

		synchronized (users) {
			if (users.containsKey(email)) {
				logger.info("Failed to register user - email taken");
				return false;
			}

			byte[] salt = new byte[SALT_BYTES];
			random.get().nextBytes(salt);

			byte[] hash = Security.getHashedPassword(password, salt, pepper, ITERATIONS, HASH_BYTES);
			for (int i = 0; i < password.length; i++)// Zero out the password so that it doesnt stay around in memory
				password[i] = (char) 0x00;
			Account account = new Account(email, hash, name, salt, nextUserID++);
			users.put(email, account);
			idsToEmails.put(account.getUserID(), account.getEmail());
		}
		logger.info("Successfully registered user");
		return true;
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
		logger.info((result ? "Successful" : "Unsuccessful") + " login by user " + (account == null ? null : account.getName()) + " determined in " + ms + " ms");
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
		Account account;
		synchronized (users) {
			account = users.remove(email);
			if (account != null) {
				idsToEmails.remove(account.getUserID());
			}
		}
		return account;
	}

	public Account removeUser(long id) {
		return removeUser(idsToEmails.get(id));
	}

	public SecureRandom getRandom() {
		return random.get();
	}

	public void addSession(byte[] session, long userID) {
		synchronized (sessions) {
			sessions.put(new ByteArrayWrapper(session), new SessionData(session, userID, DateTime.now().plus(NetAPIs.MAX_SESSION_COOKIE_AGE)));
		}
	}

	public boolean hasSession(byte[] session) {
		return getSession(session) != null;
	}

	public SessionData getSession(byte[] session) {
		SessionData data = sessions.get(new ByteArrayWrapper(session));// The session is valid if both exists and is less than the max age
		if (data != null && data.isValid())
			return data;
		else
			return null;
	}

	public SessionData getSessionEver(byte[] session) {
		SessionData data = sessions.get(new ByteArrayWrapper(session));// The session is valid if both exists and is less than the max age
		return data;
	}

	public List<SessionData> getAllSessions() {
		return new ArrayList<>(sessions.values());
	}

	public List<SessionData> getAllValidSessions() {
		return sessions.values().stream()//format:off
			.filter(data -> data.isValid())
			.collect(Collectors.toList());//format:on
	}

	public List<SessionData> getRecentSessions(int count) {
		return sessions.values().stream().sorted(new Comparator<SessionData>() {
			@Override
			public int compare(SessionData o1, SessionData o2) {
				long difference = -o1.getValidUntil().compareTo(o2.getValidUntil());// negate to sort closest to farthest in time
				if (difference == 0)
					return compareArray(o1.getData(), o2.getData());
				else
					return difference > 0 ? 1 : -1;
			}
		}).limit(count).collect(Collectors.toList());
	}

	public static <T extends Comparable<T>> int compareArray(byte[] a, byte[] b) {
		if (a == b) { // also covers the case of two null arrays. those are considered 'equal'
			return 0;
		}

		// arbitrary: non-null array is considered 'greater than' null array
		if (a == null) {
			return -1; // "a < b"
		} else if (b == null) {
			return 1; // "a > b"
		}

		// now the item-by-item comparison - the loop runs as long as items in both arrays are equal
		int last = Math.min(a.length, b.length);
		for (int i = 0; i < last; i++) {
			byte ai = a[i];
			byte bi = b[i];
			int comp = Byte.compare(ai, bi);
			if (comp != 0) {
				return comp;
			}
		}

		// shorter array whose items are all equal to the first items of a longer array is considered 'less than'
		if (a.length < b.length) {
			return -1; // "a < b"
		} else if (a.length > b.length) {
			return 1; // "a > b"
		}

		// i.e. (a.length == b.length)
		return 0; // "a = b", same length, all items equal
	}

}
