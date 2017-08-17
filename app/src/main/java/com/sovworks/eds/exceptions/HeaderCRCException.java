package com.sovworks.eds.exceptions;

public class HeaderCRCException extends ApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HeaderCRCException()
	{
		super("Header crc error");
	}
}
