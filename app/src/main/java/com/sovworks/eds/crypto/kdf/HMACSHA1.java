package com.sovworks.eds.crypto.kdf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HMACSHA1 extends HMAC
{
	public static final int SHA1_BLOCK_SIZE = 64;

	public HMACSHA1(byte[] key) throws NoSuchAlgorithmException
	{
		super(key, MessageDigest.getInstance("SHA1"),SHA1_BLOCK_SIZE);
	}
}
