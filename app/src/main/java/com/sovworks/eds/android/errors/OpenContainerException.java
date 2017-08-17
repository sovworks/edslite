package com.sovworks.eds.android.errors;

import android.content.Context;

import com.sovworks.eds.android.R;

public class OpenContainerException extends UserException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OpenContainerException(Context context)
	{
		super(context,R.string.err_failed_opening_container);
	}

}
