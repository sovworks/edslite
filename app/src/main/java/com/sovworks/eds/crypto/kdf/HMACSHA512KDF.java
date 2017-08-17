package com.sovworks.eds.crypto.kdf;

import java.security.NoSuchAlgorithmException;

import com.sovworks.eds.crypto.EncryptionEngineException;

public class HMACSHA512KDF extends PBKDF
{
	@Override
	protected HMAC initHMAC(byte[] password) throws EncryptionEngineException
	{
		try
		{
			return new HMACSHA512(password);
		}
		catch (NoSuchAlgorithmException e)
		{
			EncryptionEngineException e1 = new EncryptionEngineException();
			e1.initCause(e);
			throw e1;
		}
	}	
}