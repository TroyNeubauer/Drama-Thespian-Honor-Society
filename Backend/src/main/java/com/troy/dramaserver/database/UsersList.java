package com.troy.dramaserver.database;

import java.io.Serializable;
import java.util.HashMap;

public class UsersList implements Serializable {
	
	private HashMap<String, Account> users;

	public UsersList() {
		users = new HashMap<String, Account>();
	}
	
	public HashMap<String, Account> getUsers() {
		return users;
	}
	
}
