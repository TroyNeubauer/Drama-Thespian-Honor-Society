package com.troy.dramaserver.database;

import java.io.Serializable;

import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.format.*;

import com.troy.dramaserver.Main;

public class SessionData implements Serializable {

	private static final long serialVersionUID = 0;

	private byte[] data;
	private long id;
	private DateTime validUntil;

	private static final DateTimeFormatter formatter = DateTimeFormat.shortDateTime();

	public SessionData(byte[] data, long id, DateTime validUntil) {
		this.data = data;
		this.id = id;
		this.validUntil = validUntil;
	}

	public byte[] getData() {
		return data;
	}

	public long getId() {
		return id;
	}

	public DateTime getValidUntil() {
		return validUntil;
	}
	
	public void setValidUntil(DateTime validUntil) {
		this.validUntil = validUntil;
	}

	@Override
	public String toString() {
		Account account = Main.server.getAccount(id);
		return "SessionData [session=0x" + Hex.encodeHexString(data) + ", id=" + (account == null ? "deleted user(" + id + ")" : account.getName()) + ", validUntil=" + validUntil.toString(formatter)
				+ ", valid=" + isValid() + "]";
	}

	public boolean isValid() {
		return DateTime.now().isBefore(validUntil);
	}
}