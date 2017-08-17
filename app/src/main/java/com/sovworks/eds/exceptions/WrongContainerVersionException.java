package com.sovworks.eds.exceptions;


import com.sovworks.eds.exceptions.ApplicationException;

public class WrongContainerVersionException extends ApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WrongContainerVersionException()
	{
		super("Unsupported container version");
	}

	public WrongContainerVersionException(String msg)
	{
		super(msg);
	}

}
