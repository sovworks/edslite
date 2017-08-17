package com.sovworks.eds.fs.errors;

import java.io.IOException;

public class FileInUseException extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FileInUseException()
	{
		
	}
	
	public FileInUseException(String msg)
	{
		super(msg);
	}

}
