package com.sovworks.eds.crypto;

import android.annotation.SuppressLint;

import com.sovworks.eds.crypto.engines.AESCTR;
import com.sovworks.eds.crypto.kdf.HMACSHA512KDF;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@SuppressLint("TrulyRandom")
public class SimpleCrypto
{	
	public static byte[] getStrongKeyBytes(byte[] srcKey,byte[] salt)
	{
		//PBKDF2WithHmacSHA1 is not available on the GALAXY Tab.
//		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//		PBEKeySpec spec = new PBEKeySpec(passwd, salt, 100, 128);
//		SecretKey secret = factory.generateSecret(spec);
//		return new SecretKeySpec(secret.getEncoded(),"AES");
		HMACSHA512KDF kdf = new HMACSHA512KDF();
		try
		{
			return kdf.deriveKey(srcKey, salt, 100, 32);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] charsToBytes(char[] chars) 
	{
	    CharBuffer charBuffer = CharBuffer.wrap(chars);
	    ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
	    byte[] bytes = new byte[byteBuffer.limit() - byteBuffer.position()];
	    System.arraycopy(byteBuffer.array(), byteBuffer.position(), bytes, 0, bytes.length);
	    Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		SecureBuffer.eraseData(byteBuffer.array());
	    return bytes;
	}
	
	public static String calcStringMD5(String s)
	{
	    try
		{
			return toHexString(MessageDigest.getInstance("MD5").digest(s.getBytes()));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}   
	}

	public static String encrypt(SecureBuffer key, byte[] data)
	{
		byte[] decKey = key.getDataArray();
		if(decKey == null)
			throw new RuntimeException("key is closed");
		try
		{
			return toHexString(encrypt(decKey, data, 0, data.length));
		}
		finally
		{
			SecureBuffer.eraseData(decKey);
		}
	}

	public static String encrypt(SecureBuffer key, String cleartext)
	{
		return encrypt(key, cleartext.getBytes());
	}

	public static byte[] decrypt(byte[] key, String encrypted)
	{
		byte[] enc = toByte(encrypted);
		if(enc.length<IV_SIZE)
			throw new RuntimeException("Encrypted data is too small.");
		return decrypt(key, enc,0,enc.length);
	}

	public static byte[] decrypt(SecureBuffer key, String encrypted)
	{
		byte[] decKey = key.getDataArray();
		if(decKey == null)
			throw new RuntimeException("key is closed");
		try
		{
			return decrypt(decKey, encrypted);
		}
		finally
		{
			SecureBuffer.eraseData(decKey);
		}
	}

	public static String encryptWithPassword(SecureBuffer passwd, byte[] cleartext)
	{
		byte[] key = passwd.getDataArray();
		if(key == null)
			throw new RuntimeException("key is closed");
		try
		{
			return encryptWithPassword(key, cleartext);
		}
		finally
		{
			SecureBuffer.eraseData(key);
		}
	}

	public static byte[] decryptWithPassword(SecureBuffer passwd, String encrypted)
	{
		byte[] key = passwd.getDataArray();
		if(key == null)
			throw new RuntimeException("key is closed");
		try
		{
			return decryptWithPassword(key, encrypted);
		}
		finally
		{
			SecureBuffer.eraseData(key);
		}
	}

	public static String encryptWithPassword(byte[] passwd, byte[] cleartext)
	{
		return toHexString(encryptWithPasswordBytes(passwd, cleartext));
	}

    @SuppressLint("TrulyRandom")
    public static byte[] encryptWithPasswordBytes(byte[] passwd, byte[] cleartext)
	{
		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[SALT_SIZE];
		sr.nextBytes(salt);
		byte[] key = getStrongKeyBytes(passwd, salt);
		try
		{
			byte[] enc = encrypt(key, cleartext, 0, cleartext.length);
			byte[] res = new byte[SALT_SIZE + enc.length];
			System.arraycopy(salt, 0, res, 0, SALT_SIZE);
			System.arraycopy(enc, 0, res, SALT_SIZE, enc.length);
			return res;
		}
		finally
		{
			SecureBuffer.eraseData(key);
		}
	}

	public static byte[] decryptWithPassword(byte[] passwd, String encrypted)
	{
		return decryptWithPasswordBytes(passwd, toByte(encrypted));
	}

	public static byte[] decryptWithPasswordBytes(byte[] passwd, byte[] encrypted)
	{
		if(encrypted.length<SALT_SIZE + IV_SIZE)
			throw new RuntimeException("Encrypted data is too small.");
		byte[] salt = new byte[SALT_SIZE];
		System.arraycopy(encrypted, 0, salt, 0, SALT_SIZE);
		byte[] key = getStrongKeyBytes(passwd, salt);
		try
		{
			return decrypt(key, encrypted, SALT_SIZE, encrypted.length - SALT_SIZE);
		}
		finally
		{
			SecureBuffer.eraseData(key);
		}
	}
	
	public static String toHex(String txt)
	{
		return toHexString(txt.getBytes());
	}

	public static String fromHex(String hex)
	{
		return new String(toByte(hex));
	}

	public static byte[] toByte(String hexString)
	{
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
		return result;
	}

	public static char[] toHex(byte[] buf)
	{
		if (buf == null) return new char[0];
		CharBuffer result = CharBuffer.allocate(buf.length*2);
		for (byte aBuf : buf)
			appendHex(result, aBuf);
		return result.array();
	}

	public static String toHexString(byte[] buf)
	{
		return new String(toHex(buf));
	}	

	private static final int SALT_SIZE = 8;
	private static final int IV_SIZE = 16;
	private final static String HEX = "0123456789ABCDEF";
	
	private static EncryptionEngine getCipher() throws Exception
	{
		return new AESCTR();
	}

	public static byte[] encrypt(byte[] key, byte[] clear,int offset, int count)
	{
        try
        {
			byte[] iv = new byte[IV_SIZE];
			SecureRandom sr = new SecureRandom();
			sr.nextBytes(iv);
			EncryptionEngine ee = getCipher();
			ee.setKey(key);
			ee.setIV(iv);
			ee.init();
			try
			{
				byte[] res = new byte[IV_SIZE + count];
				System.arraycopy(iv, 0, res, 0, IV_SIZE);
				System.arraycopy(clear, offset, res, IV_SIZE, count);
				ee.encrypt(res, IV_SIZE, count);
				return res;
			}
			finally
			{
				ee.close();
			}
		}
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
	}

	public static byte[] decrypt(byte[] key, byte[] encrypted, int offset, int count)
	{
        try
        {
			byte[] iv = new byte[IV_SIZE];
			System.arraycopy(encrypted, offset, iv, 0, IV_SIZE);
			EncryptionEngine ee = getCipher();
			ee.setKey(key);
			ee.setIV(iv);
			ee.init();
			try
			{
				byte[] res = new byte[count - IV_SIZE];
				System.arraycopy(encrypted, offset + IV_SIZE, res, 0, res.length);
				ee.decrypt(res, 0, res.length);
				return res;
			}
			finally
			{
				ee.close();
			}

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
	}
	
	private static void appendHex(CharBuffer sb, byte b)
	{
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}

}