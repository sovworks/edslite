package com.sovworks.eds.fs.errors;

import java.io.IOException;

public class DirectoryIsNotEmptyException extends IOException
{
	public DirectoryIsNotEmptyException(String path)
	{
		_path = path;
	}
	
	public DirectoryIsNotEmptyException(String path,String msg)
	{
		super(msg);
		_path = path;
	}
	
	public String getPath()
	{
		return _path;
	}
		
	private static final long serialVersionUID = 1L;
	
	private final String _path;
	
}
