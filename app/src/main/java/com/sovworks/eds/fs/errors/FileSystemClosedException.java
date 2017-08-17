package com.sovworks.eds.fs.errors;

import java.io.IOException;

public class FileSystemClosedException extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FileSystemClosedException()
	{
		super("File system is closed.");
	}
}
