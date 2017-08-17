package com.sovworks.eds.android.errors;

import android.content.Context;

import com.sovworks.eds.android.R;

public class NoFreeSpaceLeftException extends UserException
{	

	public NoFreeSpaceLeftException(Context context)
	{
		super(context,R.string.no_free_space_left);
	}

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
}