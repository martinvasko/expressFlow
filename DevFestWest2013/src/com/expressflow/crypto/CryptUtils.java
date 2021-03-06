package com.expressflow.crypto;

public class CryptUtils {

	private static byte[] keyBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04,
			0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
			0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };

	private static byte[] ivBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x00,
			0x01, 0x02, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };

	public static byte[] getKeyBytes() {
		return keyBytes;
	}

	public static byte[] getIVBytes() {
		return ivBytes;
	}
}
