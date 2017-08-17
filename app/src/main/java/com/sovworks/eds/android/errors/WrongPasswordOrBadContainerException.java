package com.sovworks.eds.android.errors;

import android.content.Context;

import com.sovworks.eds.android.R;

public class WrongPasswordOrBadContainerException extends UserException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WrongPasswordOrBadContainerException(Context context)
	{
		super(context,R.string.bad_container_file_or_wrong_password);
	}
}
