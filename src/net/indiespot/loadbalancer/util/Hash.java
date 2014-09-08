package net.indiespot.loadbalancer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

	private static final char[] hex_table = "0123456789abcdef".toCharArray();

	public static String toHex(byte[] raw) {
		char[] hex = new char[raw.length * 2];
		for (int i = 0, p = 0; i < raw.length; i++) {
			hex[p++] = hex_table[(raw[i] >> 4) & 0xf];
			hex[p++] = hex_table[(raw[i] >> 0) & 0xf];
		}
		return new String(hex);
	}

	public static byte[] fromHex(String hex) {
		if (hex.length() % 2 != 0) {
			throw new IllegalStateException();
		}

		byte[] raw = new byte[hex.length() >> 1];
		int counter = 0;
		for (char c : hex.toCharArray()) {
			int hexDigit;
			if (c >= '0' && c <= '9') {
				hexDigit = c - '0';
			} else if (c >= 'a' && c <= 'f') {
				hexDigit = (c - 'a') + 10;
			} else if (c >= 'A' && c <= 'F') {
				hexDigit = (c - 'A') + 10;
			} else {
				throw new IllegalStateException("invalid hex char: " + c);
			}

			raw[counter >> 1] <<= 4;
			raw[counter >> 1] |= hexDigit;
			counter++;
		}
		return raw;
	}

	//

	public static byte[] sha1(byte[] message) {
		return hash(message, "SHA-1");
	}

	public static MessageDigest sha1() {
		try {
			return MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static String sha1(File file) throws IOException {
		MessageDigest digest = sha1();

		if (!file.exists()) {
			System.err.println("hashing non existing file: " + file.getAbsolutePath());
			digest.update(Text.ascii("~~~file_not_found~~~"));
		} else if (file.isFile()) {
			// try (InputStream in = new FileInputStream(file)) {
			Streams.copy(new FileInputStream(file), new DigestOutputStream(new NullOutputStream(), digest));
		} else {
			System.err.println("hashing directory: " + file.getAbsolutePath());
			digest.update(Text.ascii("~~~directory~~~"));
		}

		return Text.hex(digest.digest());
	}

	public static String sha1(InputStream in) throws IOException {
		MessageDigest digest = sha1();
		Streams.copy(in, new DigestOutputStream(new NullOutputStream(), digest));
		return Text.hex(digest.digest());
	}

	public static byte[] sha256(byte[] message) {
		return hash(message, "SHA-256");
	}

	public static byte[] hash(byte[] message, String algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			md.update(message);
			return md.digest();
		} catch (Exception exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static byte[] join(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}
