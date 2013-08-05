package com.tpsoft.pushnotification.utils;

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import android.util.Base64;

public class Crypt {

	private static final String ALGORITHM_DES = "DES/CBC/NoPadding"; // �Լ������������
	private static byte[] iv = { 1, 2, 3, 4, 5, 6, 7, 8 };

	public static String md5(String string) {
		byte[] hash;
		try {
			hash = MessageDigest.getInstance("MD5").digest(string.getBytes());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Huh, MD5 should be supported?", e);
		}

		StringBuilder hex = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			if ((b & 0xFF) < 0x10)
				hex.append("0");
			hex.append(Integer.toHexString(b & 0xFF));
		}
		return hex.toString().toUpperCase();
	}

	public static String rsaEncrypt(String key, String str) throws Exception {
		return desEncrypt(key, str);
	}

	public static String rsaDecrypt(String key, String str) throws Exception {
		return desDecrypt(key, str);
	}

	/**
	 * DES�㷨������
	 * 
	 * @param str
	 *            �������ַ���
	 * @param key
	 *            ������Կ�����Ȳ��ܹ�С��8λ
	 * @return ���ܺ���ַ���(BASE64)
	 * @throws CryptException
	 *             �쳣
	 */
	public static String desEncrypt(String key, String str) throws Exception {
		IvParameterSpec zeroIv = new IvParameterSpec(iv);
		DESKeySpec dks = new DESKeySpec(key.getBytes("UTF-8"));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key secretKey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, zeroIv);

		byte[] data = str.getBytes("UTF-8");
		int dataLength = data.length;
		int paddingLength = (8 - (4 + dataLength) % 8) % 8;
		ByteBuffer buffer = ByteBuffer.allocate(8 + 4 + dataLength
				+ paddingLength);
		byte[] paddingBytes = new byte[paddingLength];
		buffer.put(new byte[8]); // �������õ�8���ַ�
		buffer.putInt(dataLength);
		buffer.put(data);
		buffer.put(paddingBytes);

		byte[] bytes = buffer.array();
		byte[] encryptedData = cipher.doFinal(bytes);
		String encryptedText = Base64.encodeToString(encryptedData,
				Base64.DEFAULT);
		return encryptedText;
	}

	/**
	 * DES�㷨������
	 * 
	 * @param str
	 *            �����ܵ��ַ���(BASE64)
	 * @param key
	 *            ������Կ�����Ȳ��ܹ�С��8λ
	 * @return ���ܺ���ַ���
	 * @throws Exception
	 *             �쳣
	 */
	public static String desDecrypt(String key, String str) throws Exception {
		IvParameterSpec zeroIv = new IvParameterSpec(iv);
		DESKeySpec dks = new DESKeySpec(key.getBytes("UTF-8"));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key secretKey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance(ALGORITHM_DES);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, zeroIv);

		byte[] data = Base64.decode(str, Base64.DEFAULT);
		byte[] bytes = cipher.doFinal(data);

		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		int dataLength = buffer.getInt(8);
		if (dataLength < 0 || dataLength > bytes.length - 8 - 4)
			throw new Exception("DES decrypt error");
		ByteBuffer decryptedData = ByteBuffer.allocate(dataLength);
		decryptedData.put(buffer.array(), 8 + 4, dataLength);

		return new String(decryptedData.array(), "UTF-8");
	}

}
