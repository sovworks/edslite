package com.sovworks.eds.exceptions;

import com.sovworks.eds.exceptions.ApplicationException;

public class UnsupportedContainerTypeException extends ApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnsupportedContainerTypeException()
	{
		super("Unsupported container type");
	}

	public UnsupportedContainerTypeException(String msg)
	{
		super(msg);
	}
}
