package com.sovworks.eds.exceptions;

public class NativeError extends Exception
{	
	public static final int ENOENT = -2;
	public static final int EIO = -5;
	public static final int EBADF = -9;
	
	public int errno;
	public NativeError(int errn)
	{
		errno=errn;
	}
	
	private static final long serialVersionUID = 1L;
}
