package com.troy.dramaserver.database;

import java.io.Serializable;

import org.joda.time.LocalDate;

public class Account implements Serializable {

	private static final long serialVersionUID = 0;

	private long userID, itsID;
	private String name, email;
	private byte[] password, salt;
	private LocalDate indunctionDate;
	private boolean admin;
	private int gradYear, studentID;
	private long phoneNumber, cellPhoneNumber;
	private String address;
	byte[] picture;

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
		return "Account [userID=" + userID + ", name=" + name + ", email=" + email + "]\n[ pic length=" + (picture == null ? "null" : picture.length) + ", indunctionDate=" + indunctionDate
				+ ", admin=" + admin + ", gradYear=" + gradYear + ", studentID=" + studentID + ", phoneNumber=" + phoneNumber + ", cellPhoneNumber=" + cellPhoneNumber + ", address=" + address
				+ ", ITS ID=" + itsID + "]";
	}

	public byte[] getPicture() {
		return picture;
	}

	public void setPicture(byte[] picture) {
		this.picture = picture;
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

	public void setGradYear(int gradYear) {
		this.gradYear = gradYear;
	}

	public void setStudentID(int studentID) {
		this.studentID = studentID;
	}

	public void setPhoneNumber(long phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setIndunctionDate(LocalDate indunctionDate) {
		this.indunctionDate = indunctionDate;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCellPhoneNumber(long cellPhoneNumber) {
		this.cellPhoneNumber = cellPhoneNumber;
	}

	public int getGradYear() {
		return gradYear;
	}

	public int getStudentID() {
		return studentID;
	}

	public long getPhoneNumber() {
		return phoneNumber;
	}

	public long getCellPhoneNumber() {
		return cellPhoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public long getUserID() {
		return userID;
	}

	public void setItsID(long itsID) {
		this.itsID = itsID;
	}

	public long getItsID() {
		return itsID;
	}

}
