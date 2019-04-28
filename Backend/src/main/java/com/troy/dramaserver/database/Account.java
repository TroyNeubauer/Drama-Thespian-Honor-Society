package com.troy.dramaserver.database;

import java.io.Serializable;

public class Account implements Serializable {

	private String username, email;
	private byte[] password;

	public Account() {
	}

	public Account(String username, String email, byte[] password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

}
