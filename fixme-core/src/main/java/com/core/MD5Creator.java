package com.core;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Creator {
	public static String createMD5FromObject(String id) {
		String myHash = "error_check_sum";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(id.getBytes());
			byte[] digest = md.digest();
			myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return myHash;
	}
}
