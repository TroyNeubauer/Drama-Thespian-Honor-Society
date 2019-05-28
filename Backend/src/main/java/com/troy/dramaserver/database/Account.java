package com.troy.dramaserver.database;

import java.io.Serializable;
import java.time.LocalDate;

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

	public Account(String name, byte[] password, String email, byte[] salt, long userID) {
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

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "Account [name=" + name + ", email=" + email + "]";
	}

	public String getPicturePath() {
		return picturePath;
	}

	public void setPicturePath(String picturePath) {
		this.picturePath = picturePath;
	}

	public LocalDate getIndunctionDate() {
		return indunctionDate;
	}

	public void setIndunctionDate(LocalDate indunctionDate) {
		this.indunctionDate = indunctionDate;
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

	public void setGradYear(int gradYear) {
		this.gradYear = gradYear;
	}

	public int getStudentID() {
		return studentID;
	}

	public void setStudentID(int studentID) {
		this.studentID = studentID;
	}

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(int phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public int getCellPhoneNumber() {
		return cellPhoneNumber;
	}

	public void setCellPhoneNumber(int cellPhoneNumber) {
		this.cellPhoneNumber = cellPhoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public long getUserID() {
		return userID;
	}

}
