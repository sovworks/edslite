package com.sovworks.eds.crypto;

import com.sovworks.eds.exceptions.ApplicationException;


public class EncryptionEngineException extends ApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EncryptionEngineException()
	{

	}

	public EncryptionEngineException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public EncryptionEngineException(String msg)
	{
		super(msg);
	}
}
