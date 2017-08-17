package com.sovworks.eds.fs.errors;

import java.io.IOException;

public class NoFreeSpaceLeftException extends IOException
{
	public NoFreeSpaceLeftException()
	{
		super("No free space left");
	}


	private static final long serialVersionUID = 1L;

}
