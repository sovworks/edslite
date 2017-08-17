package com.sovworks.eds.settings;

import java.io.IOException;

public class SettingsException extends IOException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public SettingsException()
	{
		super("Settings file error");
	}
	
	public SettingsException(Throwable cause)
	{
		super("Settings file error");
		initCause(cause);
	}

}
