package com.sovworks.eds.android.errors;

import android.content.Context;

import com.sovworks.eds.android.R;

public class ClosedContainerException extends UserException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4482745672661567457L;

	public ClosedContainerException(Context context)
	{
		super(context,R.string.err_target_container_is_closed);
	}

}
