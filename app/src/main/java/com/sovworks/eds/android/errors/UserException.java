package com.sovworks.eds.android.errors;

import android.content.Context;

public class UserException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UserException(Context context,int messageId)
	{
		super(context!=null ? context.getString(messageId) : "");
	}
	
	public UserException(Context context,int messageId,Throwable cause)
	{
		super(context.getString(messageId),cause);
	}
	
	protected UserException(String message)
	{
		super(message);
	}
	
	protected UserException()
	{
		
	}
}
