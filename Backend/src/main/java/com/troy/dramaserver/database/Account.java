package com.troy.dramaserver.database;

import java.io.Serializable;

import org.joda.time.LocalDate;

public class Account implements Serializable {

	private long userID;
	private String name, email;
	private byte[] password, salt;
	private String picturePath;
	private LocalDate indunctionDate;
	private boolean admin;
	private int gradYear, studentID, phoneNumber, cellPhoneNumber;
	private String address;

	public Account() {
	}

	public Account(String email, byte[] password, String name, byte[] salt, long userID) {
		this.name = name;
		this.password = password;
		this.email = email;
		this.salt = salt;
		this.userID = userID;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "Account [userID=" + userID + ", name=" + name + ", email=" + email + "]\n[ picturePath=" + picturePath
				+ ", indunctionDate=" + indunctionDate + ", admin=" + admin + ", gradYear=" + gradYear + ", studentID="
				+ studentID + ", phoneNumber=" + phoneNumber + ", cellPhoneNumber=" + cellPhoneNumber + ", address="
				+ address + "]";
	}

	public String getPicturePath() {
		return picturePath;
	}

	public LocalDate getIndunctionDate() {
		return indunctionDate;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public int getGradYear() {
		return gradYear;
	}

	public int getStudentID() {
		return studentID;
	}

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public int getCellPhoneNumber() {
		return cellPhoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public long getUserID() {
		return userID;
	}

}
