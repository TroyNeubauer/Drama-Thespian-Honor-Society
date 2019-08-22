package com.troy.dramaserver.database;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.*;
import org.joda.time.DateTime;

import com.troy.dramaserver.Security;
import com.troy.dramaserver.net.*;

import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.concurrent.FastThreadLocal;

public class Database implements Serializable {

	private static final long serialVersionUID = 0;

	private static final Logger logger = LogManager.getLogger(Database.class);

	private static final int ITERATIONS = 10000, HASH_BYTES = 64, SALT_BYTES = 32, PEPPER_BYTES = 16;

	private List<Account> deletedAccounts = new ArrayList<Account>();

	// maps emails to users
	private HashMap<String, Account> users = new HashMap<String, Account>();
	private transient LongObjectHashMap<String> idsToEmails;

	// Sessions to session data
	private HashMap<ByteArrayWrapper, SessionData> sessions = new HashMap<ByteArrayWrapper, SessionData>();

	private ArrayList<PointEntry> approvedPoints = new ArrayList<PointEntry>(), pendingPoints = new ArrayList<PointEntry>();
	private long nextUserID = 1, nextPointID = 1;

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

	// Attempts to look up a user treating line as the id or email
	public Account lookupUser(String line) {
		Account acc = null;
		try {
			long id = Long.parseLong(line);
			acc = getUserByID(id);
			if (acc != null)
				return acc;
		} catch (NumberFormatException e) {

		}
		acc = getUserByEmail(line);
		return acc;
	}

	public Account getUserByID(long id) {
		return getUserByEmail(idsToEmails.get(id));
	}

	public Account getUserByEmail(String email) {
		if (email == null)
			return null;
		return users.get(email);
	}

	public Account removeUser(Account account) {
		if (account != null) {
			deletedAccounts.add(account);
			synchronized (users) {
				users.remove(account.getEmail());
				idsToEmails.remove(account.getUserID());
			}
			cleanSessions();
		}
		return account;
	}

	public void cleanSessions() {
		synchronized (sessions) {
			sessions.entrySet().removeIf(entry -> getUserByID(entry.getValue().getId()) == null);
		}
	}

	public List<Account> getDeletedAccounts() {
		return deletedAccounts;
	}

	public Account removeUserByLookup(String line) {
		return removeUser(lookupUser(line));
	}

	public Account removeUserByID(long id) {
		return removeUser(getUserByID(id));
	}

	public Account removeUserByEmail(String email) {
		return removeUser(getUserByEmail(email));
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
		synchronized (sessions) {
			return new ArrayList<>(sessions.values());
		}
	}

	public List<SessionData> getAllValidSessions() {
		synchronized (sessions) {
			return sessions.values().stream()//format:off
			.filter(data -> data.isValid())
			.collect(Collectors.toList());//format:on
		}
	}

	public List<SessionData> getRecentSessions(int count) {
		synchronized (sessions) {
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
	}

	public synchronized long nextPointID() {
		return nextPointID++;
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

	public ArrayList<PointEntry> getPendingPoints() {
		return pendingPoints;
	}

	public ArrayList<PointEntry> getApprovedPoints() {
		return approvedPoints;
	}

	public boolean transferPoint(long id, ArrayList<PointEntry> start, ArrayList<PointEntry> end) {
		for (int i = 0; i < start.size(); i++) {
			PointEntry entry = start.get(i);
			if (entry.getPointID() == id) {
				start.remove(i);
				end.add(entry);

				return true;
			}
		}
		return false;
	}

	public ArrayList<PointEntry> getAllPoints() {
		ArrayList<PointEntry> result = new ArrayList<PointEntry>(approvedPoints);
		for (PointEntry entry : pendingPoints)
			result.add(entry);
		return result;
	}

	public boolean removePoint(long id) {
		for (int i = 0; i < approvedPoints.size(); i++) {
			if (approvedPoints.get(i).getPointID() == id) {
				approvedPoints.remove(i);
				return true;
			}
		}
		for (int i = 0; i < pendingPoints.size(); i++) {
			if (pendingPoints.get(i).getPointID() == id) {
				pendingPoints.remove(i);
				return true;
			}
		}

		return false;
	}

}
