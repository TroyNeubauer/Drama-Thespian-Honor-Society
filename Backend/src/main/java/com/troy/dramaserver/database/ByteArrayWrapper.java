package com.troy.dramaserver.database;

import java.io.Serializable;
import java.util.Arrays;

public class ByteArrayWrapper implements Serializable {

	private byte[] array;

	public ByteArrayWrapper(byte[] array) {
		this.array = array;
	}

	public byte[] getArray() {
		return array;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return Arrays.equals(array, ((ByteArrayWrapper) obj).array);
	}
}
