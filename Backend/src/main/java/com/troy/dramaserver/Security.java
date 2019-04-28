package com.troy.dramaserver;

import java.security.NoSuchAlgorithmException;
import java.security.spec.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.logging.log4j.*;

public class Security {

	private static final Logger logger = LogManager.getLogger(Security.class);
	
	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final SecretKeyFactory FACTORY = getAlgorithm();
	
	public static byte[] getHashedPassword(char[] password, byte[] salt,  byte[] pepper, int iterations,  int derivedKeyLength) {
		byte[] saltPlusPepper = new byte[salt.length + pepper.length];
		System.arraycopy(salt, 0, saltPlusPepper, 0, salt.length);
		System.arraycopy(pepper, 0, saltPlusPepper, salt.length, pepper.length);
	    KeySpec spec = new PBEKeySpec(password, saltPlusPepper, iterations, derivedKeyLength * 8);

	    try {
			return FACTORY.generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException e) {
			logger.catching(e);
			return null;
		}
	}
	
	
	private static SecretKeyFactory getAlgorithm() {
		try {
			return SecretKeyFactory.getInstance(ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			logger.fatal("Unable to find the required algorithm for hashing a password, " + ALGORITHM);
			logger.catching(e);
		}
		return null;
	}


	private Security() {
	}

}