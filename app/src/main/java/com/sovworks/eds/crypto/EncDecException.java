package com.sovworks.eds.crypto;

import java.io.IOException;

public class EncDecException extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EncDecException(String msg)
	{
		super(msg);
	}

	public EncDecException(EncryptionEngineException cause)
	{
		super(cause.getMessage());
	}
}
